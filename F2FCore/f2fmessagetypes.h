/***************************************************************************
 *   Filename: f2fmessagetypes.h
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
 *   All message types of f2f computing
 ***************************************************************************/

#ifndef F2FMESSAGETYPES_H_
#define F2FMESSAGETYPES_H_

#include "f2ftypes.h"
#include "f2fticket.h"

typedef enum
{
	F2FMessageTypeInvite, /** Invite peer to group */
	F2FMessageTypeInviteAnswer, /** Answer of the invited peer */
	F2FMessageTypeGetJobTicket, /** Ask for ticket to submit jobs */
	F2FMessageTypeGetJobTicketAnswer, /** The answer including the ticket */
	F2FMessageTypeData, /** Just some unstructured data */
	/* here should be a lot of types to inquire status data, pinging, 
	 * and exchanging routing information */ 
} F2FMessageType;

/** The message, which is sent out as initial challenge (to invite another 
 * peer to a group) contains the following: */
typedef struct
{
	unsigned char messagetype; /**    1: Type of message, must be set to F2FMessageTypeInvite */
	char reserved[3];          /**  2-4: reserved for later */
	F2FUID groupID;            /**  5-12: Group identifier */
	F2FUID sourcePeerID;       /** 13-20: Peer identifier of inviting peer  */
	F2FUID tmpIDAndChallenge;    /** 21-28: challenge 
		* (this is at the same time the challenge,
	 	* so we make sure the invited peer should know this
	 	*  when he answers (we need later to encrypt
	    * this with the public key of the invited peer) */
	unsigned char groupNameLength; /**    29: Group name length (max F2FMaxNameLength) */
	unsigned char inviteLength; /**  30: Invitation message length (max F2FMaxNameLength) */
	char nameAndInvite [F2FMaxNameLength*2]; /* 31- *: Group name and then invitation message */
    /* maybe timestamps should be added */
} F2FMessageInvite;

/** This is the answer which should be sent back after you get an invite to accept the
 * invitation - we might need here a feedback to the user - TODO implement this feedback */
typedef struct
{
	unsigned char messagetype; /** Type of message, must be set to F2FMessageTypeInviteAnswer */
	char reserved[3];          /** reserved for later */
	F2FUID groupID;            /** Group identifier */
	F2FUID sourcePeerID;       /** Peer identifier of answering peer  */
	F2FUID destPeerID;         /** Peer identifier of peer who invited  */
	F2FUID tmpIDAndChallenge;  /** challenge - also to identify local peer entry */
} F2FMessageInviteAnswer;

/** Request for a job ticket, which can be used to run a job
 * on a peer */
typedef struct
{
	unsigned char messagetype; /** Type of message, must be set to F2FMessageTypeGetJobTicket */
	char reserved[3];          /** reserved for later */
	F2FUID groupID;            /** Group identifier */
	F2FUID sourcePeerID;       /** Peer identifier of peer asking for job ticket  */
	F2FUID destPeerID;         /** Peer identifier of peer being asked for ticket  */
} F2FMessageGetJobTicket;

/** A job ticket, which can be used to run a job
 * on a peer */
typedef struct
{
	unsigned char messagetype; /** Type of message, must be set to F2FMessageTypeGetJobTicketAnswer */
	char reserved[3];          /** reserved for later */
	F2FUID groupID;            /** Group identifier */
	F2FUID sourcePeerID;       /** Peer identifier of peer asked for job ticket 
	 							 * The peer creating the ticket */
	F2FUID destPeerID;         /** Peer identifier of peer asking for ticket  */
	F2FTicket ticket;          /** The requested ticket  */
} F2FMessageGetJobTicketAnswer;

/** raw data */
typedef struct
{
	unsigned char messagetype; /** Type of message, must be set to F2FMessageTypeData */
	char reserved[3];          /** reserved for later */
	F2FUID groupID;            /** Group identifier */
	F2FUID sourcePeerID;       /** sending peer  */
	F2FUID destPeerID;         /** destination peer  */
	F2FSize size;			   /** Amount of data sent */
	int binary;				   /** Set if, this is raw binary data */
	/* The data will be appended here */
} F2FMessageData;


#endif /*F2FMESSAGETYPES_H_*/
