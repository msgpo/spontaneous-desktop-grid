/***************************************************************************
 *   Filename: f2fcore.h
 *   Author: ulno
 ***************************************************************************
 *   Copyright (C) 2008 by Ulrich Norbisrath 
 *   devel@mail.ulno.net   
 *                                                                         
 *   This program is free software; you can redistribute it and/or modify  
 *   it under the terms of the GNU Library General Public License as       
 *   published by the Free Software Foundation; either version 2 of the    
 *   License, or (at your option) any later version.                       
 *                                                                         
 *   This program is distributed in the hope that it will be useful,       
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 *   GNU General Public License for more details.                          
 *                                                                         
 *   You should have received a copy of the GNU Library General Public     
 *   License along with this program; if not, write to the                 
 *   Free Software Foundation, Inc.,                                       
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
 ***************************************************************************
 *   Description:
 *   This is the F2FCore interface implementation
 ***************************************************************************/

#include <stdio.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <arpa/inet.h>

#include "mtwist/mtwist.h"
#include "b64/b64.h"
#include "f2fcore.h"
#include "f2fpeerlist.h"
#include "f2fgroup.h"
#include "f2fgrouplist.h"
#include "f2fmessagetypes.h"
#include "f2fticketrequest.h"

static int initWasCalled = 0; // this will be set if init was successful
static F2FError globalError = F2FErrOK; // a global error variable

F2FError f2fGetErrorCode()
{
	return globalError;
}

/** Static state vector for randomness of mersenne twister, this is global in this module */
mt_state randomnessState;

static F2FPeer *myself; /* saves own peer information */

/** Send buffer, this can be also sent to multiple peers */
static struct
{
	char buffer[F2FMaxMessageSize];
	F2FSize size; /* how much is filled */
	F2FPeer * peerList[ F2FMaxPeers ];
	F2FSize peercount; /* the number of ids in list */
} sendBuffer;

/** Send IM buffer.
 * This buffer stores IM messages, which have to be sent
 * They could be sent to multiple peers.
 * This is, why there is a list of local peer ids.
 */
static struct
{
	char buffer[F2FMaxEncodedMessageSize+1]; /* usually cleartext, so reserve space
								for terminating 0 */
	F2FSize size; /* how much is filled */
	F2FWord32 localPeerIDlist[ F2FMaxPeers ];
	F2FSize peercount; /* the number of ids in list */
} sendIMBuffer;

/** get the 0 terminated sendIMBuffer */
char * f2fSendIMBufferGetBuffer()
{
	if(sendIMBuffer.size > F2FMaxEncodedMessageSize) sendIMBuffer.size = F2FMaxEncodedMessageSize;
	sendIMBuffer.buffer[sendIMBuffer.size] = 0; /* make sure this is terminated */
	return sendIMBuffer.buffer;
}

/** get the size of the message in sendIMBuffer */
F2FSize f2fSendIMBufferGetBufferSize()
{
	return sendIMBuffer.size;	
}

/** return the next peer id of the buffer where data has to be sent and
 * decrease list
 * return -1 if there is nothing to send */
F2FWord32 f2fSendIMBufferGetNextLocalPeerID()
{
	if (sendIMBuffer.peercount <= 0) return -1; // none left
	return sendIMBuffer.localPeerIDlist[-- sendIMBuffer.peercount];
}

/** Receive buffer */
static struct
{
	F2FGroup *group; /** The group in which this data was sent */
	F2FPeer *sourcePeer; /** The sourcepeer of this data */
	F2FPeer *destPeer; /** The destination peer of this data 
	 					 * TODO: if this is not this peer, the data should 
	 					 * be forwarded*/
	int filled; /** indicate, if something is in here */
	F2FMessageType messageType; /** what kind of data is in here */
	F2FSize size; /* how much is filled */
	char buffer[F2FMaxMessageSize];
} receiveBuffer;

/* this is a secure strlen with no buffer overrun */
static inline size_t strnlen( const char *str, size_t max )
{
	size_t size;
	
	for (size = 0; size < max; ++size) {
		if( ! str[size] ) break;
	}
	return size;
}

// Forward references
F2FError f2fPeerSubmitJob( const F2FGroup *group, F2FPeer *peer );
static F2FError parseReceiveBuffer();

/** Do the initialization - especially create a random seed and get your own PeerID 
 * Must be called first.
 * Gets the name of this peer (for example "Ulrich Norbisrath's peer") and the public key */
