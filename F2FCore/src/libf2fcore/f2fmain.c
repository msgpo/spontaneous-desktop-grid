/***************************************************************************
 *   Filename: f2fmain.h
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
 *   This is the main part the F2FCore implementation
 ***************************************************************************/

#include <stdio.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <arpa/inet.h>

#include "mtwist/mtwist.h"
#include "b64/b64.h"
#include "f2fmain.h"
#include "f2fpeerlist.h"
#include "f2fgrouplist.h"
#include "f2fticketrequest.h"
#include "f2fadapterreceivebuffer.h"

static int initWasCalled = 0; // this will be set if init was successful
static F2FError globalError = F2FErrOK; // a global error variable

F2FError f2fGetErrorCode()
{ return globalError; }

/** Static state vector for randomness of mersenne twister, this is global in this module */
static mt_state randomnessState;

static F2FPeer *myself; /* saves own peer information */

/** Internal send buffer, this can be also sent to multiple peers 
 * This is used to store data, which should be sent internally or via the network
 * It is not used to send data via IM */
static struct
{
	char buffer[F2FMaxMessageSize];
	F2FSize size; /* how much is filled */
	F2FPeer * peerList[ F2FMaxPeers ];
	F2FSize peercount; /* the number of ids in list */
} sendBuffer;

/* this is a secure strlen with no buffer overrun */
static inline F2FSize strnlen( const char *str, F2FSize max )
{
	F2FSize size;
	for (size = 0; size < max; ++size)
	{ if( ! str[size] ) break; }
	return size;
}

// Forward references
F2FError f2fPeerSubmitJob( F2FGroup *group, F2FPeer *peer );
static F2FError prepareBufferWithData( char * buffer, 
		const F2FGroup *group,
		const F2FPeer *destPeer,
		F2FMessageType type,
		const char * data, F2FSize len);
static F2FError parseBuffer(F2FPeer *srcPeer, F2FPeer *dstPeer, F2FGroup *group, 
		F2FMessageType type, const char * buffer, F2FSize buffersize );

/** Do the initialization - especially create a random seed and get your own PeerID
 * Must be called first.
 * Gets the name of this peer (for example "Ulrich Norbisrath's peer") and the public key */
