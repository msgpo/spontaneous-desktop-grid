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

/** As a next step, the user has to create a new F2FGroup, in which his intenden Job can be
 * computeted.
 * This group gets a name, which should be displayed in the invitation of clients (other peers). */
F2FError f2fCreateGroup( const F2FString groupname, /*out*/ F2FGroup *group )
{
	if (! initWasCalled) return F2FErrInitNotCalled;
	group->id.hi = mts_lrand( &randomnessState );
	group->id.lo = mts_lrand( &randomnessState );
	group->name[ F2FMaxNameLength ] = 0; // Make sure string is terminated
	strncpy( group->name, groupname, F2FMaxNameLength );
	return F2FErrOK; 
}



