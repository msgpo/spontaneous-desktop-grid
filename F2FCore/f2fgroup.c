/***************************************************************************
 *   Filename: f2fgroup.c
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
 *   everything around the f2fgroup, especially dealing with the peerlist
 *   in the group
 ***************************************************************************/

#include <string.h> 
#include "f2fcore.h"

/** Find the nearest upper peer or the peer itself via the uid, return index
 * If the list is empty or has one element which is higher than the current 0 will be returned.
 * If it is higher than all peers the returned index equals the group->listSize */
static int f2fGroupFindNearestUpperPeer( const F2FGroup *group, 
		const F2FWord32 uidhi, const F2FWord32 uidlo )
{
	int searchpos = group->listSize / 2;
	int interval = (group->listSize + 3 )/ 4; /* be 1, if list is at least 1 element */ 
	int newIsHigherThanSearchMinus1 = 0, newIsLowerThanSearch = 0;
	while( ! (newIsHigherThanSearchMinus1 && newIsLowerThanSearch) ) // binary search
	{ 
		if (searchpos == 0)
			newIsHigherThanSearchMinus1 = 1;
		else
		{
			F2FPeer *searchMinus1 = group->sortedIdsList[searchpos-1];
			newIsHigherThanSearchMinus1 = ( uidhi > searchMinus1->id.hi ) || 
				( uidhi == searchMinus1->id.hi && uidlo >= searchMinus1->id.lo );
		}
		if (searchpos == group->listSize)
			newIsLowerThanSearch = 1;
		else
		{
			F2FPeer *search = group->sortedIdsList[searchpos];
			newIsLowerThanSearch = ( uidhi < search->id.hi ) || 
				( uidhi == search->id.hi && uidlo < search->id.lo );
		}
		if( ! newIsHigherThanSearchMinus1 )
		{
			searchpos -= interval;
			interval = (interval+1)/2;
		}
		else if( ! newIsLowerThanSearch )
		{
			searchpos += interval;
			interval = (interval+1)/2;
		}	
	}
	return searchpos;
}

/** Try to find the exact peer. If it does not exist, return NULL. Else return the peer */
F2FPeer * f2fGroupFindPeer( const F2FGroup *group, const F2FWord32 uidhi, const F2FWord32 uidlo )
{
	if( group->listSize == 0 ) return NULL;
	int index = f2fGroupFindNearestUpperPeer( group, uidhi, uidlo );
	if( index == 0) return NULL;
	F2FPeer *candidate = group->sortedIdsList[ index - 1 ];
	if( candidate->id.hi == uidhi || candidate->id.lo == uidlo )
		return candidate;
	else
		return NULL;
}

/** add one peer to the list, return peer or NULL if no space left */
F2FError f2fGroupPeerListAdd( /* out */ F2FGroup *group, F2FPeer *peer )
{
	/* insert in sorted list */
	int searchpos = f2fGroupFindNearestUpperPeer( group, peer->id.hi, peer->id.lo );
	if( searchpos < 0 )
		return searchpos;
	/* do the actual insert */
	memmove( group->sortedIdsList + searchpos + 1, group->sortedIdsList + searchpos, 
			(group->listSize - searchpos) * sizeof(*(group->sortedIdsList)) );
	group->sortedIdsList [searchpos] = peer;
	group->listSize ++;
	return F2FErrOK;
}

/** remove a peer from the list */
F2FError f2fGroupPeerListRemove( F2FGroup *group, F2FPeer *peer )
{
	if( group->listSize == 0 ) return F2FErrListEmpty;
	int index = f2fGroupFindNearestUpperPeer( group, peer->id.hi, peer->id.lo );
	if( index == 0 ) return F2FErrNotFound;
	F2FPeer *candidate = group->sortedIdsList[ index - 1];
	if( candidate->id.hi == peer->id.hi || candidate->id.lo == peer->id.lo )
	{
		/* remove from sorted list */
		memmove( group->sortedIdsList + index - 1, group->sortedIdsList + index, 
				(group->listSize - index) * sizeof(*(group->sortedIdsList)) );
		group->listSize --;
		return F2FErrOK;
	}
	else
		return F2FErrNotFound;
}

/** Add peer to group (update lists in peer and in group) */
F2FError f2fGroupAddPeer( F2FGroup *group, F2FPeer *peer )
{
	return f2fPeerAddToGroup( peer, group );
}

/** Remove peer from group (update lists in peer and in group) */
F2FError f2fGroupRemovePeer( F2FGroup *group, F2FPeer *peer )
{
	return f2fPeerRemoveFromGroup( peer, group );
}
