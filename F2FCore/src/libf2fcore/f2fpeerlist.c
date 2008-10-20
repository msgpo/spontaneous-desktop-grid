/***************************************************************************
 *   Filename: f2fpeerlist.c
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
 *                searching ) - implementation
 ***************************************************************************/

#include <string.h>

#include "f2fcore.h"
#include "f2fpeerlist.h"

/* Allocate all the necessary memory */
static F2FPeer peerList[ F2FMaxPeers ];
/* This list will keep track of the free and occupied pepers in peerList */
static int isReservedList[ F2FMaxPeers ];
/* This list is sorted via the IDs of the Peers */
static F2FPeer *sortedIdsList [ F2FMaxPeers ];
/* The number of elements in the list */
static F2FSize listsize = 0;

/** Find the nearest upper peer or the peer itself via the uid, return index
 * If the list is empty or has one element which is higher than the current 0 will be returned.
 * If it is higher than all peers the returned index equals the listsize */
static int findNearestUpperPeer( const F2FWord32 uidhi, const F2FWord32 uidlo )
{
	int searchpos = listsize / 2;
	int interval = (listsize + 3 )/ 4; /* be 1, if list is at least 1 element */ 
	int newIsHigherThanSearchMinus1 = 0, newIsLowerThanSearch = 0;
	while( ! (newIsHigherThanSearchMinus1 && newIsLowerThanSearch) ) // binary search
	{ 
		if (searchpos == 0)
			newIsHigherThanSearchMinus1 = 1;
		else
		{
			F2FPeer *searchMinus1 = sortedIdsList[searchpos-1];
			newIsHigherThanSearchMinus1 = ( uidhi > searchMinus1->id.hi ) || 
				( uidhi == searchMinus1->id.hi && uidlo >= searchMinus1->id.lo );
		}
		if (searchpos == listsize)
			newIsLowerThanSearch = 1;
		else
		{
			F2FPeer *search = sortedIdsList[searchpos];
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
F2FPeer * f2fPeerListFindPeer( const F2FWord32 uidhi, const F2FWord32 uidlo )
{
	if( listsize == 0 ) return NULL;
	int index = findNearestUpperPeer( uidhi, uidlo );
	if( index == 0) return NULL;
	F2FPeer *candidate = sortedIdsList[ index - 1 ];
	if( candidate->id.hi == uidhi || candidate->id.lo == uidlo )
		return candidate;
	else
		return NULL;
}

/** add one peer to the list, return peer or NULL if no space left */
F2FPeer * f2fPeerListNew( F2FWord32 uidhi, F2FWord32 uidlo )
{
	F2FPeer *current = peerList;
	
	if (listsize == 0) // if nothing is in the list, clear the list
		memset( isReservedList, 0, F2FMaxPeers*sizeof(int) );

	/* find the first free List element and set the reservedflag for it*/
	int i;
	for ( i = 0; i < F2FMaxPeers; ++i )
	{
		if ( ! isReservedList[i] ) // found a free entry
		{
			isReservedList[i] = 1; // reserve this entry
			/* insert in sorted list */
			int searchpos = findNearestUpperPeer( uidhi, uidlo );
			/* do the actual insert */
			memmove( sortedIdsList + searchpos + 1, sortedIdsList + searchpos, 
					(listsize - searchpos) * sizeof(*sortedIdsList) );
			sortedIdsList [searchpos] = current;
			current->id.hi = uidhi;
			current->id.lo = uidlo;
			current->groupsListSize = 0;
			listsize ++;
			return current;
		}
		current ++;
	}
	return NULL; // List is full
}

/** remove a peer from the list, only use, when sure peer was removed from all groups!!! */
F2FError f2fPeerListRemove( F2FPeer *peer )
{
	if( listsize == 0 ) return F2FErrListEmpty;
	int index = findNearestUpperPeer( peer->id.hi, peer->id.lo );
	if( index == 0 ) return F2FErrNotFound;
	F2FPeer *candidate = sortedIdsList[ index - 1];
	if( candidate->id.hi == peer->id.hi || candidate->id.lo == peer->id.lo )
	{
		/* remove from sorted list */
		memmove( sortedIdsList + index - 1, sortedIdsList + index, 
				(listsize - index) * sizeof(*sortedIdsList) );
		isReservedList[peerList - candidate] = 0; // free this entry
		listsize --;
		return F2FErrOK;
	}
	else
		return F2FErrNotFound;
}

/** move a peer in the list, changing its id */
F2FError f2fPeerListChange( F2FPeer *peer, F2FWord32 hi, F2FWord32 lo )
{
	if( listsize == 0 ) return F2FErrListEmpty;
	int index = findNearestUpperPeer( peer->id.hi, peer->id.lo );
	if( index == 0 ) return F2FErrNotFound;
	F2FPeer *candidate = sortedIdsList[ index - 1];
	if( candidate->id.hi == peer->id.hi || candidate->id.lo == peer->id.lo )
	{
		/* remove from sorted list */
		memmove( sortedIdsList + index - 1, sortedIdsList + index, 
				(listsize - index) * sizeof(*sortedIdsList) );
		isReservedList[peerList - candidate] = 0; // free this entry
		listsize --;
	}
	else return F2FErrNotFound;
	/* Add at the right position */
	int searchpos = findNearestUpperPeer( hi, lo );
	/* do the actual insert */
	memmove( sortedIdsList + searchpos + 1, sortedIdsList + searchpos, 
			(listsize - searchpos) * sizeof(*sortedIdsList) );
	sortedIdsList [searchpos] = peer;
	peer->id.hi = hi;
	peer->id.lo = lo;
	listsize ++;
	return F2FErrOK;
}

/** Return size of the general peerlist */
F2FSize f2fPeerListGetSize()
{
	return listsize;
}

/** Return a pointer to a peer in the global peerlist */
F2FPeer * f2fPeerListGetPeer( F2FWord32 peerindex )
{
	if(peerindex<0 || peerindex>listsize) return NULL;
	return sortedIdsList[peerindex];
}