F2FPeer * f2fInit( const F2FString myName, const F2FString myPublicKey )
{
#define MAXBUFFERSIZE 1024
	char buffer[MAXBUFFERSIZE+1]; // Buffer for concatenation of name and key
	buffer [MAXBUFFERSIZE]=0; // Make sure this is 0 terminated
	size_t buffersize;
	F2FWord32 seeds[ MT_STATE_SIZE ];
	
	// First create the seed for the mersenne twister
	srand( time(NULL) );  // Initialize the poor randomizer of C itself
	// copy name and publickey in buffer (this copy should be secure)
	strncpy( buffer, myName, MAXBUFFERSIZE );
	buffersize = strlen( buffer );
	strncpy( buffer + buffersize, myPublicKey, MAXBUFFERSIZE - buffersize );
	buffersize = strlen( buffer );
	if (buffersize < 4) // not enough data to create seed
	{
		globalError = F2FErrNotEnoughSeed;
	    return NULL;
	}
	// iterate over seed
	int seedCell;
	for (seedCell = 0; seedCell < MT_STATE_SIZE; ++seedCell) 
	{
		// Xor a random number with a random 4 byte pair out of th ebuffer
		seeds[ seedCell ] = rand() ^ *( (F2FWord32 *) (buffer + ( rand()%(buffersize-3) ) ) );  
	}
	mts_seedfull( &randomnessState, seeds ); // activate the seed
	/* Create a new peer with random uid */
	F2FPeer *newpeer = f2fPeerListNew( mts_lrand( &randomnessState ), mts_lrand( &randomnessState ) );
	if (newpeer == NULL)
	{
		globalError = F2FErrListFull;
		return NULL;
	}
	/* initialize buffers */
	receiveBuffer.filled = 0;
	receiveBuffer.size = 0;
	sendBuffer.size = 0;
	sendIMBuffer.peercount = 0;
	/* Init was successfull */
	initWasCalled = 1;
	myself = newpeer;
	myself->activeprovider = F2FProviderMyself;
	return newpeer;
}

/** Return a random number from the seeded mersenne twister */
F2FWord32 f2fRandom()
{
	return mts_lrand( &randomnessState );
}

/** Return a random number from the seeded mersenne twister, but not 0 */
F2FWord32 f2fRandomNotNull()
{
	 F2FWord32 rand;
	 while( (rand = mts_lrand( &randomnessState )) == 0);
	 return rand;
}

/** As a next step, the user has to create a new F2FGroup, in which his intenden Job can be
 * computeted.
 * This group gets a name, which should be displayed in the invitation of clients (other peers). */
F2FGroup * f2fCreateGroup( const F2FString groupname )
{
	if (! initWasCalled)
	{
		globalError = F2FErrInitNotCalled;
		return NULL;
	}
	F2FGroup * group = f2fGroupListCreate( groupname );
	if( group )
	{
		globalError = f2fGroupAddPeer(group, myself); /* Add myself in this new group */
		return group;
	}
	globalError = F2FErrListFull;
	return NULL;
}

/** encode a message for sending via the f2f IM channel in the
 * sendIMBuffer ignore localpeerids */
F2FError encodeIMMessage( const char * message, 
		const F2FSize size )
{
	F2FSize currentsize, newsize;

	/* Encode message for sending in f2f framework */
	strcpy( sendIMBuffer.buffer, F2FMessageMark ); /* header */
	currentsize = F2FMessageMarkLength;
	newsize = b64encode( message, sendIMBuffer.buffer + currentsize,
				size, F2FMaxEncodedMessageSize-currentsize );
	if(newsize == 0) return F2FErrMessageTooLong;
	currentsize += newsize;	/* prepare sending of the new message */
	sendIMBuffer.size = currentsize;
	return F2FErrOK;
}

/** prepare message to send an IM message to one locally known peer */
F2FError f2fIMSend( const F2FWord32 localpeerid, const F2FString message, 
		const F2FSize size )
{
	F2FError error;
	
	if( sendIMBuffer.peercount > 0 ) // not empty
	{
		return F2FErrBufferFull;
	}
	error = encodeIMMessage( message, size );
	sendIMBuffer.localPeerIDlist[ 0 ] = localpeerid;
	sendIMBuffer.peercount = 1;
	return F2FErrOK;
}

/** Add a local peer to the sendIMBuffer */
F2FError f2fIMAddPeer( const F2FWord32 localpeerid )
{
	if( sendIMBuffer.peercount >= F2FMaxPeers ) return F2FErrListFull;
	sendIMBuffer.localPeerIDlist[ ++ sendIMBuffer.peercount ] = localpeerid;
	return F2FErrOK;
}

/* use the special send function to send a IM message to a local known peer */
F2FError f2fIMDecode( const F2FString message, const F2FSize messagelen, 
		F2FString decodebuffer, const F2FSize maxdecodelen )
{
	/* TODO: eventually remove whitespace */
	/* Decode message from IM ending in f2f framework */
	if( memcmp( message, F2FMessageMark, F2FMessageMarkLength ) ) /* test header */
		return F2FErrNotF2FMessage; /* not the right header */
	b64decode( message + F2FMessageMarkLength, decodebuffer, messagelen, maxdecodelen );
	/* TODO: eventually evaluate result of b64decode */ 
	return F2FErrOK;
}

/** Finally friends (other peers) can be added to this group. This function triggers
 * the registration to ask the specified peer to join a F2F Computing group 
 * If we know his public key, we can send it as a challenge. He would then also get our publickey,
 * which could be compared to a allready cached one, to create an own challenge, and later used to
 * do encrypted and authenticated communication. Of course our own peerid from f2fInit is also
 * included.
 * - localPeerId will be the id used to send an IM message to this friend, it has to be managed
 * in the middle layer
 * - identifier can be the name in the addressbook or one of the addresses including the protocol,
 * example: "test@jabber.xyz (XMPP)" 
 * This function will call the SendMethodIP-function*/