F2FPeer * f2fInit( const F2FString myName, const F2FString myPublicKey )
{
#define MAXBUFFERSIZE 1024
	char buffer[MAXBUFFERSIZE+1]; // Buffer for concatenation of name and key
	buffer [MAXBUFFERSIZE]=0; // Make sure this is 0 terminated
	F2FSize buffersize;
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
	sendBuffer.size = 0;
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
static F2FError encodeIMMessage( const char * message,
		const F2FSize size, F2FAdapterReceiveMessage *dest )
{
	F2FSize currentsize, newsize;

	/* Encode message for sending in f2f framework */
	strcpy( dest->buffer, F2FMessageMark ); /* header */
	currentsize = F2FMessageMarkLength;
	newsize = b64encode( message, dest->buffer + currentsize,
				size, F2FMaxEncodedMessageSize - currentsize );
	if(newsize == 0) return F2FErrMessageTooLong;
	currentsize += newsize;	/* prepare sending of the new message */
	dest->buffersize = currentsize;
	return F2FErrOK;
}

///** prepare message to send an IM message to one locally known peer */
///* TODO: unused???!!! */
//static F2FAdapterReceiveMessage * f2fIMSend( const F2FWord32 localpeerid, const F2FString message,
//		const F2FSize size )
//{
//	F2FError error;
//
//	// Get free buffer
//	F2FAdapterReceiveMessage * freebuffer = f2fAdapterReceiveBufferReserve();
//	if( freebuffer == NULL ) // not space left
//	{
//		return NULL;
//	}
//	freebuffer->buffertype = F2FAdapterReceiveMessageTypeIMForward;
//	error = encodeIMMessage( message, size, freebuffer );
//	freebuffer->localPeerIDlist[ 0 ] = localpeerid;
//	freebuffer->peercount = 1;
//	return freebuffer;
//}

///** Add a local peer to the sendIMBuffer */
///* TODO: unused???!!! */
//static F2FError f2fIMAddPeer( const F2FWord32 localpeerid, F2FAdapterReceiveMessage *dest )
//{
//	if( dest->peercount >= F2FMaxPeers ) return F2FErrListFull;
//	dest->localPeerIDlist[ ++ dest->peercount ] = localpeerid;
//	return F2FErrOK;
//}

/* Decode a received instant message */
static F2FError f2fIMDecode( const F2FString message, const F2FSize messagelen,
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
 * which could be compared to an allready cached one, to create an own challenge, and later used to
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

/** tries to receive a message. If succesful, this gives a pointer to a receive-message.
 * If not, the returned messagepointer will be NULL and F2FErrNothingAvail will be in F2FError.
 * This routine must be called on a regulary interval - it can't be used in parallel to
 * the other methods here in this interface. */
F2FAdapterReceiveMessage * f2fReceiveMessage()
{
	/* Try now to receive stuff from the internal network (not IM) */
	/* call then parseMessage, this might fill the receiveBuffer */
	// TODO: necessary? f2fProcess(); // Do some processing and fill eventually the buffer TODO: check error
	F2FAdapterReceiveMessage * msg = f2fAdapterReceiveBufferGetMessage();
	if( msg == NULL)
	{
		globalError = F2FErrNothingAvail;
		return NULL;
	}
	globalError = F2FErrOK;
	return msg;
}

static F2FError fillAdapterReceiveBuffer( F2FAdapterReceiveMessageType buffertype,
		F2FWord32 grouphi, F2FWord32 grouplo,
		F2FWord32 srchi, F2FWord32 srclo,
		F2FWord32 desthi, F2FWord32 destlo,
		F2FMessageType type, const char *buf, F2FSize size )
{
	F2FGroup *group;
	F2FPeer *src, *dst;

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
		// TODO: check, what is if this destination has not th IM-Provider
	//	if(dst != myself)
	//	{
	//		printf("Found a package to route - not supported, package discarded.\n");
	//		return F2FErrOK; // Routing not supported at the moment
	//	}
	//	// TODO: Consider, what has to be done for routing
	}
	if(!dst) return F2FErrNotFound;
	// Acquire a new ReceiveBuffer
	F2FAdapterReceiveMessage * newbuffer = f2fAdapterReceiveBufferReserve();
	if( newbuffer == NULL )
		return F2FErrBufferFull;
	newbuffer->group = group;
	newbuffer->sourcePeer = src;
	newbuffer->destPeer = dst;
	newbuffer->buffertype = buffertype;
	newbuffer->messagetype = type;
	newbuffer->buffersize = size;
	newbuffer->localPeerIDlist[0] = dst->localPeerId;
	newbuffer->peercount = 1;
	memcpy( newbuffer->buffer, buf, size );
	return F2FErrOK;
}

/** Send content of the local sendBuffer to ALL destination peers.
 * It can happen that this function immediatly fills the local receive-buffer.
 * At the moment there are no more effects on the internal buffers.
 * Make sure that all IM messages are sent before this method is called.
 * However, the internal send buffers should be cleared after this call. */
static F2FError f2fSend(F2FPeer * srcPeer, F2FPeer * dstPeer, F2FGroup * group)
{
	F2FError error;
	F2FMessageHeader * hdr;

	while(sendBuffer.peercount > 0)
	{
		/* Take the first peer and process message to it */
		F2FPeer * peer = sendBuffer.peerList[sendBuffer.peercount - 1];
		/* Find the communication provider, which helps
		 * us to reach the destination */
		switch( peer->activeprovider )
		{
		case F2FProviderMyself:
			/* here we can just evaluate the received data */
			hdr = (F2FMessageHeader *) & sendBuffer.buffer;
			error = parseBuffer( srcPeer, dstPeer, group, 
							hdr->messagetype, sendBuffer.buffer + sizeof(F2FMessageHeader),
							ntohl(hdr->len) );
			if(error!=F2FErrOK) return error;
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
	// should be done automatically f2fSetSentBufferEmpty(); /* Release the SentBuffer again */
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

	/* Check if peer waits for an invite answer*/
	F2FPeer * waitingPeer = f2fPeerListFindPeer(
					ntohl(msg->tmpIDAndChallenge.hi), ntohl(msg->tmpIDAndChallenge.lo) );
	if( ! waitingPeer )
		return F2FErrNotAuthenticated;
	if( waitingPeer->status != F2FPeerWaitingForInviteConfirm )
		return F2FErrNotAuthenticated;
	if( f2fPeerFindGroupIndex( waitingPeer, group ) < 0 )
		return F2FErrNotAuthenticated;
	if(! f2fTestSentBufferEmpty() ) /* as joining has to be announced, check send buffer */
		return F2FErrBufferFull;
	F2FAdapterReceiveMessage * freebuffer =  f2fAdapterReceiveBufferReserve();
	if( freebuffer == NULL )
		return F2FErrBufferFull;
	freebuffer->peercount = 0;
	/* Change the id to the official id */
	error = f2fPeerChangeUID( waitingPeer,
			ntohl(msg->realSourceID.hi), ntohl(msg->realSourceID.lo) );
	if( error != F2FErrOK ) return error;
	waitingPeer -> status = F2FPeerActive; /* active now */
	
	/* Send updates to the remaining peers, that there is a new group member */
	F2FMessageAnnounceNewPeerInGroup announcemsg;
	announcemsg.newpeer.lo = htonl(waitingPeer->id.lo);
	announcemsg.newpeer.hi = htonl(waitingPeer->id.hi);
	int peerindex;
	for( peerindex = 0; peerindex < group->listSize; peerindex ++)
	{
		F2FPeer *peer = group->sortedPeerList[peerindex].peer;
		if( peer->status == F2FPeerActive ) // Make sure, peer is active
		{
			if ( peer != myself	&& peer != waitingPeer ) /* Don't send to sender and myself */
			{
				if ( peer->activeprovider == F2FProviderIM  )
				{
					freebuffer->localPeerIDlist[freebuffer->peercount++] =
						peer->localPeerId;
				}
				else sendBuffer.peerList[sendBuffer.peercount++] = peer;
			}
		}
	}
	error = prepareBufferWithData( sendBuffer.buffer, group, NULL,
			F2FMessageTypeAnnounceNewPeerInGroup, (char *)& announcemsg, sizeof(F2FMessageAnnounceNewPeerInGroup) );
	if (error != F2FErrOK ) return error;
	/* and the IM sendbuffer */
	if( freebuffer->peercount > 0 )
	{
		error = encodeIMMessage( sendBuffer.buffer, sendBuffer.size, freebuffer );
		if (error != F2FErrOK ) return error;
		freebuffer->buffertype = F2FAdapterReceiveMessageTypeIMForward;
		//debug printf("processInviteMessageAnswer: sending Announcement -->%s<--.\n",freebuffer->buffer);
	}
	else f2fAdapterReceiveBufferRelease( freebuffer );
	
	error = f2fSend(myself, NULL, group); /* Send, what is in the buffer */
	if (error != F2FErrOK ) return error;	
	/* Announcement sent */
	
	/* Send a list of known peers to the new peer */
	F2FMessagePeerListInfo peerlistinfo;
	int entrynr=0;
	for( peerindex = 0; peerindex < group->listSize; peerindex++ )
	{
		F2FPeer * infopeer = group->sortedPeerList[peerindex].peer;
		 // only record new and active peers (inactive ones will send an announcement later)
		if(infopeer != myself && infopeer != waitingPeer 
				&& infopeer->status == F2FPeerActive)
		{
			peerlistinfo.ids[entrynr].hi = htonl(infopeer->id.hi);
			peerlistinfo.ids[entrynr].lo = htonl(infopeer->id.lo);
			entrynr ++;
		}
		if(entrynr == F2FMaxPeerListInfoEntries)
		{
			peerlistinfo.count = htonl(entrynr);
			error = f2fPeerSendData(group,waitingPeer,F2FMessageTypePeerListInfo,
					(char *)&peerlistinfo,
					sizeof(peerlistinfo));
			if (error != F2FErrOK ) return error;			
			entrynr == 0;
		}
	}
	if(entrynr>0) //sent the rest
	{
		peerlistinfo.count =  htonl(entrynr);
		error = f2fPeerSendData(group,waitingPeer,F2FMessageTypePeerListInfo,
				(char *)&peerlistinfo,
				sizeof(peerlistinfo)-(F2FMaxPeerListInfoEntries-entrynr)*sizeof(F2FUID));
		if (error != F2FErrOK ) return error;					
	}
	/* list of known peers sent */
		
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
			F2FError error = f2fPeerSubmitJob( waitingPeer->groups[index], waitingPeer );
			if(error != F2FErrOK)
			{
				printf("processInviteMessageAnswer: Could not submit job to new peer!!!.\n");
				return error;
			}
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
	jobmsg.job.blocknr = htonl(0);
	/* Set the ticket */
	jobmsg.job.ticket.hi = msg->ticket.hi; /* Endian transfer not necessary */
	jobmsg.job.ticket.lo = msg->ticket.lo;
	jobmsg.job.ticket.validUntil = msg->ticket.validUntil;
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

/** process a Job
 * check, if ticket is correct on evtl. discard buffer
 * */
static F2FError processJob(
		F2FGroup *group, F2FPeer *srcPeer, F2FPeer *dstPeer,
		const F2FMessageJob *msg )
{
	F2FSize size = ntohl(msg->size);
	if (size > F2FMaxMessageSize )
		size = F2FMaxMessageSize;
	if (size < 0)
		return F2FErrMessageTooShort;
	/* check ticket */
	F2FGroupPeer * grouppeer = f2fGroupFindGroupPeer(group, srcPeer->id.hi, srcPeer->id.lo );
	if( grouppeer == NULL) return F2FErrNotFound;
	/* TODO: also check if ticket is still valid */
	if( ntohl(msg->ticket.hi) ==  grouppeer->receiveTicket.hi
			&& ntohl(msg->ticket.lo) ==  grouppeer->receiveTicket.lo )
	{
		return fillAdapterReceiveBuffer(F2FAdapterReceiveMessageTypeData, 
				group->id.hi, group->id.lo, srcPeer->id.hi, srcPeer->id.lo, 
				dstPeer->id.hi, dstPeer->id.lo, F2FMessageTypeJob, 
				((char *) msg )+ sizeof(F2FMessageJob), size );
	}
	else
	{
		return F2FErrNotAuthenticated;
	}
}

static F2FError processAnnounceNewPeerInGroup(F2FGroup *group, F2FPeer *srcPeer,
		F2FPeer *dstPeer, F2FMessageAnnounceNewPeerInGroup * msgannounce)
{
	if (dstPeer == NULL) // TODO: Here should be more security
	{
		dstPeer = myself;
	}
	if (srcPeer == NULL) // need to know this as I trust this one
	{
		return F2FErrNotAuthenticated;
	}
	F2FPeer * newPeer = f2fPeerListNew(ntohl(msgannounce->newpeer.hi),
			ntohl(msgannounce->newpeer.lo) );
	if (newPeer == NULL)
		return F2FErrListFull;
	newPeer->localPeerId = srcPeer->localPeerId;
	newPeer->activeprovider = srcPeer->activeprovider; // Only IM at this time
	strncpy(srcPeer->identifier, "unknown", F2FMaxNameLength);
	return f2fGroupAddPeer(group, newPeer);
}

static F2FError processPeerListInfo(F2FGroup *group, F2FPeer *srcPeer,
		F2FPeer *dstPeer, F2FMessagePeerListInfo * peerlistinfo)
{
	F2FError error;
	
	if (dstPeer == NULL) // TODO: Here should be more security
	{
		return F2FErrNotFound;
	}
	if (srcPeer == NULL) // need to know this as I trust this one
	{
		return F2FErrNotAuthenticated;
	}
	int index;
	for( index = 0; index < ntohl(peerlistinfo->count); index++)
	{
		F2FUID currentid;
		currentid.hi = ntohl(peerlistinfo->ids[index].hi);
		currentid.lo = ntohl(peerlistinfo->ids[index].lo);
		// check if peer is known
		F2FPeer * infopeer = f2fPeerListFindPeer(currentid.hi, currentid.lo);
		if( infopeer != NULL ) // if yes
		{ // Check is it already in this group
			if( f2fGroupFindPeer(group, currentid.hi, currentid.lo ) == NULL )
			{
				error = f2fGroupAddPeer(group, infopeer);
				if(error!=F2FErrOK) return error;
			}
		}
		else
		{ // Add it to group and peerlist
			infopeer = f2fPeerListNew(currentid.hi, currentid.lo);
			if (infopeer == NULL)
				return F2FErrListFull;
			infopeer->localPeerId = srcPeer->localPeerId;
			infopeer->activeprovider = srcPeer->activeprovider; // Only IM at this time
			strncpy(srcPeer->identifier, "unknown", F2FMaxNameLength);
			error = f2fGroupAddPeer(group, infopeer);
			if(error!=F2FErrOK) return error;
		}
	}
	return F2FErrOK;
}

/** parse messages (internal or IM messages), they are already decoded */
static F2FError parseBuffer(F2FPeer *srcPeer, F2FPeer *dstPeer, F2FGroup *group, 
		F2FMessageType type, const char * buffer, F2FSize buffersize )
{
	F2FError error;

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
	F2FSize mesExtLen = buffersize;
	if(mesExtLen > F2FMaxMessageSize)
		mesExtLen = F2FMaxMessageSize;
	const char * mesExt = buffer/* + sizeof( F2FMessageHeader ) obsolet */;
	switch ( type )
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
		fillAdapterReceiveBuffer(F2FAdapterReceiveMessageTypeData, 
				group->id.hi, group->id.lo, srcPeer->id.hi, srcPeer->id.lo, 
				dstPeer->id.hi, dstPeer->id.lo, type, 
				mesExt, mesExtLen );
		// they are parsed now return F2FErrNotParsed;
		return F2FErrOK;
		break;
	case F2FMessageTypeText:
		fillAdapterReceiveBuffer(F2FAdapterReceiveMessageTypeData, 
				group->id.hi, group->id.lo, srcPeer->id.hi, srcPeer->id.lo, 
				dstPeer->id.hi, dstPeer->id.lo, type, 
				mesExt, mesExtLen );
		// they are parsed now return F2FErrNotParsed;
		return F2FErrOK;
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
		error = processJob(
					group, srcPeer, dstPeer,
					(F2FMessageJob *) mesExt);
		if( error != F2FErrOK ) return error;
		// they are parsed now else return F2FErrNotParsed;
		return F2FErrOK;
		break;
	case F2FMessageTypeAnnounceNewPeerInGroup:
		error = processAnnounceNewPeerInGroup(
				group, srcPeer, dstPeer,
				(F2FMessageAnnounceNewPeerInGroup *) mesExt);
		if( error != F2FErrOK ) return error;
		break;
	case F2FMessageTypePeerListInfo:
		error = processPeerListInfo(
				group, srcPeer, dstPeer,
				(F2FMessagePeerListInfo *) mesExt);
		if( error != F2FErrOK ) return error;
		break;
	default:
		return F2FErrMessageTypeUnknown;
		break;
	}
	return F2FErrOK;
}

/** Forward messages from the IM program to the core.
 * To avoid letting the receivebuffer get too full 
 * f2fReceive should be called to be able to clear
 * the receive buffers.
 * The messages have to start with the right header and must be base64 encoded to be detectable.
 * If any other message is passed here, the function will return F2FErrNotF2FMessage.
 * We send here the local peerid and the local identifier as this peer might not
 * be in our peer list.
 *
 * @param F2FString identifier - display name of contact in list
 * @param F2FWord32 localPeerId - local reference for Peer
 *
 * */
F2FError f2fForward( const F2FWord32 localPeerId,
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
	F2FPeer * dstPeer;
	if(ntohl(hdr->destPeerID.hi) == 0 && ntohl(hdr->destPeerID.lo) == 0)
		dstPeer = myself;
	else dstPeer = f2fPeerListFindPeer(
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
	if( dstPeer == myself )
		return parseBuffer( srcPeer, dstPeer, group, 
				hdr->messagetype, tmpbuffer + sizeof(F2FMessageHeader), ntohl(hdr->len) );
	// Message is not for myself, so send it either internally or give it back to send via IM
	if( dstPeer->activeprovider == F2FProviderIM )
		return fillAdapterReceiveBuffer( F2FAdapterReceiveMessageTypeIMForward,
			group->id.hi, group->id.lo,
			ntohl(hdr->sourcePeerID.hi), ntohl(hdr->sourcePeerID.lo),
			dstPeer->id.hi, dstPeer->id.lo,
			hdr->messagetype,
			message, size ); // This implements a simple routing
	printf("f2fForward: Provider not supported.\n"); // TODO: other providers
	return F2FErrOK;
}

/** Prepare the Buffers - don't check, if they are filled.
 * Destpeer can be Null, then 0-ids will be set. */
static F2FError prepareBufferWithData( char * buffer, 
		const F2FGroup *group,
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
	memcpy( buffer, &msg, sizeof(F2FMessageHeader) );
	memcpy( buffer + sizeof(F2FMessageHeader), data, len);
	sendBuffer.size = len + sizeof(F2FMessageHeader);
	return F2FErrOK;
}

/** Fill send buffer with data for all group members */
static F2FError groupSend( F2FGroup *group,
		F2FMessageType type,
		const char * message, F2FSize len)
{
	/* Check if send-buffers are empty */
	if(! f2fTestSentBufferEmpty())
		return F2FErrBufferFull;
	/* fill the lists of peers to send data to */
	/* go through all peers in this group and add these to the
	 * respective sendlists */
	// obsolet - already asked sendBuffer.peercount = 0;
	// obsolet - already asked sendIMBuffer.peercount = 0;
	// Get a receive buffer for the adapter
	F2FAdapterReceiveMessage * freebuffer =  f2fAdapterReceiveBufferReserve();
	if( freebuffer == NULL )
		return F2FErrBufferFull;
	freebuffer->peercount = 0;
	int peerindex;
	for( peerindex = 0; peerindex < group->listSize; peerindex ++)
	{
		F2FPeer *peer = group->sortedPeerList[peerindex].peer;
		if( peer != myself && peer->status == F2FPeerActive ) // Make sure, peer is active and not myself
		{
			if ( peer->activeprovider == F2FProviderIM )
				freebuffer->localPeerIDlist[freebuffer->peercount++] =
					peer->localPeerId;
			else
				sendBuffer.peerList[sendBuffer.peercount++] = peer;
		}
	}
	char buffer[F2FMaxMessageSize];
	F2FError error = prepareBufferWithData( buffer, group, NULL, type, message, len );
	if (error != F2FErrOK ) return error;
	/* and the IM endbuffer */
	if( freebuffer->peercount > 0 )
	{
		error = encodeIMMessage( sendBuffer.buffer, sendBuffer.size, freebuffer );
		if (error != F2FErrOK ) return error;
		freebuffer->buffertype = F2FAdapterReceiveMessageTypeIMForward;
	}
	else f2fAdapterReceiveBufferRelease( freebuffer ); /* Was not necessary */
	error = f2fSend(myself, NULL, group); /* send it out */
	if (error != F2FErrOK ) return error;
	// Send the message to myself
	return parseBuffer(myself,myself,group,type,message,len);
}

/** Send data to specific peer in a group */
F2FError f2fPeerSendData( F2FGroup *group, F2FPeer *peer,
		F2FMessageType type,
		const char *data, const F2FWord32 dataLen )
{
	if( peer == myself)
	{
		return parseBuffer(myself,myself,group,type,data,dataLen);
	}
	if(! f2fTestSentBufferEmpty())
		return F2FErrBufferFull;
	/* TODO: make sure, bigger blocks can be sent !!! */
	F2FError error = prepareBufferWithData( sendBuffer.buffer, group, peer, type, data, dataLen );
	if (error != F2FErrOK ) return error;
	/* prepare the right destinations for the buffer
	 * in terms of provider */
	if (peer->activeprovider == F2FProviderIM )
	{
		F2FAdapterReceiveMessage * freebuffer =  f2fAdapterReceiveBufferReserve();
		if( freebuffer == NULL )
			return F2FErrBufferFull;
		/* fill the IM sendbuffer */
		error = encodeIMMessage( sendBuffer.buffer, sendBuffer.size, freebuffer );
		if ( error!= F2FErrOK ) return error;
		freebuffer->localPeerIDlist[0] = peer->localPeerId;
		freebuffer->peercount = 1;
		freebuffer->buffertype = F2FAdapterReceiveMessageTypeIMForward;
	}
	else
	{
		sendBuffer.peerList[0] = peer;
		sendBuffer.peercount = 1;
		return f2fSend(myself, peer, group);
	}
	return F2FErrOK;
}

/** Fill send buffer for a specific peer in a group with raw data */
F2FError f2fPeerSendRaw( F2FGroup *group, F2FPeer *peer,
		const char *data, const F2FWord32 dataLen )
{
	return f2fPeerSendData( group, peer, F2FMessageTypeRaw, data, dataLen );
}

/** Fill send buffer with data for all group members */
F2FError f2fGroupSendData( F2FGroup *group,
		const char * message, F2FSize len )
{
	return groupSend( group, F2FMessageTypeRaw, message, len );
}

/** Fill send buffer with a text message for all group members */
F2FError f2fGroupSendText( F2FGroup *group, const F2FString message )
{
	F2FSize len = strnlen( message, F2FMaxMessageSize );
	return groupSend( group, F2FMessageTypeText, message, len );
}

/** test, if data in buffer has been sent (Buffers are empty) */
int f2fTestSentBufferEmpty( )
{
	return sendBuffer.peercount == 0;
}

/** empty, send buffers for data, even if it has not been sent */
F2FError f2fSetSentBufferEmpty()
{
	/* TODO: maybe return F2FErrBufferFull if buffer not empty before? */
	sendBuffer.peercount = 0;
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
	ticketRequest.archivesize = htonl(group -> jobarchivesize);
	ticketRequest.hdspace = 0; /* Nothing at the moment, TODO: use */
	groupSend( group, F2FMessageTypeGetJobTicket,
			(char *) &ticketRequest, sizeof(ticketRequest) );
	/** TODO: Should a challenge be sent out, to make sure, that only the clients
	 * addressed here can answer? */
	return F2FErrOK;
}

/** Send a job ticket request to a peer which joined later */
F2FError f2fPeerSubmitJob( F2FGroup *group, F2FPeer *peer )
{
	F2FMessageGetJobTicket ticketRequest;
	F2FError error;

	ticketRequest.archivesize = htonl( group->jobarchivesize );
	ticketRequest.hdspace = 0; /* Nothing at the moment, TODO: use */
	error = f2fPeerSendData(group, peer, F2FMessageTypeGetJobTicket,
			(char *) &ticketRequest, sizeof(ticketRequest) );
	/** TODO: Should a challenge be sent out, to make sure, that only the clients
	 * addressed here can answer? */
	return error;
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

