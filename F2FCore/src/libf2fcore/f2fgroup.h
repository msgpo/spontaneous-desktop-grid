/***************************************************************************
 *   Filename: f2fgroup.h
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
 *   everything around the f2fgroup
 ***************************************************************************/

#ifndef F2FGROUP_H_
#define F2FGROUP_H_

#include "f2ftypes.h"
#include "f2fpeer.h"
#include "f2fticket.h"

#ifdef __cplusplus
extern "C"
    {
#endif

enum F2FTicketStateEnum
{
	F2FTicketStateEmpty = 0,
	F2FTicketStateRequested = 1,
	F2FTicketStateReceived = 2
} F2FTicketState;

typedef struct F2FGroupPeerStruct
{
	F2FPeer *peer; /** a pointer to the actual peer */
	F2FTicket sendTicket; /** This is the ticket, which allows the 
						* current F2F instance to execute jobs on this peer */
	F2FTicket receiveTicket; /** This is the ticket, which allows another peer 
						*  to execute jobs in the current F2F instance */
	enum F2FTicketStateEnum ticketState; /** Request state of the ticket */
} F2FGroupPeer;

/** F2FGroup - a collection of peers solving one designated task (also 64bit random id) */
typedef struct F2FGroupStruct
{
	F2FUID id;
	char name[F2FMaxNameLength+1]; /** The name of this group, does not need to be unique
								   * might be something like: "Ulno's blender computing group" */	
	F2FGroupPeer sortedPeerList [ F2FMaxPeers ]; /** peers belonging to this group */
	F2FSize listSize; /** current size of the sorted IDs list */
	char jobfilepath[F2FMaxPathLength+1]; /** points to the name
	 	* of the submitted job
		* empty, if no job was submitted
	 	* TODO: consider multiple jobs in one group */
	F2FSize jobarchivesize; /** Length in byte of the jobarchive on disk */
} F2FGroup;

/** Try to find the exact peer. If it does not exist, return NULL. Else return the peer */
F2FPeer * f2fGroupFindPeer( F2FGroup *group, const F2FWord32 uidhi, const F2FWord32 uidlo );

/** Try to find the exact group-peer. If it does not exist, return NULL. Else return the group-peer */
F2FGroupPeer * f2fGroupFindGroupPeer( F2FGroup *group, const F2FWord32 uidhi, const F2FWord32 uidlo );

/** add one peer to the list, return peer or NULL if no space left */
F2FError f2fGroupPeerListAdd( /* out */ F2FGroup *group, F2FPeer *peer );

/** remove a peer from the list */
F2FError f2fGroupPeerListRemove( F2FGroup *group, F2FPeer *peer );

/** Add peer to group (update lists in peer and in group) */
F2FError f2fGroupAddPeer( F2FGroup *group, F2FPeer *peer );

/** Remove peer from group (update lists in peer and in group) */
F2FError f2fGroupRemovePeer( F2FGroup *group, F2FPeer *peer );


#ifdef __cplusplus
    }
#endif

#endif /*F2FGROUP_H_*/
