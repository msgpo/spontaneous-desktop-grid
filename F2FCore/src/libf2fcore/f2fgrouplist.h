/***************************************************************************
 *   Filename: f2fgrouplist.h
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
 *   Description: All around the grouplist.
 *   
 ***************************************************************************/

#ifndef F2FGROUPLIST_H_
#define F2FGROUPLIST_H_

#include "f2ftypes.h"
#include "f2fgroup.h"

#ifdef __cplusplus
extern "C"
    {
#endif

/** Try to find the exact group. If it does not exist, return NULL. Else return the peer */
F2FGroup * f2fGroupListFindGroup( const F2FWord32 uidhi, const F2FWord32 uidlo );

/** Create a new group - give it also a uid */
F2FGroup * f2fGroupListCreate( const F2FString name );

/** create a group in the list, return group in success or NULL if no space left */
F2FGroup * f2fGroupListAdd( const F2FString name, const F2FWord32 hiuid, const F2FWord32 louid );

/** remove a peer from the list */
F2FError f2fGroupListRemove( F2FGroup *group );

#ifdef __cplusplus
    }
#endif

#endif /*F2FGROUPLIST_H_*/