F2FError f2fGroupRegisterPeer( /* out */ F2FGroup *group, const F2FWord32 localPeerId,
		const F2FString identifier, const F2FString inviteMessage,
		const F2FString otherPeersPublicKey )
{
	/* Ask new peer, if he is f2f capable: send a challenge, save the challenge.
	 * If the peer sends back the correct answer at one point, it is added as a peer */

	/* first create ID for new peer and save this peer as an unconfirmed peer */
	F2FPeer *newpeer = f2fPeerListNew( f2fRandomNotNull(), f2fRandomNotNull() );
	/* This id here is only temporary and only used as challenge, so the client can 
	 * authenticate itself, that it really knows this number.
	 * It is temporarely saved in the peer's id and replaced by 
	 * the real id, when the peer sends its real id. */
	/* initialize  the newly allocated peer */
	newpeer->localPeerId = localPeerId;
	newpeer->status = F2FPeerWaitingForInviteConfirm;
	newpeer->activeprovider = F2FProviderIM; /* Contact at the moment via IM */
	newpeer->lastActivity = time( NULL );
	newpeer->identifier[F2FMaxNameLength] = 0;
	f2fPeerAddToGroup( newpeer, group );
	strncpy( newpeer->identifier, identifier, F2FMaxNameLength);
	
	/* prepare the outgoing message */
	F2FMessageInvite mes;
	mes.tmpIDAndChallenge.hi = htonl( newpeer->id.hi ); 
	mes.tmpIDAndChallenge.lo = htonl( newpeer->id.lo );
	F2FSize groupNameLength = strnlen( identifier, F2FMaxNameLength );
	mes.groupNameLength = htonl(groupNameLength);
	memcpy( mes.nameAndInvite, identifier, groupNameLength );
	F2FSize inviteLength = strnlen( inviteMessage, F2FMaxNameLength );
	mes.inviteLength = htonl( inviteLength );
	memcpy( mes.nameAndInvite + groupNameLength, identifier, inviteLength );
	return f2fPeerSendData(group, newpeer, F2FMessageTypeInvite, (char *) &mes, 
			sizeof(F2FMessageInvite) - 2 * F2FMaxNameLength
			+ groupNameLength + inviteLength );
}

/** unregister the peer again, must be in group 
 * changes group and peer (because of the embedded lists) */
F2FError f2fGroupUnregisterPeer( F2FGroup *group, F2FPeer *peer )
{
	return f2fGroupPeerListRemove(group, peer);
	// TODO: implement notfication
}

/** Return size of a peerlist in a group */
F2FSize f2fGroupGetPeerListSize( const F2FGroup *group )
{
	return group->listSize;
}

/** Return a pointer to a peer of a group */
F2FPeer * f2fGroupGetPeerFromList( const F2FGroup *group, 
		F2FWord32 peerindex )
{
	if(peerindex<0 || peerindex>group->listSize) return NULL;
	return group->sortedPeerList[peerindex].peer;
}

/** tries to receive a message. If succesful, this gives a peer and the corresponding
 * message, if not peer and message will be NULL and F2FErrNothingAvail will be returned.
 * In success case F2FErrOK will be returned.
 * This routine must be called on a regulary interval - it can't be used in parallel to
 * the other methods here in this interface. 
 * If the timeout value is >0 then it will be used in an internal select. The function will
 * then block to the maximum timeout ms. 
 * This function returns F2FErrBufferFull, if there is still data to receive available.
 * The function should be called directly again (after processing the received data) */
F2FError f2fReceive()
{
	if(receiveBuffer.filled) // Can't receive with full buffer
		return F2FErrBufferFull;
	/* From here on receive.filled is 0 */
	/* Try now to receive stuff from the internal network (not IM) */
	/* call then parseMessage, this might fill the receiveBuffer */
	/* TODO: select(... */
	return F2FErrOK;
}

/** return 1, if there is data in the ReceiveBuffer */
int f2fReceiveBufferIsFilled()
{ return receiveBuffer.filled; }

/** return 1, if the data in the ReceiveBuffer is raw data */
int f2fReceiveBufferIsRaw()
{ return receiveBuffer.messageType == F2FMessageTypeRaw; }

/** return 1, if the data in the ReceiveBuffer is text data */
int f2fReceiveBufferIsText()
{ return receiveBuffer.messageType == F2FMessageTypeText; }

/** return 1, if the data in the ReceiveBuffer is a job */
int f2fReceiveBufferIsJob()
{ return receiveBuffer.messageType == F2FMessageTypeJob; }

/** get the group of the received data */
F2FGroup * f2fReceiveBufferGetGroup()
{ return receiveBuffer.group; }

/** get received Source peer */
F2FPeer * f2fReceiveBufferGetSourcePeer()
{ return receiveBuffer.sourcePeer; }

/** get received destination peer */
F2FPeer * f2fReceiveBufferGetDestPeer()
{ return receiveBuffer.destPeer; }

/** get size of current buffer */
F2FSize f2fReceiveBufferGetSize()
{ return receiveBuffer.size; }

/** get a pointer to the content of the buffer */
char * f2fReceiveBufferGetContentPtr()
{ return receiveBuffer.buffer; }

/** special function for SWIG to return a binary buffer 
 * maxlen is a pointer to a variable, which specifies the maximum len, which can be taken and
 * will have the actual length of copied data at the end
 * The data must be copied into content */
