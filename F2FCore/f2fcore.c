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

static int initWasCalled = 0; // this will be set if init was successful
static F2FError globalError = F2FErrOK; // a global error variable

F2FError f2fGetErrorCode()
{
	return globalError;
}

/** Static state vector for randomness of mersenne twister, this is global in this module */
mt_state randomnessState;

static F2FPeer *myself; /* saves own peer information */

/** Send buffer */
static struct
{
	char buffer[F2FMaxMessageSize];
	F2FSize size; /* how much is filled */
} sendBuffer;

/** Send IM buffer.
 * This buffer stores IM messages, which have to be sent
 * They could be sent to multiple peers.
 * This is, why there is a list of local peer ids.
 */
static struct
{
	char buffer[F2FMaxMessageSize+1]; /* usually cleartext, so reserve space
								for terminating 0 */
	F2FSize size; /* how much is filled */
	F2FWord32 localPeerIDlist[ F2FMaxPeers ];
	F2FSize localidscount; /* the number of ids in list */
} sendIMBuffer;

/** get the 0 terminated sendIMBuffer */
char * f2fSendIMBufferGetBuffer()
{
	if (sendIMBuffer.localidscount <= 0) return NULL; // buffer empty	
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
	return sendIMBuffer.localPeerIDlist[-- sendIMBuffer.size];
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
	return newpeer;
}

/** Return a random number from the seeded mersenne twister */
F2FWord32 f2fRandom()
{
	return mts_lrand( &randomnessState );
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
	newsize = b64encode( message, sendIMBuffer.buffer + currentsize, size, F2FMaxMessageSize-currentsize );
	currentsize += newsize;	/* prepare sending of the new message */
	sendIMBuffer.localPeerIDlist[ 0 ] = localpeerid;
	sendIMBuffer.localidscount = 1;
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
	if( memcmp( sendBuffer.buffer, F2FMessageMark, F2FMessageMarkLength ) ) /* test header */
		return F2FErrNotF2FMessage; /* not the right header */
	b64decode( message + F2FMessageMarkLength, decodebuffer, messagelen, maxdecodelen );
	/* TODO: eventually evaluate result of b64encode */ 
	return F2FErrOK;
}

typedef enum
{
	F2FMessageTypeInvite,
	F2FMessageTypeInviteAnswer,
	/* her should be a lot of types to inquire status data, pinging, 
	 * and exchanging routing information */ 
} F2FMessageType;

/** The message, which is sent out as initial challenge (to invite another 
 * peer to a group) contains the following: */
typedef struct
{
	unsigned char messagetype; /**    1: Type of message, must be set to F2FMessageTypeInvite */
	char reserved[3];          /**  2-4: reserved for later */
	F2FUID groupID;            /**  5-12: Group identifier */
	F2FUID sourcePeerID;       /** 13-20: Peer identifier of inviting peer  */
	F2FUID tmpIDAndChallenge;    /** 21-28: challenge 
		* (this is at the same time the challenge,
	 	* so we make sure the invited peer should know this
	 	*  when he answers (we need later to encrypt
	    * this with the public key of the invited peer) */
	unsigned char groupNameLength; /**    29: Group name length (max F2FMaxNameLength) */
	unsigned char inviteLength; /**  30: Invitation message length (max F2FMaxNameLength) */
	char nameAndInvite [F2FMaxNameLength*2]; /* 31- *: Group name and then invitation message */
    /* maybe timestamps should be added */
} InviteMessage;

/** This is the answer which should be sent back after you get an invite to accept the
 * invitation - we might need here a feedback to the user - TODO implement this feedback */
typedef struct
{
	unsigned char messagetype; /** Type of message, must be set to F2FMessageTypeInviteAnswer */
	char reserved[3];          /** reserved for later */
	F2FUID groupID;            /** Group identifier */
	F2FUID sourcePeerID;       /** Peer identifier of answering peer  */
	F2FUID destPeerID;         /** Peer identifier of peer who invited  */
	F2FUID tmpIDAndChallenge;    /** challenge - also to identify local peer entry */
} InviteMessageAnswer;

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

	InviteMessage mes;
	
	/* first create ID for new peer and save this peer as an unconfirmed peer */
	F2FPeer *newpeer = f2fPeerListNew( f2fRandom(), f2fRandom() );
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
	return f2fIMSend( localPeerId, (F2FString)&mes, sizeof(InviteMessage) 
			- 2 * F2FMaxNameLength + mes.groupNameLength + mes.inviteLength );
}

/** unregister the peer again, must be in group */
F2FError f2fGroupUnregisterPeer( const F2FGroup *group, const F2FPeer *peer )
{
	return f2fGroupPeerListRemove(group, peer);
	// TODO: implement notfication
}

/** Return size of a peerlist in a group */
F2FSize f2fGroupGetPeerListSize( const F2FGroup *group )
{
	return group->listSize;
}

/** Return a pointer to the peers of a group */
F2FPeer * f2fGroupGetPeerList( const F2FGroup *group )
{
	return group->sortedIdsList;
}

