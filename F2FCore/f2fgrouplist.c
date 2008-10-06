/***************************************************************************
 *   Filename: f2fgrouplist.c
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
 *   Description: All around the grouplist
 *   
 ***************************************************************************/

#include <string.h>
#include "f2fcore.h"
#include "f2fgrouplist.h"

/* Allocate all the necessary memory */
static F2FGroup groupList[ F2FMaxGroups ];
static int groupListSize = 0;

/** Try to find the exact group. If it does not exist, return NULL. Else return the peer */
F2FGroup * f2fGroupListFindGroup( const F2FWord32 uidhi, const F2FWord32 uidlo )
{
	if( groupListSize == 0 ) return NULL;
	int index = 0;
	for (index = 0; index < groupListSize; ++index)
	{
		if( groupList[index].id.hi == uidhi && groupList[index].id.lo == uidlo )
			return groupList + index;
	}
	return NULL;
}

/** create a group in the list, return group in success or NULL if no space left */
F2FGroup * f2fGroupListAdd( const F2FString name, const F2FWord32 hiuid, const F2FWord32 louid )
{
	F2FGroup *current;
	
	if( groupListSize >= F2FMaxGroups ) return NULL; /* No Space in list left */
	current = groupList + groupListSize; 
	current->name[F2FMaxNameLength]=0; // Make sure string is terminated
	strncpy( current->name, name, F2FMaxNameLength );
	current->id.hi = hiuid;
	current->id.lo = louid;
	current->jobfilepath = NULL;
	groupListSize ++;
	return current;
}

/** Create a new group - give it also a uid */
F2FGroup * f2fGroupListCreate( const F2FString name )
{
	return f2fGroupListAdd( name, f2fRandomNotNull(), f2fRandomNotNull() );
}

/** remove a peer from the list */
F2FError f2fGroupListRemove( F2FGroup *group )
{
	if( groupListSize <= 0 ) return F2FErrListEmpty;
	/* remove from list */
	memmove( group, group + 1, 
			groupListSize - ( group - groupList  ) * sizeof( F2FGroup ) );
	groupListSize --;
	return F2FErrOK;
}
