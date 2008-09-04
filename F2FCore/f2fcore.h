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
 *   This is the F2FCore interface, which will be used by the Instant
 *   Messenger specific Middlelayer (F2FAdapter)
 ***************************************************************************/

#ifndef F2FCORE_H_
#define F2FCORE_H_

#include <stdint.h> // We need to know what 32bit-word is, so we can still run on 64 and more

#include "f2fcommunicationprovider.h"

typedef int32_t F2FWord32;

typedef char *F2FString; // The string of F2F on C layer is just an array of characters terminated with 0

/** Maximum of name strings */
#define F2FMaxNameLength 255

/** Number of maximum groups */
#define F2FMaxGroups 16

/** Number of maximum peers */
#define F2FMaxPeers 1024

/** Error return codes */
typedef enum  {
	F2FErrOK = 0,
	F2FErrCreationFailed = -1,
} F2FError;

/** A peer is represented by its random 64 bit id */
typedef struct
{
	F2FWord32 idhi;
	F2FWord32 idlo;
	F2FPeerCommunicationProviderInfo communicationproviderinfo; /** Space for the comm. providers 
	 														   * includes active provider */
} F2FPeer;

/** F2FGroup - a collection of peers solving one designated task (also 64bit random id) */
typedef struct
{
	F2FWord32 idhi;
	F2FWord32 idlo;
	char name[F2FMaxNameLength]; /** The name of this group, does not need to be unique
								   * might be something like: "Ulno's blender computing group" */	
} F2FGroup;


F2FError createF2FGroup( const F2FString groupname, /*out*/ F2FGroup *group );

F2FError createF2FPeer( /*out*/ F2FPeer *peer );


/** hand over messages from the IM program to the core */
F2FError notifyCoreWithReceived( const F2FPeer *fromPeer, const F2FString message );

F2FError f2fGroupAddPeer( const F2FGroup *group, const F2FPeer *peer ); // Add the peer to the group

F2FError f2fGroupRemovePeer( const F2FGroup *group, const F2FPeer *peer ); // Remove peer from group

typedef long F2FSize; // a length of something (is signed as it could be negative in an error case)

/** Give a list of all peer-ids in a group */
F2FSize f2fGroupPeerList( F2FGroup *group, const F2FPeer ** peerlist);

inline int f2fSizeValid( F2FSize size ) // check if it is valid
{ return size >= 0; }

/* sendMethodIM is a non blocking method, which must be implemented and will be used for doing 
 * the reliable IM communication in F2FCore
 * this send-method will be called in F2FGroupSend or F2FPeerSend and must be given as a parameter there */ 
typedef F2FError (*SendMethodIM) ( F2FPeer peer, F2FString message );

F2FError f2fGroupSend( F2FGroup group, F2FString message, SendMethodIM sendFuncPtr );
F2FError f2fPeerSend( F2FPeer peer, F2FString message, SendMethodIM sendFuncPtr );

/** gets peeridList and corresponding messageList,
 *  returns how many of these pairs are available
 *  if nothing was received the return value is -1 
 *  receive may have to be called often, if kernel does not run as thread */
F2FSize f2fGroupReceive( F2FPeer *peerList, F2FString *messageList );
/* check with F2FSizeValid */

#endif /*F2FCORE_H_*/
