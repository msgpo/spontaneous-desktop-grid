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

#include "mtwist/mtwist.h"
#include "f2fcore.h"
#include "f2fpeerlist.h"
#include "f2fgrouplist.h"

static F2FSendMethodIM sendMethod;
static int initWasCalled = 0; // this will be set if init was successful

/** Static state vector for randomness of mersenne twister, this is global in this module */
mt_state randomnessState;

/** Do the initialization - especially create a random seed and get your own PeerID 
 * Must be called first.
 * Gets the name of this peer (for example "Ulrich Norbisrath's peer") and the public key */
F2FError f2fInit( const F2FString myName, const F2FString myPublicKey, 
		const F2FSendMethodIM sendFunc, /*out*/ F2FPeer **peer )
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
	    return F2FErrNotEnoughSeed;
	// iterate over seed
	int seedCell;
	for (seedCell = 0; seedCell < MT_STATE_SIZE; ++seedCell) 
	{
		// Xor a random number with a random 4 byte pair out of th ebuffer
		seeds[ seedCell ] = rand() ^ *( (F2FWord32 *) (buffer + ( rand()%(buffersize-3) ) ) );  
	}
	mts_seedfull( &randomnessState, seeds ); // activate the seed
	/* Create a new peer with random uid */
	F2FPeer *newpeer = f2fPeerListAdd( mts_lrand( &randomnessState ), mts_lrand( &randomnessState ) );
	if (newpeer == NULL) return F2FErrOK;
	*peer = newpeer;
	/* save the send method */
	sendMethod = sendFunc;
	/* add peer to peerlist */
	/* Init was successfull */
	initWasCalled = 1;
	return F2FErrOK;
}

/** Return a random number from the seeded mersenne twister */
F2FWord32 F2FRandom()
{
	return mts_lrand( &randomnessState );
}

/** As a next step, the user has to create a new F2FGroup, in which his intenden Job can be
 * computeted.
 * This group gets a name, which should be displayed in the invitation of clients (other peers). */
F2FError f2fCreateGroup( const F2FString groupname, /*out*/ F2FGroup **group )
{
	if (! initWasCalled) return F2FErrInitNotCalled;
	*group = f2fGroupListCreate( groupname );
	if( *group ) return F2FErrOK;
	else return F2FErrListFull;
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
F2FError f2fGroupRegisterPeer( const F2FGroup *group, const F2FWord32 localPeerId,
		const F2FString identifier,	const F2FString otherPeersPublicKey )
{
	/* Ask new peer, if he is f2f capable: send a challenge, save the challenge.
	 * If the peer sends back the correct answer at one point, it is added as a peer */
	/* The message, which is sent out as initial challenge contains the following:
	 * Byte:
	 *     1: Type of message (set to invite)
	 *   2-4: reserved for later
	 *  5-12: Group identifier
	 * 13-20: Peer identifier of inviting peer
	 * 21-28: Peer identifier offer (this is at the same time the challenge, so we make sure
	 *        the invited peer should know this, when he answers (we need later to encrypt
	 *        this with the public key of the invited peer)
	 *    29: Group name length (max F2FMaxNameLength)
	 *    30: Invitation message length (max F2FMaxNameLength)
	 * 31- *: Group name and then invitation message */
	
}



