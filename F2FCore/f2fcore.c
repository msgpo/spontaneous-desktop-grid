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

#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <arpa/inet.h>

#include "mtwist/mtwist.h"
#include "b64/b64.h"
#include "f2fcore.h"
#include "f2fpeerlist.h"
#include "f2fgrouplist.h"
#include "f2fmessagetypes.h"

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
	F2FSize localidscount; /* the number of ids in list */
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
	F2FSize localidscount; /* the number of ids in list */
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
	if (sendIMBuffer.localidscount <= 0) return -1; // none left
	return sendIMBuffer.localPeerIDlist[-- sendIMBuffer.localidscount];
}

/** Receive buffer */
static struct
{
	F2FGroup *group;
	F2FPeer *sourcePeer;
	F2FPeer *destPeer;
	int filled; /** indicate, if something is in here */
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
	sendIMBuffer.localidscount = 0;
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
	if( group ) return group;
	globalError = F2FErrListFull;
	return NULL;
}

/** prepare message to send a IM message to one locally known peer */
F2FError f2fIMSend( const F2FWord32 localpeerid, const F2FString message, 
		const F2FSize size )
{
	F2FSize currentsize, newsize;
	
	if( sendIMBuffer.localidscount > 0 ) // not empty
	{
		return F2FErrBufferFull;
	}
	/* Encode message for sending in f2f framework */
	strcpy( sendIMBuffer.buffer, F2FMessageMark ); /* header */
	currentsize = F2FMessageMarkLength;
	newsize = b64encode( message, sendIMBuffer.buffer + currentsize,
				size, F2FMaxEncodedMessageSize-currentsize );
	currentsize += newsize;	/* prepare sending of the new message */
	sendIMBuffer.localPeerIDlist[ 0 ] = localpeerid;
	sendIMBuffer.localidscount = 1;
	sendIMBuffer.size = currentsize;
	return F2FErrOK;
}

/** Add a local peer to the sendIMBuffer */
F2FError f2fIMAddPeer( const F2FWord32 localpeerid )
{
	if( sendIMBuffer.localidscount >= F2FMaxPeers ) return F2FErrListFull;
	sendIMBuffer.localPeerIDlist[ ++ sendIMBuffer.localidscount ] = localpeerid;
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

	F2FMessageInvite mes;
	
	/* first create ID for new peer and save this peer as an unconfirmed peer */
	F2FPeer *newpeer = f2fPeerListNew( f2fRandomNotNull(), f2fRandomNotNull() );
	/* This id here is only temporary and only used as challenge, so the client can 
	 * authenticate itself, that it really knows this number.
	 * It is temporarely saved in the peer's id and replaced by 
	 * the real id, when the peer sends its real id. */
	/* initialize  the newly allocated peer */
	newpeer->localPeerId = localPeerId;
	newpeer->status = F2FPeerWaitingForInviteConfirm;
	newpeer->lastActivity = time( NULL );
	newpeer->identifier[F2FMaxNameLength] = 0;
	f2fPeerAddToGroup( newpeer, group );
	strncpy( newpeer->identifier, identifier, F2FMaxNameLength);
	
	/* prepare the outgoing message */
	mes.sourcePeerID.hi = htonl(myself->id.hi);
	mes.sourcePeerID.lo = htonl(myself->id.lo);
	mes.tmpIDAndChallenge.hi = htonl( newpeer->id.hi ); 
	mes.tmpIDAndChallenge.lo = htonl( newpeer->id.lo );
	mes.groupID.hi = htonl( group->id.hi );
	mes.groupID.lo = htonl( group->id.lo );
	mes.messagetype = F2FMessageTypeInvite;
	mes.groupNameLength = strnlen( identifier, F2FMaxNameLength );
	memcpy( mes.nameAndInvite, identifier, mes.groupNameLength );
	mes.inviteLength = strnlen( inviteMessage, F2FMaxNameLength );
	memcpy( mes.nameAndInvite + mes.groupNameLength, identifier, mes.inviteLength );

	/* send the message out to the respective peer via IM */
	return f2fIMSend( localPeerId, (F2FString)&mes, sizeof(F2FMessageInvite) 
			- 2 * F2FMaxNameLength + mes.groupNameLength + mes.inviteLength );
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
	if(receiveBuffer.filled) // Can receive with full buffer
		return F2FErrBufferFull;
	/* From here on receive.filled is 0 */
	/* Try now to receive stuff from the network */
	/* TODO: select(... */
	return F2FErrOK;
}

/** return 1, if there is data in the ReceiveBuffer */
int f2fReceiveBufferIsFilled()
{ return receiveBuffer.filled; }

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

/** show that the buffer has been read and can be filled again */
F2FError f2fReceiveBufferRelease()
{
	receiveBuffer.filled = 0;
	return F2FErrOK;
}