void f2fReceiveBufferGetContent(char *content, int *maxlen )
{
	if (*maxlen > receiveBuffer.size) *maxlen = receiveBuffer.size;
	memcpy(content,receiveBuffer.buffer,*maxlen);
}

/* analog to the last function (f2fReceiveBufferGetContent) receive a job */
void f2fReceiveJob(char *content, int *maxlen )
{
	if( !f2fReceiveBufferIsFilled() || !f2fReceiveBufferIsJob() )
	{
		*maxlen = 0;
		return;
	}
	F2FMessageJob *jobmsg = (F2FMessageJob *) receiveBuffer.buffer;
	F2FSize jobsize = ntohl(jobmsg->size);
	if (*maxlen > jobsize) *maxlen = jobsize;
	memcpy(content,receiveBuffer.buffer + sizeof(F2FMessageJob),*maxlen);
	/* TODO: adapt to longer jobs */
}


/** show that the buffer has been read and can be filled again */
F2FError f2fReceiveBufferRelease()
{
	receiveBuffer.filled = 0;
	return F2FErrOK;
}

/* Parse the contents of the receive buffer and release it, if successfull */
F2FError f2fReceiveBufferParse()
{
	F2FError error = parseReceiveBuffer();
	if(error != F2FErrOK)
		return error;
	return f2fReceiveBufferRelease();
}

static F2FError fillReceiveBuffer( F2FWord32 grouphi, F2FWord32 grouplo,
		F2FWord32 srchi, F2FWord32 srclo,
		F2FWord32 desthi, F2FWord32 destlo,
		F2FMessageType type, const char *buf, F2FSize size )
{
	F2FGroup *group;
	F2FPeer *src, *dst;
	
	if( receiveBuffer.filled ) /* Still filled */
		return F2FErrBufferFull;
	if( size > F2FMaxMessageSize)
		return F2FErrMessageTooLong;
	/* Find group and peers */
	group = f2fGroupListFindGroup(grouphi, grouplo);
	if(!group) return F2FErrNotFound;
	src = f2fPeerListFindPeer(srchi, srclo);
	/* if(!src) return F2FErrNotFound;might be zero, if invitemessage answer */
	if(desthi == 0 && destlo == 0)
	{ // If both 0 then don't route just assume message is to me
		dst = myself;
	}
	else
	{
		dst = f2fPeerListFindPeer(desthi, destlo);
		// TODO: Consider, what has to be done for routing
	}
	if(!dst) return F2FErrNotFound;
	receiveBuffer.filled = 1; /* Now it will be filled */
	receiveBuffer.group = group;
	receiveBuffer.sourcePeer = src;
	receiveBuffer.destPeer = dst;
	receiveBuffer.messageType = type;
	memcpy( receiveBuffer.buffer, buf, size );
	receiveBuffer.size = size;
	return F2FErrOK;
}

/** Send content of the local sendBuffer 
 * TODO: Think, if timeout is needed here */
F2FError f2fSend()
{
	if(sendBuffer.peercount > 0)
	{
		/* Take the first peer and process message to it */
		F2FPeer * peer = sendBuffer.peerList[sendBuffer.peercount - 1];
		/* Find the communication provider, which helps
		 * us to reach the destination */
		switch( peer->activeprovider )
		{
		case F2FProviderMyself:
			/* here we can just evaluate the received data */
			if( receiveBuffer.filled )
				return F2FErrBufferFull;
			F2FMessageHeader * msg = (F2FMessageHeader *) & sendBuffer.buffer;
			fillReceiveBuffer(
					ntohl(msg->groupID.hi), ntohl(msg->groupID.lo),
					ntohl(msg->sourcePeerID.hi), ntohl(msg->sourcePeerID.lo),
					ntohl(msg->destPeerID.hi), ntohl(msg->destPeerID.lo),
					msg->messagetype,
					sendBuffer.buffer + sizeof(F2FMessageHeader),
					ntohl(msg->len) );
			/* receiveBuffer.group ? */
			/* parseMessage(sendBuffer.buffer, sendBuffer.size); will be called in f2fReceive */
			break;
		case F2FProviderTCPIPV4:
			break;
			/* TODO: implement */
		case F2FProviderUDPHolePunch:
			/* TODO: implement */
			break;
		case F2FProviderIM:
			break;
		}
		sendBuffer.peercount --;
	}
	return F2FErrOK;
}

/** process an InviteMessage sent to me, react to this invite
 * (create the answer) */
static F2FError processInviteMessage( 
		F2FGroup *group, F2FPeer *srcPeer, F2FPeer *dstPeer,
		const F2FMessageInvite *msg )
{
	/* dstpeer should be myself, as the message was sent to me */
	/* TODO: maybe check? */
	/* Send answer back */
	F2FMessageInviteAnswer myanswer;
	myanswer.tmpIDAndChallenge.hi = msg->tmpIDAndChallenge.hi; /* no transfer in endian necessary */
	myanswer.tmpIDAndChallenge.lo = msg->tmpIDAndChallenge.lo; /* no transfer in endian necessary */
	myanswer.realSourceID.hi = htonl(myself->id.hi);
	myanswer.realSourceID.lo = htonl(myself->id.lo);
	// Add the peer and myself to the group
	f2fGroupAddPeer(group,srcPeer);
	f2fGroupAddPeer(group,myself);
	return f2fPeerSendData( group, srcPeer, F2FMessageTypeInviteAnswer,
			(char *)& myanswer, sizeof(F2FMessageInviteAnswer));
}

