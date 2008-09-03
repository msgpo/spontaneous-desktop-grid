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

typedef long F2FPeer; // Just an id, referencing the peer, corresponds to all contact addresses of a peer
typedef enum { F2FErrOK = 0,
			   F2FErrNotifyFailed = -1,
			   F2FErrGroupAddFailed = -2,
			   F2FErrGroupRemoveFailed = -3
			 } F2FError;

F2FPeer createF2FPeer();

inline int F2FPeerValid( F2FPeer peer ) // check if creation was successfull
{ return peer >= 0; }

typedef char *F2FString; // The string of F2F on C layer is just an array of characters terminated with 0

/** hand over messages from the IM program to the core */
F2FError notifyCoreWithReceived( F2FPeer fromPeer, const F2FString message );

typedef long F2FGroup; // Id of a F2FGroup - a collection of peers, this is the mean of communication usually
F2FGroup createF2FGroup( groupname );

inline int F2FGroupValid( F2FGroup group ) // check if creation was successfull
{ return group >= 0; }

F2FError F2FGroupAddPeer( F2FGroup group, F2FPeer peerid ); // Add the peer to the group

F2FError F2FGroupRemovePeer( F2FGroup group, F2FPeer peerid ); // Remove peer from group

typedef long F2FSize; // a length of something (is signed as it could be negative in an error case)

/** Give a list of all peer-ids in a group */
F2FSize F2FGroupPeerList( F2FGroup group, const F2FPeer ** peerlist);

inline int F2FSizeValid( F2FZize size ) // check if it is valid
{ return group >= 0; }

/* sendMethodIM is a non blocking method, which must be implemented and will be used for doing 
 * the reliable IM communication in F2FCore
 * this send-method will be called in F2FGroupSend or F2FPeerSend and must be given as a parameter there */ 
typedef (*SendMethodIM) ( F2FPeer peer, F2FString message );

F2FError F2FGroupSend( F2FGroup group, F2FString message, SendMethodIM sendFuncPtr );
F2FError F2FPeerSend( F2FPeer peer, F2FString message, SendMethodIM sendFuncPtr );

// sets peerid and message, returns how many of these pairs are available - if nothing was received peerid and message are empty and the return value is -1 // receive may have to be called often, if kernel does not run as thread
F2FSize F2FGroupReceive( F2FPeer *peerList, F2FString *messageList );
/* check with F2FSizeValid */

#endif /*F2FCORE_H_*/
