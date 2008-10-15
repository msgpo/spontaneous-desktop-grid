/***************************************************************************
 *   Filename: f2fticketrequest.c
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
 *   Handles Ticketrequests.
 ***************************************************************************/

#include <time.h>
#include <string.h>
#include <arpa/inet.h>

#include "f2fticketrequest.h"
#include "f2fconfig.h"
#include "f2fcore.h"
#include "f2fgroup.h"
#include "f2fticket.h"


typedef struct TicketRequestStruct
{
	F2FPeer * requestingPeer;
	F2FGroup * group;
	time_t requesttime;
} TicketRequest;

static TicketRequest ticketRequestList[F2FMaxTicketRequests];
static int ticketRequestListSize=0;

F2FError f2fTicketRequestAdd( F2FGroup *group, F2FPeer *peer )
{
	if(ticketRequestListSize >= F2FMaxTicketRequests )
		return F2FErrTicketListFull;
	ticketRequestList[ticketRequestListSize].group = group;
	ticketRequestList[ticketRequestListSize].requestingPeer = peer;
	ticketRequestList[ticketRequestListSize].requesttime = time(NULL);
	ticketRequestListSize ++;
	return F2FErrOK;
}

F2FGroup * f2fTicketRequestGetGroup()
{
	if(ticketRequestListSize == 0) return NULL;
	return ticketRequestList[0].group;
}

F2FPeer * f2fTicketRequestGetPeer()
{
	if(ticketRequestListSize == 0) return NULL;
	return ticketRequestList[0].requestingPeer;
}

/** Grant the ticket-request and send a ticket back */
F2FError f2fTicketRequestGrant()
{
	if(ticketRequestListSize == 0) return F2FErrTicketListEmpty;
	F2FMessageGetJobTicketAnswer msg;
	F2FGroupPeer * groupPeer = f2fGroupFindGroupPeer( ticketRequestList[0].group, 
			ticketRequestList[0].requestingPeer->id.hi,
			ticketRequestList[0].requestingPeer->id.lo );
	if(groupPeer == NULL)
		return F2FErrNotFound;
	f2fTicketInitialize( & (groupPeer->receiveTicket) );
	msg.ticket.validUntil = htonl(groupPeer->receiveTicket.validUntil);
	msg.ticket.hi = htonl(groupPeer->receiveTicket.hi);
	msg.ticket.lo = htonl(groupPeer->receiveTicket.lo);
	/* send generated answer message */
	f2fPeerSendData(ticketRequestList[0].group,
			ticketRequestList[0].requestingPeer, 
			F2FMessageTypeInviteAnswer,
			(char *) & msg, sizeof( F2FMessageGetJobTicketAnswer ) );
	/* delete request */
	ticketRequestListSize --;
	memmove( ticketRequestList, ticketRequestList + 1, ticketRequestListSize * sizeof (TicketRequest) );
	return F2FErrOK;
}