/** process an InviteMessageAnswer */
static F2FError processInviteMessageAnswer( 
		F2FGroup *group, F2FPeer *srcPeer, F2FPeer *dstpeer,
		const F2FMessageInviteAnswer *msg )
{
	F2FError error;
	
	/* Check if peer waits for an invite */
	F2FPeer * waitingPeer = f2fPeerListFindPeer(
					ntohl(msg->tmpIDAndChallenge.hi), ntohl(msg->tmpIDAndChallenge.lo) );
	if( ! waitingPeer )
		return F2FErrNotAuthenticated;
	if( waitingPeer->status != F2FPeerWaitingForInviteConfirm )
		return F2FErrNotAuthenticated;
	if( f2fPeerFindGroupIndex( waitingPeer, group ) < 0 )
		return F2FErrNotAuthenticated;
	/* Change the id to the official id */
	error = f2fPeerChangeUID( waitingPeer, 
			ntohl(msg->realSourceID.hi), ntohl(msg->realSourceID.lo) );
	if( error != F2FErrOK ) return error;
	waitingPeer -> status = F2FPeerActive; /* active now */
	/* Send updates to the remaining peers, that there is a new group member */
	/* TODO: implement */
	/* For all groups, where a job is submitted, get the corresponding
	 * tickets */
	int index;
	for( index = 0; index < waitingPeer->groupsListSize; index ++)
	{
		if( waitingPeer->groups[index]->jobfilepath[0] )
		{
			/* TODO: This works at the moment only, if there is only one group */
			if(index>0)
			{
				printf("processInviteMessageAnswer: Multiple groups not supported\n");
				return F2FErrWierdError;
			}
			f2fPeerSubmitJob( waitingPeer->groups[index], waitingPeer );
		}
	}
	return F2FErrOK;
}

/** process a message of type data */
/*
static F2FError processMessageData( const char * buffer)
{
	F2FMessageHeader *msg = (F2FMessageHeader *) buffer;
	
	if( msg->binary )
		printf("Received binary data from %d, %d in group %d, %d.\n",
				ntohl(msg->hdr.sourcePeerID.hi), ntohl(msg->hdr.sourcePeerID.lo),
				ntohl(msg->hdr.groupID.hi), ntohl(msg->hdr.groupID.lo));
	else
	{
		char tmpbuffer[F2FMaxMessageSize+1];
		if( msg->size > F2FMaxMessageSize )
			return F2FErrMessageTooLong;
		memcpy(tmpbuffer, buffer + sizeof(*msg), msg->size );
		tmpbuffer[msg->size] = 0;
		printf("Received text data from %d, %d in group %d, %d: %s\n",
				ntohl(msg->hdr.sourcePeerID.hi), ntohl(msg->hdr.sourcePeerID.lo),
				ntohl(msg->hdr.groupID.hi), ntohl(msg->hdr.groupID.lo),
				tmpbuffer );
	}
	return F2FErrOK;
}
*/

/** process a Job-Ticket-Request sent and create ticket request to user */
static F2FError processGetJobTicket( 
		F2FGroup *group, F2FPeer *srcPeer, F2FPeer *dstPeer,
		const F2FMessageGetJobTicket *msg )
{
	/* TODO: Also add the description params from F2FMessageGetJobTicket */
	F2FError error = f2fTicketRequestAdd( group, srcPeer );
	return error;
}

/** process a Job-Ticket-Answer (send the job) */
static F2FError processGetJobTicketAnswer( 
		F2FGroup *group, F2FPeer *srcPeer, F2FPeer *dstPeer,
		const F2FMessageGetJobTicketAnswer *msg )
{
	/* TODO: Check, if this was requested */
	/* Save ticket and then initiate sending the job */
	F2FGroupPeer *groupPeer = f2fGroupFindGroupPeer( group, 
			srcPeer->id.hi, srcPeer->id.lo );
	if( groupPeer == NULL ) return F2FErrNotFound;
	groupPeer->sendTicket.hi = ntohl( msg->ticket.hi );
	groupPeer->sendTicket.lo = ntohl( msg->ticket.lo );
	groupPeer->sendTicket.validUntil = ntohl( msg->ticket.validUntil );
	/* Set up job */
	struct
	{
		F2FMessageJob job;
		char data[F2FMaxMessageSize-sizeof(F2FMessageHeader)-sizeof(F2FMessageJob)];
	} jobmsg;
	if( group->jobarchivesize > F2FMaxMessageSize-sizeof(F2FMessageHeader)-sizeof(F2FMessageJob))
		return F2FErrWierdError; 	/* TODO: deal with multiple blocks */
	jobmsg.job.size = htonl(group->jobarchivesize);
	jobmsg.job.blocknr = 0;
	/* Read job from file */
	FILE * jobfile = fopen( group->jobfilepath, "r");
	fread(jobmsg.data, group->jobarchivesize, 1, jobfile);
	/* TODO: check errors */
	/* send data */
	F2FError error = f2fPeerSendData( group, srcPeer, F2FMessageTypeJob, 
			(char *)& jobmsg, sizeof(F2FMessageJob) + group->jobarchivesize );
	fclose( jobfile );	
	return error;
}

