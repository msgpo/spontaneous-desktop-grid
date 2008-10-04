/***************************************************************************
 *   Filename: f2fticket.c
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
 *   Implements the methods of f2fticket
 ***************************************************************************/

#include "f2fticket.h"
#include "f2fcore.h"

/** check if ticket is null (all values null) */
int f2fTicketNull( F2FTicket *ticket )
{
	return ticket->hi == 0 && ticket->lo == 0;
}

/** set ticket to null (all values null) -> uninitialized */
void f2fTicketSetNull( F2FTicket *ticket )
{
	ticket->hi = 0;
	ticket->lo = 0;
}

/** create valus for an F2FTicket and initialize the given structure **/
F2FError f2fTicketInitialize( F2FTicket *newticket )
{
	while(1)
	{
		newticket->hi = f2fRandom();
		newticket->lo = f2fRandom();
		if (!f2fTicketNull( newticket )) break; // Even if this nearly never will happen
	}
	return F2FErrOK; 
}

/** Test if two F2FTickets are equal */
int f2fTicketEqual( const F2FTicket *ticket1, const F2FTicket *ticket2 )
{
	return ticket1->hi == ticket2->hi && ticket1->lo == ticket2->lo;
}
