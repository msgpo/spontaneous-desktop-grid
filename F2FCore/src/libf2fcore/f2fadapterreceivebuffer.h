/***************************************************************************
 *   Filename: f2fadapterreceivebuffer.h
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


#ifndef F2FMESSAGEADAPTERRECEIVEBUFFER_H_
#define F2FMESSAGEADAPTERRECEIVEBUFFER_H_

#include "f2fconfig.h"
#include "f2ftypes.h"
#include "f2fmessagetypes.h"
#include "f2fgroup.h"

#ifdef __cplusplus
extern "C"
{
#endif

typedef enum F2FAdapterReceiveMessageTypeEnum
{
	F2FAdapterReceiveMessageTypeIMForward, /** This message has to be forwarded to instant messaging. */
	F2FAdapterReceiveMessageTypeData, /** This is data for the current client. */
} F2FAdapterReceiveMessageType;

/** one of the messages, which can be received 
 * from the AdapterReceiveBuffer */
typedef struct F2FAdapterReceiveMessageStruct
{
	enum F2FAdapterReceiveMessageTypeEnum buffertype; /* type of this buffer */
	char buffer[F2FMaxEncodedMessageSize+1]; /* usually cleartext, so reserve space
								for terminating 0 */
	F2FSize buffersize; /* how much of the buffer is filled */
	/* This is data needed for the type F2FAdapterReceiveMessageTypeIMForward */
	F2FWord32 localPeerIDlist[ F2FMaxPeers ]; /* a number of ids, to which this has to be sent */
	F2FSize peercount; /* the number of ids in list */
	/* This is data needed for F2FAdapterReceiveMessageTypeData */
	unsigned char messagetype; /** Type of message, see F2FMessageType */
	F2FGroup *group; /** The group in which this data was sent */
	F2FPeer *sourcePeer; /** The sourcepeer of this data */
	F2FPeer *destPeer; /** The Destination Peer */
} F2FAdapterReceiveMessage;

/** get a free slot in the receive buffer */
F2FAdapterReceiveMessage * f2fAdapterReceiveBufferReserve( void );

/** release a specific buffer slot */
F2FError f2fAdapterReceiveBufferRelease( F2FAdapterReceiveMessage *msg );

/** get an occupied slot in the receive buffer */
F2FAdapterReceiveMessage * f2fAdapterReceiveBufferGetMessage( void );

#ifdef __cplusplus
}
#endif

#endif /* F2FADAPTERRECEIVEBUFFER_H_*/
