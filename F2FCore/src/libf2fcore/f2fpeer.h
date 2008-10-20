/***************************************************************************
 *   Filename: f2fpeer.h
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
 *   Everything around the f2fpeer data strtructure.
 ***************************************************************************/
#ifndef F2FPEER_H_
#define F2FPEER_H_

#include "f2ftypes.h"

enum F2FProviderTypeEnum
{
	F2FProviderMyself, /** This is the own peerid, so direct communication is
	 				* possible - will be just a memory transfer */
	F2FProviderIM, /** Instant messaging provider, important info in localPeerId */
	F2FProviderTCPIPV4, /** Connection via TCP over sockets */
	F2FProviderUDPHolePunch, /** Connection via UDP Hole punch */
	/* UPNP */
	/* IPV6 */
	/* Bluetooth */
	/* Skype data transfer */
} F2FProviderType;

typedef enum
{
	F2FPeerActive,
	F2FPeerWaitingForInviteConfirm,
} F2FPeerState;

struct F2FGroupStruct;

/** A peer is represented by its random 64 bit id */
typedef struct F2FPeerStruct
{
	F2FUID id;
	F2FPeerState status;
	F2FWord32 localPeerId; /** the id under which this peer is referred in the 
	 * adapter layer. This has to be given to the send function to send an IM to
	 * this peer */
	char identifier[F2FMaxNameLength + 1]; /** Displayname of the peer */
	time_t lastActivity; /** When was the last network activity with this  peer.
	 					   * This is needed to remove peer from peerlist after some time */
	enum F2FProviderTypeEnum activeprovider; /** the active provider.
				* The one over which data is sent to and recieved from this peer */ 
	struct F2FGroupStruct *groups[F2FMaxGroups]; /* Member in this groups */
	F2FSize groupsListSize; /* Member in how many groups */
	/** Local Ip number */
	/** Ip number of router */
	/** which providers are trying to connect, in which state are they ... */
} F2FPeer;

/** Add peer to group (update lists in peer and in group) */
F2FError f2fPeerAddToGroup( /*out*/ F2FPeer *peer, struct F2FGroupStruct *group );

/** Remove peer from group (update lists in peer and in group) */
F2FError f2fPeerRemoveFromGroup( /*out*/ F2FPeer *peer, struct F2FGroupStruct *group );

/** Find a specific group in the list of groups, return index or -1 if not found */
int f2fPeerFindGroupIndex( const F2FPeer *peer, const struct F2FGroupStruct *group );

/** Change uid and update corresponding lists */
F2FError f2fPeerChangeUID( F2FPeer *peer, 
		const F2FWord32 hi, const F2FWord32 lo );

#endif /*F2FPEER_H_*/