/** tries to receive a message. If succesful, this gives a peer and the corresponding
 * message, if not peer and message will be NULL and F2FErrNothingAvail will be returned.
 * In success case F2FErrOK will be returned.
 * This routine must be called on a regulary interval - it can't be used in parallel to
 * the other methods here in this interface. 
 * If the timeout value is >0 then it will be used in an internal select. The function will
 * then block to the maximum timeout ms. 
 * This function returns F2FErrBufferFull, if there is still data to receive available.
 * The function shoul dbe called directly again (after processing the received data) */
F2FError f2fGroupReceive( /*out*/ F2FGroup **group, F2FPeer **sourcePeer,
			F2FPeer **destPeer, F2FString *message, F2FSize *size, 
			const F2FWord32 timeout )
{
	/* Check, if there is still something in the buffer */
	if( receiveBuffer.filled )
	{
		*group = receiveBuffer.group;
		*sourcePeer = receiveBuffer.sourcePeer;
		*destPeer = receiveBuffer.destPeer;
		*message = receiveBuffer.buffer;
		*size = receiveBuffer.size;
		receiveBuffer.filled = 0; /* as it has been requested, this will be free the next call */
		return F2FErrOK;
	}
	/* From here on receive.filled is 0 */
	/* Try now to receive stuff from the network */
	/* TODO: select(... */
	return F2FErrOK;
}

/** process an InviteMessage sent to me, react to this invite */
static F2FError processInviteMessage( const F2FWord32 localPeerId,
		const F2FString identifier, const InviteMessage *msg)
{
	/* check if I know this peer already */
	F2FPeer * srcPeer = f2fPeerListFindPeer( msg->sourcePeerID.lo, msg->sourcePeerID.lo );
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
		group = f2fGroupListAdd( groupname, ntohl(msg->groupID.hi), ntohl(msg->groupID.lo) );
		if( group == NULL ) return F2FErrListFull;
	}
	/* Send answer back */
	InviteMessageAnswer myanswer;
	myanswer.destPeerID.hi = msg->sourcePeerID.hi; /* no transfer in endian necessary */
	myanswer.destPeerID.lo = msg->sourcePeerID.lo; /* no transfer in endian necessary */
	myanswer.sourcePeerID.hi = htonl(myself->id.hi);
	myanswer.sourcePeerID.lo = htonl(myself->id.lo);
	myanswer.groupID.hi = msg->groupID.hi; /* no transfer in endian necessary */
	myanswer.groupID.lo = msg->groupID.lo; /* no transfer in endian necessary */
	myanswer.tmpIDAndChallenge.hi = msg->tmpIDAndChallenge.hi; /* no transfer in endian necessary */
	myanswer.tmpIDAndChallenge.lo = msg->tmpIDAndChallenge.lo; /* no transfer in endian necessary */
	myanswer.messagetype = F2FMessageTypeInviteAnswer;
	return f2fIMSend( localPeerId, (F2FString)&myanswer, sizeof(myanswer) );
}

/** process an InviteMessageAnswer */
static F2FError processInviteMessageAnswer( const InviteMessageAnswer *msg )
{
	F2FError error;
	
	F2FPeer *answerPeer = f2fPeerListFindPeer( msg->tmpIDAndChallenge.hi, 
			msg->tmpIDAndChallenge.lo );
	if( answerPeer == NULL ) return F2FErrNotAuthenticated; /* this peer did 
	 * not get an invite */ 
	/* Check if peer waits for an invite */
	if( answerPeer->status != F2FPeerWaitingForInviteConfirm )
		return F2FErrNotAuthenticated;
	/* Check, if peer was invited in the group it specifies */
	F2FGroup *answerGroup = f2fGroupListFindGroup( msg->groupID.hi,
			msg->groupID.lo );
	if( answerGroup == NULL ) return F2FErrNotAuthenticated; /* this peer did
	 * not get an invite */ 
	if( f2fPeerFindGroupIndex( answerPeer, answerGroup) < 0 )
		return F2FErrNotAuthenticated;
	/* Change the id to the official id */
	error = f2fPeerChangeUID( answerPeer, msg->sourcePeerID.hi, 
			msg->sourcePeerID.lo, &answerPeer);
	if( error != F2FErrOK ) return error;
	answerPeer -> status = F2FPeerActive; /* active now */
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
	/* parse message */
	switch ( (F2FMessageType) receiveBuffer.buffer[0] )
	{
	case F2FMessageTypeInviteAnswer:
		error = processInviteMessageAnswer( (InviteMessageAnswer *) receiveBuffer.buffer);
		if( error != F2FErrOK ) return error;
		break;
	case F2FMessageTypeInvite:
		error = processInviteMessage( localPeerId, identifier,
				(InviteMessage *) receiveBuffer.buffer);
		if( error != F2FErrOK ) return error;
		break;
	default:
		return F2FErrMessageTypeUnknown;
		break;
	}
	return F2FErrOK;
}

/* Send a text message to all group members */
F2FError f2fGroupSendText( const F2FGroup *group, const F2FString message )
{
	// TODO: implement
	return F2FErrOK;
}

/** Send data to a peer in this group */
F2FError f2fGroupPeerSendData( const F2FGroup *group, const F2FPeer *peer,
		const F2FString data, const F2FWord32 dataLen )
{
	// TODO: implement
	return F2FErrOK;
}
