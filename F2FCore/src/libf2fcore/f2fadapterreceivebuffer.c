/***************************************************************************
 *   Filename: f2fadapterreceivebuffer.c
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
 *   A messagebuffer for messages for the f2f adapter.
 ***************************************************************************/

#include <stdlib.h>
#include <string.h>

#include "f2fconfig.h"
#include "f2fadapterreceivebuffer.h"

typedef struct BufferSlotStruct
{
	int filled;
	F2FAdapterReceiveMessage msg;
} BufferSlot;

static BufferSlot buffer[F2FAdapterReceiveBufferSize];
static buffersize = 0;

/** get a free slot in the receive buffer */
F2FAdapterReceiveMessage * f2fAdapterReceiveBufferReserve( void )
{
	if (buffersize < F2FAdapterReceiveBufferSize)
	{
		/* find free spot */
		int i;
		for(i=0; i<F2FAdapterReceiveBufferSize; i++)
		{
			if(! buffer[i].filled)
			{
				buffer[i].filled = 1;
				buffersize ++;
				return & (buffer[i].msg);
			}
		}
	}
	return NULL;
}

/** release a specific buffer slot */
F2FError f2fAdapterReceiveBufferRelease( F2FAdapterReceiveMessage *msg )
{
	if(buffersize < 0) return F2FErrListEmpty;
	
	size_t position = ((char *)msg - (char *) buffer) / sizeof(BufferSlot);
	
	if (&(buffer[position].msg) != msg)
	{
		return F2FErrNotFound;
	}
	buffersize --;
	buffer[position].filled = 0;
	return F2FErrOK;
}

/** get an occupied slot in the receive buffer */
F2FAdapterReceiveMessage * f2fAdapterReceiveBufferGetMessage( void )
{
	if (buffersize > 0)
	{
		/* find filled spot */
		int i;
		for(i=0; i<F2FAdapterReceiveBufferSize; i++)
		{
			if(buffer[i].filled)
			{
				return & (buffer[i].msg);
			}
		}
	}
	return NULL;
}

/** return 1, if there is data in the ReceiveBuffer available */
int f2fMessageAvailable()
{ return buffersize > 0; }