/** process a Job */
/*static F2FError processJob( 
		F2FGroup *group, F2FPeer *srcPeer, F2FPeer *dstPeer,
		const F2FMessageJob *msg )
{
	char *jobdata = (char *) (msg+1);
	char buffer[F2FMaxMessageSize+1];
	F2FSize size = ntohl(msg->size);
	if (size > F2FMaxMessageSize )
		size = F2FMaxMessageSize;
	if (size < 0)
		return F2FErrMessageTooShort;
	memcpy(buffer,jobdata,size);
	buffer[size]=0;
	printf("Received the following job: \n---\n%s\n---\n", buffer);
	return F2FErrOK;
}*/

/** parse messages (internal or IM messages), they are already decoded 
 * and copied to the receiveBuffer */
static F2FError parseReceiveBuffer()
{
	if ( ! receiveBuffer.filled ) return F2FErrNothingAvail;
	
	F2FError error;
	F2FPeer *srcPeer = receiveBuffer.sourcePeer;
	F2FPeer *dstPeer = receiveBuffer.destPeer;
	F2FGroup *group = receiveBuffer.group;
	F2FSize len = receiveBuffer.size;
	char *buffer = receiveBuffer.buffer;
	
	/* TODO: evaluate fromPeer to the source Peer sent in the message
	 * for doing the proper routing */
	if( group == NULL ) /* group unknown */
		return F2FErrNotFound;
	/* srcPeer can be null, if it was an invite answer,
	 * where the actual source is not known */
	if( srcPeer != NULL ) /* is in the local peer list */
		srcPeer->lastActivity = time(NULL); // update timestamp
	/* TODO: Check length of the message extensions, should match! */ 
	/* process different messages */
	F2FSize mesExtLen = len;
	if(mesExtLen > F2FMaxMessageSize)
		mesExtLen = F2FMaxMessageSize;
	const char * mesExt = buffer/* + sizeof( F2FMessageHeader ) obsolet */;
	switch ( receiveBuffer.messageType )
	{
	case F2FMessageTypeInviteAnswer:
		error = processInviteMessageAnswer(
					group, srcPeer, dstPeer,
					(F2FMessageInviteAnswer *) mesExt );
		if( error != F2FErrOK ) return error;
		break;
	case F2FMessageTypeInvite:
		error = processInviteMessage(
					group, srcPeer, dstPeer, 
					(F2FMessageInvite *) mesExt);
		if( error != F2FErrOK ) return error;
		break;
	case F2FMessageTypeRaw:
		/* allready in there 
		 * if(receiveBuffer.filled)
			return F2FErrBufferFull;
		receiveBuffer.binary = 1;
		memcpy(receiveBuffer.buffer, mesExt, mesExtLen );
		receiveBuffer.size = mesExtLen;
		receiveBuffer.group = group;
		receiveBuffer.sourcePeer = srcPeer;
		receiveBuffer.destPeer = dstPeer;
		receiveBuffer.filled = 1; */
		return F2FErrNotParsed;
		break;
	case F2FMessageTypeText:
		/* allready in there 
		 * if(receiveBuffer.filled)
			return F2FErrBufferFull;
		receiveBuffer.binary = 0;
		memcpy(receiveBuffer.buffer, mesExt, mesExtLen );
		receiveBuffer.size = mesExtLen;
		receiveBuffer.group = group;
		receiveBuffer.sourcePeer = srcPeer;
		receiveBuffer.destPeer = dstPeer;
		receiveBuffer.filled = 1; */
		return F2FErrNotParsed;
		break;
	case F2FMessageTypeGetJobTicket:
		error = processGetJobTicket(
					group, srcPeer, dstPeer, 
					(F2FMessageGetJobTicket *) mesExt);
		if( error != F2FErrOK ) return error;
		break;
	case F2FMessageTypeGetJobTicketAnswer:
		error = processGetJobTicketAnswer(
					group, srcPeer, dstPeer, 
					(F2FMessageGetJobTicketAnswer *) mesExt);
		if( error != F2FErrOK ) return error;
		break;
	case F2FMessageTypeJob:
		/* not handled error = processJob(
					group, srcPeer, dstPeer, 
					(F2FMessageJob *) mesExt);
		if( error != F2FErrOK ) return error; */
		return F2FErrNotParsed;
		break;
	default:
		return F2FErrMessageTypeUnknown;
		break;
	}
	return F2FErrOK;
}

/** hand over messages from the IM program to the core, before this function can be
 * called the second time f2fGroupReceive must be called to be able to clear
 * the buffers.
 * The messages start with the right header and must be base64 encoded to be detectable.
 * If any other message is passed here, the function will return F2FErrNotF2FMessage. */
