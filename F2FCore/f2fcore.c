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

#include "f2fcore.h"

F2FPeer createF2FPeer();

/** hand over messages from the IM program to the core */
F2FError notifyCoreWithReceived( F2FPeer fromPeer, const F2FString message );

F2FGroup createF2FGroup( groupname );

F2FError F2FGroupAddPeer( F2FGroup group, F2FPeer peerid ); // Add the peer to the group

F2FError F2FGroupRemovePeer( F2FGroup group, F2FPeer peerid ); // Remove peer from group

/** Give a list of all peer-ids in a group */
F2FSize F2FGroupPeerList( F2FGroup group, const F2FPeer ** peerlist);

F2FError F2FGroupSend( F2FGroup group, F2FString message, SendMethodIM sendFuncPtr );

F2FError F2FPeerSend( F2FPeer peer, F2FString message, SendMethodIM sendFuncPtr );

// sets peerid and message, returns how many of these pairs are available - if nothing was received peerid and message are empty and the return value is -1 // receive may have to be called often, if kernel does not run as thread
F2FSize F2FGroupReceive( F2FPeer *peerList, F2FString *messageList );
/* check with F2FSizeValid */

#endif /*F2FCORE_H_*/
