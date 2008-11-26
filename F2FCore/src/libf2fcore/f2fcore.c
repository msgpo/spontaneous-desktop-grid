/***************************************************************************
 *   Filename: f2fmain.h
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
 *   This implements the interface for F2FCore (refer to f2fcore.h)
 *   Here are mainly only exteranlly used access functions for reading values.
 *   The core functionality is in f2fmain
 ***************************************************************************/

#include <stdio.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <arpa/inet.h>

#include "mtwist/mtwist.h"
#include "b64/b64.h"
#include "f2fcore.h"
#include "f2fmain.h"
#include "f2fpeerlist.h"
#include "f2fgroup.h"
#include "f2fgrouplist.h"
#include "f2fmessagetypes.h"
#include "f2fticketrequest.h"
#include "f2fadapterreceivebuffer.h"

/** return the next peer id of the buffer where data has to be sent and
 * decrease list
 * return -1 if there is nothing to send */
F2FWord32 f2fMessageGetNextLocalPeerID( F2FAdapterReceiveMessage *msg )
{
	if (msg->peercount <= 0) return -1; // none left
	return msg->localPeerIDlist[-- msg->peercount];
}

/** get the 0 terminated sendIMBuffer */
char * f2fMessageGetText( F2FAdapterReceiveMessage *msg )
{
	if(msg->buffersize > F2FMaxEncodedMessageSize) msg->buffersize = F2FMaxEncodedMessageSize;
	msg->buffer[msg->buffersize] = 0; /* make sure this is terminated */
	return msg->buffer;
}

/** Return size of a peerlist in a group */
F2FSize f2fGroupGetPeerListSize( const F2FGroup *group )
{
	return group->listSize;
}

/** Return a pointer to a peer of a group */
F2FPeer * f2fGroupGetPeerFromList( const F2FGroup *group,
		F2FWord32 peerindex )
{
	if(peerindex<0 || peerindex>group->listSize) return NULL;
	return group->sortedPeerList[peerindex].peer;
}

/** Getter for GroupUID */
F2FWord32 f2fGroupGetUIDHi(const F2FGroup * group)
{
	return group->id.hi;
}

/** Getter for GroupUID */
F2FWord32 f2fGroupGetUIDLo(const F2FGroup * group)
{
	return group->id.lo;
}

/** return 1, if the data in the ReceiveBuffer is to be forwarded */
int f2fMessageIsForward( F2FAdapterReceiveMessage *msg )
{ return msg->buffertype == F2FAdapterReceiveMessageTypeIMForward; }

/** return 1, if the data in the ReceiveBuffer is to be received by the adapter */
int f2fMessageIsReceive( F2FAdapterReceiveMessage *msg )
{ return msg->buffertype == F2FAdapterReceiveMessageTypeData; }

/** return 1, if the data in the ReceiveBuffer is raw data */
int f2fMessageIsRaw( F2FAdapterReceiveMessage *msg )
{ return msg->messagetype == F2FMessageTypeRaw; }

/** return 1, if the data in the ReceiveBuffer is text data */
int f2fMessageIsText( F2FAdapterReceiveMessage *msg )
{ return msg->messagetype == F2FMessageTypeText; }

/** return 1, if the data in the ReceiveBuffer is a job */
int f2fMessageIsJob( F2FAdapterReceiveMessage *msg )
{ return msg->messagetype == F2FMessageTypeJob; }

/** get the group of the received data */
F2FGroup * f2fMessageGetGroup( F2FAdapterReceiveMessage *msg )
{ return msg->group; }

/** get received Source peer */
F2FPeer * f2fMessageGetSourcePeer( F2FAdapterReceiveMessage *msg )
{ return msg->sourcePeer; }

/** get received destination peer */
F2FPeer * f2fMessageGetDestPeer( F2FAdapterReceiveMessage *msg )
{ return msg->destPeer; }

/** get the size of the message in the buffer */
F2FSize f2fMessageGetSize( F2FAdapterReceiveMessage *msg )
{ return msg->buffersize; }

/** get a pointer to the content of the buffer */
char * f2fMessageGetContentPtr( F2FAdapterReceiveMessage *msg )
{ return msg->buffer; }

/** special function for SWIG to return a binary buffer
 * maxlen is a pointer to a variable, which specifies the maximum len, which can be taken and
 * will have the actual length of copied data at the end
 * The data must be copied into content */
void f2fMessageGetContent(F2FAdapterReceiveMessage *msg, char *content, int *maxlen)
{
	if ( *maxlen > msg->buffersize ) *maxlen = msg->buffersize;
	memcpy( content, msg->buffer, *maxlen );
}

/* analog to the last function (f2fMessageGetContent) receives a job */
void f2fMessageGetJob(F2FAdapterReceiveMessage *msg, char *content, int *maxlen )
{
	if( !f2fMessageIsJob( msg ) )
	{
		*maxlen = 0;
		return;
	}
	F2FMessageJob *jobmsg = (F2FMessageJob *) msg->buffer;
	F2FSize jobsize = ntohl(jobmsg->size);
	if (*maxlen > jobsize) *maxlen = jobsize;
	memcpy(content, msg->buffer + sizeof(F2FMessageJob),*maxlen);
	/* TODO: adapt to longer jobs */
}

/** show that the buffer has been read and can be filled again */
F2FError f2fMessageRelease( F2FAdapterReceiveMessage *msg )
{ return f2fAdapterReceiveBufferRelease( msg ); }