F2FError f2fNotifyCoreWithReceived( const F2FWord32 localPeerId,
		const F2FString identifier, const F2FString message, 
		const F2FSize size )
{
	F2FError error;
	char tmpbuffer[F2FMaxMessageSize];

	/* TODO: check reasonable? if( size < (sizeof( F2FMessageHeader )+2)/3 * 4)
			return F2FErrMessageTooShort; */
	error = f2fIMDecode(message, size, tmpbuffer, F2FMaxMessageSize);
	if( error != F2FErrOK ) return error;
	F2FMessageHeader *hdr = (F2FMessageHeader *) tmpbuffer;
	F2FGroup * group = f2fGroupListFindGroup(
			ntohl(hdr->groupID.hi), ntohl(hdr->groupID.lo) );
	F2FPeer * srcPeer = f2fPeerListFindPeer( 
			ntohl(hdr->sourcePeerID.hi), ntohl(hdr->sourcePeerID.lo) );
	F2FPeer * dstPeer = f2fPeerListFindPeer( 
			ntohl(hdr->destPeerID.hi), ntohl(hdr->destPeerID.lo) );
	// Special treatment for invitemessages
	if( hdr->messagetype == F2FMessageTypeInvite )
	{
		if( group == NULL ) /* the group does not exist, normal, when I am invited */
		{
			/* TODO: process the actual invite string and ask myself, if I want to be part
			 * of this computation group */
			F2FMessageInvite * msgiv = 
				(F2FMessageInvite *) (tmpbuffer + sizeof (F2FMessageHeader));
			F2FSize namelen = ntohl(msgiv->groupNameLength);
			if (namelen>F2FMaxNameLength) namelen=F2FMaxNameLength;
			char groupname[namelen+1];
			groupname[namelen] = 0;
			memcpy(groupname, msgiv->nameAndInvite, namelen);
			group = f2fGroupListAdd( groupname, 
					ntohl(hdr->groupID.hi), ntohl(hdr->groupID.lo) );
			if( group == NULL ) return F2FErrListFull;
		}
		/* TODO: is the else branch here important for security? */
		if( srcPeer == NULL ) /* not in the local peer list,  normal, when I am invited */
		{
			/* TODO: verify that this is really my friend contacting me */
			/* add to my peer list */
			srcPeer = f2fPeerListNew( ntohl(hdr->sourcePeerID.hi),	ntohl(hdr->sourcePeerID.lo) );
			if( srcPeer == NULL ) return F2FErrListFull;
			srcPeer->localPeerId = localPeerId;
			srcPeer->activeprovider = F2FProviderIM; // Only IM at this time
			srcPeer->identifier[F2FMaxNameLength] = 0;
			strncpy( srcPeer->identifier, identifier, F2FMaxNameLength );
		}
		if( dstPeer == NULL ) /* also very likely as this was a fake temporary number, which will be used
						* as a challenge */
		{
			dstPeer = myself; /* Assume invitation was for me */
		}
		/* TODO: is the else branch here important for security? */
	}
	if( group == NULL ) return F2FErrNotFound;
	/*if( srcPeer == NULL ) return F2FErrNotFound; can be NULL if InviteMessageAnswer */
	if( dstPeer == NULL ) return F2FErrNotFound;
	/* maybe check the src for Invitemessageanswers here */
	// parsing will be done later, obsolet: return parseMessage( tmpbuffer, size );
	return fillReceiveBuffer(
			group->id.hi, group->id.lo,
			ntohl(hdr->sourcePeerID.hi), ntohl(hdr->sourcePeerID.lo),
			dstPeer->id.hi, dstPeer->id.lo,
			hdr->messagetype,
			tmpbuffer + sizeof(F2FMessageHeader), ntohl(hdr->len) );
}

/** Prepare the Buffers - don't check, if they are filled.
 * Destpeer can be Null, then 0-ids will be set. */
static F2FError prepareSendBuffersWithData( const F2FGroup *group,
		const F2FPeer *destPeer,
		F2FMessageType type,
		const char * data, F2FSize len)
{
	F2FMessageHeader msg;

	msg.messagetype = type;
	msg.reserved[0] = 'F';
	msg.reserved[1] = '2';
	msg.reserved[2] = 'F';
	msg.groupID.hi = htonl( group->id.hi );
	msg.groupID.lo = htonl( group->id.lo );
	msg.sourcePeerID.hi = htonl( myself->id.hi );
	msg.sourcePeerID.lo = htonl( myself->id.lo );
	if(destPeer)
	{
		msg.destPeerID.hi = htonl( destPeer->id.hi );
		msg.destPeerID.lo = htonl( destPeer->id.lo );
	}
	else
	{
		msg.destPeerID.hi = 0; /** goes to multiple destinations */
		msg.destPeerID.lo = 0;
	}
	msg.len = htonl( len );
	/* prepare normal sendBuffer */
	if ( len > sizeof(sendBuffer.buffer)-sizeof(F2FMessageHeader) ) 
		return F2FErrMessageTooLong;
	memcpy( sendBuffer.buffer, &msg, sizeof(F2FMessageHeader) );
	memcpy( sendBuffer.buffer + sizeof(F2FMessageHeader), data, len);
	sendBuffer.size = len + sizeof(F2FMessageHeader);
	return F2FErrOK;	
}

