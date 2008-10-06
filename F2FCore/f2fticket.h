/***************************************************************************
 *   Filename: f2fticket.h
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
 *   This handles all methods concerning f2fticket
 ***************************************************************************/

#ifndef F2FTICKET_H_
#define F2FTICKET_H_

#include <time.h>
#include "f2ftypes.h"

/** a ticket which allows to execute jobs, if known.
 * The direct use of this structure is discouraged, use the access methods */
typedef struct F2FTicketStruct
{
	F2FWord32 hi;
	F2FWord32 lo;
	time_t validUntil;
} F2FTicket;

/** create valus for an F2FTicket and initialize the given structure **/
F2FError f2fTicketInitialize( F2FTicket *newticket );

/** Test if two F2FTickets are equal */
int f2fTicketEqual( const F2FTicket *ticket1, const F2FTicket *ticket2 );

/** set ticket to null (all values null) -> uninitialized */
void f2fTicketSetNull( F2FTicket *ticket );

/** check if ticket is null (all values null) */
int f2fTicketNull( F2FTicket *ticket );

#endif /*F2FTICKET_H_*/