/** process an InviteMessage sent to me, react to this invite */
static F2FError processInviteMessage( const F2FWord32 localPeerId,
		const F2FString identifier, const F2FMessageInvite *msg)
{
	/* check if I know this peer already */
	F2FPeer * srcPeer = f2fPeerListFindPeer( ntohl(msg->sourcePeerID.hi), ntohl(msg->sourcePeerID.lo) );
	/* TODO: verify that this is really my friend contacting me */
	if( srcPeer == NULL ) /* not in the local peer list */
	{
		/* add to my peer list */
		srcPeer = f2fPeerListNew( ntohl(msg->sourcePeerID.hi), ntohl(msg->sourcePeerID.lo) );
		if( srcPeer == NULL ) return F2FErrListFull;
		srcPeer->localPeerId = localPeerId;
		srcPeer->identifier[F2FMaxNameLength] = 0;
		strncpy( srcPeer->identifier, identifier, F2FMaxNameLength );
		srcPeer->status = F2FPeerActive;
		srcPeer -> activeprovider = F2FProviderIM; /* Contact at the moment via IM */
	}
	srcPeer->lastActivity = time(NULL);
	F2FGroup *group = f2fGroupListFindGroup( ntohl(msg->groupID.hi), 
			ntohl(msg->groupID.lo) );
	if( group == NULL ) /* the group does not exist, normal, when I am invited */
	{
		/* TODO: process the actual invite string and ask myself, if I want to be part
		 * of this computation group */
		F2FSize namelen = msg->groupNameLength;
		if (namelen>F2FMaxNameLength) namelen=F2FMaxNameLength;
		char groupname[namelen+1];
		groupname[namelen] = 0;
		memcpy(groupname, msg->nameAndInvite, namelen);
		group = f2fGroupListAdd( groupname, 
				ntohl(msg->groupID.hi), 
				ntohl(msg->groupID.lo) );
		if( group == NULL ) return F2FErrListFull;
	}
	/* Send answer back */
	F2FMessageInviteAnswer myanswer;
	myanswer.destPeerID.hi = msg->sourcePeerID.hi; /* no transfer in endian necessary */
	myanswer.destPeerID.lo = msg->sourcePeerID.lo; /* no transfer in endian necessary */
	myanswer.sourcePeerID.hi = htonl(myself->id.hi);
	myanswer.sourcePeerID.lo = htonl(myself->id.lo);
	myanswer.groupID.hi = msg->groupID.hi; /* no transfer in endian necessary */
	myanswer.groupID.lo = msg->groupID.lo; /* no transfer in endian necessary */
	myanswer.tmpIDAndChallenge.hi = msg->tmpIDAndChallenge.hi; /* no transfer in endian necessary */
	myanswer.tmpIDAndChallenge.lo = msg->tmpIDAndChallenge.lo; /* no transfer in endian necessary */
	myanswer.messagetype = F2FMessageTypeInviteAnswer;
	return f2fIMSend( localPeerId, (F2FString)&myanswer,
			sizeof(myanswer) );
}

/** process an InviteMessageAnswer */
static F2FError processInviteMessageAnswer( const F2FMessageInviteAnswer *msg )
{
	F2FError error;
	
	F2FPeer *answerPeer = f2fPeerListFindPeer( ntohl(msg->tmpIDAndChallenge.hi), 
			ntohl(msg->tmpIDAndChallenge.lo) );
	if( answerPeer == NULL ) return F2FErrNotAuthenticated; /* this peer did 
	 * not get an invite */ 
	/* Check if peer waits for an invite */
	if( answerPeer->status != F2FPeerWaitingForInviteConfirm )
		return F2FErrNotAuthenticated;
	/* Check, if peer was invited in the group it specifies */
	F2FGroup *answerGroup = f2fGroupListFindGroup( ntohl(msg->groupID.hi),
			ntohl(msg->groupID.lo) );
	if( answerGroup == NULL ) return F2FErrNotAuthenticated; /* this peer did
	 * not get an invite */ 
	if( f2fPeerFindGroupIndex( answerPeer, answerGroup ) < 0 )
		return F2FErrNotAuthenticated;
	/* Change the id to the official id */
	error = f2fPeerChangeUID( answerPeer, ntohl(msg->sourcePeerID.hi), 
			ntohl(msg->sourcePeerID.lo), &answerPeer);
	if( error != F2FErrOK ) return error;
	answerPeer -> status = F2FPeerActive; /* active now */
	answerPeer -> activeprovider = F2FProviderIM; /* Contact at the moment via IM */
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
	
	/* TODO: evaluate fromPeer to the source Peer sent in the message */
	if ( receiveBuffer.filled ) // still full
		return F2FErrBufferFull;
	error = f2fIMDecode(message, size, receiveBuffer.buffer, F2FMaxMessageSize);
	if( error != F2FErrOK ) return error;
	/* parse message TODO: extract parsing in own method */
	switch ( (F2FMessageType) receiveBuffer.buffer[0] )
	{
	case F2FMessageTypeInviteAnswer:
		error = processInviteMessageAnswer( (F2FMessageInviteAnswer *) receiveBuffer.buffer);
		if( error != F2FErrOK ) return error;
		break;
	case F2FMessageTypeInvite:
		error = processInviteMessage( localPeerId, identifier,
				(F2FMessageInvite *) receiveBuffer.buffer);
		if( error != F2FErrOK ) return error;
		break;
	default:
		return F2FErrMessageTypeUnknown;
		break;
	}
	return F2FErrOK;
}

