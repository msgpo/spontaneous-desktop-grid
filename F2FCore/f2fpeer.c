/***************************************************************************
 *   Filename: f2fpeer.c
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
 *   Everything around the f2fpeer. 
 ***************************************************************************/

#include <string.h>
#include "f2fcore.h"
#include "f2fpeerlist.h"

static F2FError addToLocalGroupList( /* out */ F2FPeer *peer, /* in */ F2FGroup *group )
{
	if( peer->groupsListSize < F2FMaxGroups )
	{
		peer->groups[ peer->groupsListSize ++ ] = group;
		return F2FErrOK;
	}
	else return F2FErrListFull;
}

/** Find a specific group in the list of groups, return index or -1 if not found */
int f2fPeerFindGroupIndex( const F2FPeer *peer, const F2FGroup *group )
{
	int index;
	for (index = 0; index < peer->groupsListSize && peer->groups[index] != group; ++index) {};
	if( index< peer->groupsListSize )
	{
		return index;
	}
	else return -1;
}

static F2FError removeFromLocalGroupList( F2FPeer *peer, const F2FGroup *group )
{
	if( peer->groupsListSize >= 0 )
	{
		int index = f2fPeerFindGroupIndex( peer, group );
		if( index > 0 )
		{
			memmove ( peer->groups + index, peer->groups + index + 1, 
					sizeof (*group) * (peer->groupsListSize - index));
			return F2FErrOK;
		}
		else return F2FErrNotFound;
	}
	else return F2FErrListEmpty;
}

/** Add peer to group (update lists in peer and in group) */
F2FError f2fPeerAddToGroup( /*out*/ F2FPeer *peer, F2FGroup *group )
{
	F2FError error = F2FErrOK;
	
	if( peer->groupsListSize < F2FMaxGroups )
	{
		error = addToLocalGroupList( peer, group );
		if ( error == F2FErrOK )
		{
			error = f2fGroupPeerListAdd( group, peer );
			if ( error == F2FErrOK )
			{
				return F2FErrOK;
			}
			else
			{
				removeFromLocalGroupList( peer, group ); /* this must be undone */
				return error;
			}
		}
		else return error;
	}
	else return F2FErrListFull;
}

/** Remove peer from group (update lists in peer and in group) */
F2FError f2fPeerRemoveFromGroup( /*out*/ F2FPeer *peer, F2FGroup *group )
{
	F2FError error = F2FErrOK;
	
	if( peer->groupsListSize > 0 )
	{
		error = removeFromLocalGroupList( peer, group );
		if ( error == F2FErrOK )
		{
			error = f2fGroupPeerListRemove( group, peer );
			if ( error == F2FErrOK )
			{
				return F2FErrOK;
			}
			else
			{
				addToLocalGroupList( peer, group ); /* this must be undone */
				return error;
			}
		}
		else return error;
	}
	else return F2FErrListEmpty;
}

/** Change uid and update corresponding lists, if newpeer is not NULL, 
 * return here the new peper address */
F2FError f2fPeerChangeUID( F2FPeer *oldpeer, const F2FWord32 hi, const F2FWord32 lo, F2FPeer **newpeer )
{
	F2FPeer tmpPeer;
	F2FPeer *newpeer_tmp;
	F2FError error;
	
	/* copy current version of the peer */
	memcpy( &tmpPeer, oldpeer, sizeof(F2FPeer) );
	tmpPeer.id.hi = hi;
	tmpPeer.id.lo = lo;
	/* remove it */
	error = f2fPeerListRemove( oldpeer );
	if( error != F2FErrOK ) return error;
	/* create new with new uid */
	newpeer_tmp = f2fPeerListNew( hi, lo );
	if( newpeer_tmp == NULL ) return F2FErrWierdError; /* should not happen */
	memcpy( newpeer_tmp, &tmpPeer, sizeof(F2FPeer) ); /* update data in this peer */
	if (newpeer != NULL) *newpeer = newpeer_tmp;
	return F2FErrOK;
}