/** Fill send buffer with data for all group members */
static F2FError groupSend( const F2FGroup *group, 
		F2FMessageType type,
		const char * message, F2FSize len)
{
	/* Check if send-buffers are empty */
	if(! f2fDataSent())
		return F2FErrBufferFull;
	/* fill the lists of peers to send data to */
	/* go through all peers in this group and add these to the
	 * respective sendlists */
	// obsolet - already asked sendBuffer.peercount = 0;
	// obsolet - already asked sendIMBuffer.peercount = 0;
	int peerindex;
	for( peerindex = 0; peerindex < group->listSize; peerindex ++)
	{
		F2FPeer *peer = group->sortedPeerList[peerindex].peer;
		if( peer->status == F2FPeerActive ) // Make sure, peer is active
		{
			if ( peer->activeprovider == F2FProviderIM )
				sendIMBuffer.localPeerIDlist[sendIMBuffer.peercount++] =
					peer->localPeerId;
			else
				sendBuffer.peerList[sendBuffer.peercount++] = peer;
		}
	}
	F2FError error = prepareSendBuffersWithData( group, NULL, type, message, len );
	if (error != F2FErrOK ) return error;
	/* and the IM endbuffer */
	if( sendIMBuffer.peercount > 0 )
		error = encodeIMMessage( sendBuffer.buffer, sendBuffer.size );
	return error;
}

/** Fill send buffer for a specific peer in a group */
F2FError f2fPeerSendData( const F2FGroup *group, F2FPeer *peer, 
		F2FMessageType type,
		const char *data, const F2FWord32 dataLen )
{
	/* TODO: make sure, bigger blocks can be sent !!! */
	F2FError error = prepareSendBuffersWithData( group, peer, type, data, dataLen );
	if (error != F2FErrOK ) return error;
	/* prepare the right destinations for the buffer
	 * in terms of provider */
	if (peer->activeprovider == F2FProviderIM )
	{
		/* fill the IM sendbuffer */
		error = encodeIMMessage( sendBuffer.buffer, sendBuffer.size );
		if ( error!= F2FErrOK ) return error;
		sendIMBuffer.localPeerIDlist[0] = peer->localPeerId;
		sendIMBuffer.peercount = 1;
	}
	else
	{
		sendBuffer.peerList[0] = peer;
		sendBuffer.peercount = 1;
	}
	return F2FErrOK;
}

/** Fill send buffer with data for all group members */
F2FError f2fGroupSendData( const F2FGroup *group, 
		const char * message, F2FSize len )
{
	return groupSend( group, F2FMessageTypeRaw, message, len );
}

/** Fill send buffer with a text message for all group members */
F2FError f2fGroupSendText( const F2FGroup *group, const F2FString message )
{
	F2FSize len = strnlen( message, F2FMaxMessageSize );
	return groupSend( group, F2FMessageTypeText, message, len );
}

/** test, if data in buffer has been sent (Buffers are empty) */
int f2fDataSent( )
{
	return sendBuffer.peercount == 0 && sendIMBuffer.peercount == 0;
}

/** empty, send buffers for data, even if it has not been sent */
F2FError f2fEmptyData()
{
	/* TODO: maybe return F2FErrBufferFull if buffer not empty before? */
	sendBuffer.peercount = 0;
	sendIMBuffer.peercount = 0;
	return F2FErrOK;
}

/** submit a job to a f2f group
 * This will first ask for allowance tickets from 
 * every client in the job group. If at a later point
 * (when the job is already started) clients (more slaves)
 * are added to the group, they will be directly asked for a ticket.
 * If tickets are received back, the job will be sent to these clients.
 * The Job must be available as a local file in a special archive format
 */ 
F2FError f2fGroupSubmitJob( F2FGroup *group, const char * jobpath )
{
	F2FMessageGetJobTicket ticketRequest;
	struct stat jobfilestat;
	F2FSize jobpathlen;
	
	jobpathlen = strnlen( jobpath, F2FMaxPathLength + 1 );
	if(jobpathlen > F2FMaxPathLength )
		return F2FErrPathTooLong;
	if(stat(jobpath, & jobfilestat)<0)
		return F2FErrFileOpen;
	if( ! S_ISREG( jobfilestat.st_mode ) )
		return F2FErrFileOpen;
	group -> jobfilepath[jobpathlen] = 0;
	memcpy(group->jobfilepath, jobpath, jobpathlen );
	group -> jobarchivesize =  jobfilestat.st_size;
	ticketRequest.archivesize = jobfilestat.st_size;
	ticketRequest.hdspace = 0; /* Nothing at the moment, TODO: use */
	groupSend( group, F2FMessageTypeGetJobTicket,
			(char *) &ticketRequest, sizeof(ticketRequest) );
	/** TODO: Should a challenge be sent out, to make sure, that only the clients
	 * addressed here can answer? */
	return F2FErrOK;
}

/** Send a job ticket request to a peer which joined later */
F2FError f2fPeerSubmitJob( const F2FGroup *group, F2FPeer *peer )
{
	F2FMessageGetJobTicket ticketRequest;

	ticketRequest.archivesize = group->jobarchivesize;
	ticketRequest.hdspace = 0; /* Nothing at the moment, TODO: use */
	f2fPeerSendData(group, peer, F2FMessageTypeGetJobTicket,
			(char *) &ticketRequest, sizeof(ticketRequest) );
	/** TODO: Should a challenge be sent out, to make sure, that only the clients
	 * addressed here can answer? */
	return F2FErrOK;
}

/** distribute file */
F2FError f2fGroupDistributeFile( const char * publishname,
		const char * filepath )
{
	// TODO: implement
	return F2FErrOK;
}

/** distribute data in distr. hash table */
F2FError f2fGroupDistributeData( const char * publishname,
		char * memorypool, F2FSize len )
{
	// TODO: implement
	return F2FErrOK;
}
