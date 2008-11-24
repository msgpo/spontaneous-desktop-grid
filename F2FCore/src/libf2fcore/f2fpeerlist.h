/***************************************************************************
 *   Filename: f2fpeerlist.h
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
 *   Description: all functions around the f2fpeerlist (adding deleting,
 *                searching )
 ***************************************************************************/

#ifndef F2FPEERLIST_H_
#define F2FPEERLIST_H_

#include "f2ftypes.h"


#ifdef __cplusplus
extern "C"
    {
#endif
    
/** add one peer to the list, return peer or NULL if no space left */
F2FPeer * f2fPeerListNew( F2FWord32 uidhi, F2FWord32 uidlo );

/** remove a peer from the list, only use, when sure peer was removed from all groups!!! */
F2FError f2fPeerListRemove( F2FPeer *peer );

/** Try to find the exact peer. If it does not exist, return NULL. Else return the peer */
F2FPeer * f2fPeerListFindPeer( const F2FWord32 uidhi, const F2FWord32 uidlo );

/** Find the nearest upper peer or the peer itself via the uid, return index
 * If the list is empty or has one element which is higher than the current 0 will be returned.
 * If it is higher than all peers the returned index equals the listsize */
// static not accessible int findNearestUpperPeer( const F2FWord32 uidhi, const F2FWord32 uidlo );

/** move a peer in the list, changing its id */
F2FError f2fPeerListChange( F2FPeer *peer, F2FWord32 hi, F2FWord32 lo );

#ifdef __cplusplus
    }
#endif

#endif /* F2FPEERLIST_H_ */