static F2FError prepareSendBuffersWithData( const F2FGroup *group,
		const char * data, F2FSize len, int binary )
{
	F2FMessageData msg;

	/* Check if send-buffers are empty */
	if(! f2fDataSent())
		return F2FErrBufferFull;
	msg.messagetype = F2FMessageTypeData;
	msg.groupID.hi = htonl( group->id.hi );
	msg.groupID.lo = htonl( group->id.lo );
	msg.sourcePeerID.hi = htonl( myself->id.hi );
	msg.sourcePeerID.lo = htonl( myself->id.lo );
	msg.destPeerID.hi = 0; /** goes to multiple destinations */
	msg.destPeerID.lo = 0;
	msg.binary = binary;
	msg.size = len;
	/* prepare normal sendBuffer */
	if ( len > sizeof(sendBuffer.buffer)-sizeof(F2FMessageData) ) 
		return F2FErrMessageTooLong;
	memcpy( sendBuffer.buffer, &msg, sizeof(F2FMessageData) );
	memcpy( sendBuffer.buffer + sizeof(F2FMessageData), data, len);
	/* and the IM endbuffer */
	F2FWord32 outputlen = b64encode( data, sendIMBuffer.buffer,
			len + sizeof(F2FMessageData), sizeof(sendIMBuffer.buffer) );
	if(outputlen == 0) return F2FErrMessageTooLong;
	/* Indicate, that both buffers are filled now */
	sendIMBuffer.size = outputlen;
	sendBuffer.size = len + sizeof(F2FMessageData);
	return F2FErrOK;	
}

/** Fill send buffer with data for all group members */
static F2FError f2fGroupSendRaw( const F2FGroup *group, 
		const char * message, F2FSize len, int binary )
{
	F2FError error = prepareSendBuffersWithData( group, message, len, binary );
	if (error != F2FErrOK ) return error;
	/* fill the lists of peers to send data to */
	/* go through all peers in this group and add these to the
	 * respective sendlists */
	sendBuffer.localidscount = 0;
	sendIMBuffer.localidscount = 0;
	int peerindex;
	for( peerindex = 0; peerindex < group->listSize; peerindex ++)
	{
		if (group->sortedPeerList[peerindex].peer->activeprovider 
				== F2FProviderIM )
			sendBuffer.peerList[sendBuffer.localidscount++] =
				group->sortedPeerList[peerindex].peer;
		else
			sendIMBuffer.localPeerIDlist[sendIMBuffer.localidscount++] =
				group->sortedPeerList[peerindex].peer->localPeerId;
	}
	return F2FErrOK;
}

/** Fill send buffer with data for all group members */
F2FError f2fGroupSendData( const F2FGroup *group, 
		const char * message, F2FSize len )
{
	return f2fGroupSendRaw( group, message, len, 1 /* binary */ );
}

/** Fill send buffer with a text message for all group members */
F2FError f2fGroupSendText( const F2FGroup *group, const F2FString message )
{
	F2FSize len = strnlen( message, F2FMaxMessageSize );
	return f2fGroupSendRaw( group, message, len, 0 /* not binary */ );
}

/** Fill send buffer for a specific peer in a group */
F2FError f2fPeerSendData( const F2FGroup *group, F2FPeer *peer,
		const char *data, const F2FWord32 dataLen )
{
	F2FError error = prepareSendBuffersWithData( group, data, dataLen, 1 /* binary */ );
	if (error != F2FErrOK ) return error;
	/* prepare the right destinations for the buffer
	 * in terms of provider */
	if (peer->activeprovider == F2FProviderIM )
	{
		sendBuffer.peerList[0] = peer;
		sendBuffer.localidscount = 1;
	}
	else
	{
		sendIMBuffer.localPeerIDlist[0] = peer->localPeerId;
		sendIMBuffer.localidscount = 1;
	}
	return F2FErrOK;
}

/** test, if data in buffer has been sent (Buffers are empty) */
int f2fDataSent( )
{
	return sendBuffer.localidscount == 0 && sendIMBuffer.localidscount == 0;
}

/** empty, send buffers for data, even if it has not been sent */
F2FError f2fEmptyData()
{
	/* TODO: maybe return F2FErrBufferFull if buffer not empty before? */
	sendBuffer.localidscount = 0;
	sendIMBuffer.localidscount = 0;
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
F2FError f2fGroupSubmitJob( const char * jobpath )
{
	// TODO: implement	
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
