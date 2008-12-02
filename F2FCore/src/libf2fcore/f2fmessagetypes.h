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

#ifdef __cplusplus
extern "C"
    {
#endif

typedef enum F2FMessageTypeEnum
{
	F2FMessageTypeInvite, /** Invite peer to group */
	F2FMessageTypeInviteAnswer, /** Answer of the invited peer */
	F2FMessageTypeAnnounceNewPeerInGroup, /** Announce peer in group to other peers */
	F2FMessageTypeGetJobTicket, /** Ask for ticket to submit jobs */
	F2FMessageTypeGetJobTicketAnswer, /** The answer including the ticket */
	F2FMessageTypeJob, /** A job archive */
	F2FMessageTypeRaw, /** Just some unstructured data (binary) */
	F2FMessageTypeText, /** Just some text (which will be displayed in the group chat) */
	F2FMessageTypePeerListInfo, /** Infos about known peers */
	/* here should be a lot of types to inquire status data, pinging, 
	 * and exchanging routing information */ 
} F2FMessageType;

/** The Header of a message */
typedef struct F2FMessageHeaderStruct
{
	unsigned char messagetype; /** Type of message, see F2FMessageType */
	char reserved[3];          /** reserved for later */
	F2FUID groupID;            /** Group identifier */
	F2FUID sourcePeerID;       /** sending peer  */
	F2FUID destPeerID;         /** destination peer  */
	F2FSize len;			   /** length of the rest data, which is in this spackage */
} F2FMessageHeader;

/***** In the following there are the type specific message extensions (some messages have no extension) *****/

/** The message, which is sent out as initial challenge (to invite another 
 * peer to a group) contains the following: */
typedef struct
{
	// will be in destPeerID F2FUID tmpPeerID; /** the temporary peer ID, we have assigned to this peer */
	F2FUID tmpIDAndChallenge;    /**  challenge 
		* (this is at the same time the challenge,
	 	* so we make sure the invited peer should know this
	 	*  when he answers (we need later to encrypt
	    * this with the public key of the invited peer) */
	F2FSize groupNameLength; /**    29: Group name length (max F2FMaxNameLength) */
	F2FSize inviteLength; /**  30: Invitation message length (max F2FMaxNameLength) */
	char nameAndInvite [F2FMaxNameLength*2]; /* 31- *: Group name and then invitation message */
    /* maybe timestamps should be added */
} F2FMessageInvite;

/** This is the answer which should be sent back after you get an invite to accept the
 * invitation - we might need here a feedback to the user - TODO implement this feedback */
typedef struct
{
	F2FUID tmpIDAndChallenge;  /** challenge - also to identify local peer entry 
	                             * (the temporary peer ID, which was assigned to this peer) */
	F2FUID realSourceID; /** the real SourceID of the answering peer */
} F2FMessageInviteAnswer;

/** This is used to announce a new peer to other group memebers 
 * TODO: add a challenge respond here*/
typedef struct
{
	F2FUID newpeer; /* the id of the announced peer */
} F2FMessageAnnounceNewPeerInGroup;

/** Request for a job ticket, which can be used to run a job
 * on a peer */
typedef struct
{
	/** Request some space and other QoS parameters */
	F2FSize archivesize;
	F2FSize hdspace; /* maybe demand here 64 bit */
	/* medium bandwith, key with which the job will be signed */
} F2FMessageGetJobTicket;

/** A job ticket, which can be used to run a job
 * on a peer */
typedef struct
{
	F2FTicket ticket;          /** The requested ticket  */
} F2FMessageGetJobTicketAnswer;

/** A job ticket, which can be used to run a job
 * on a peer, Actual data of the job comes afterwards */
typedef struct
{
	F2FTicket ticket; /** the ticket, which allows to execute the job */
	F2FSize size; /** The total size of the job */
	F2FWord32 blocknr; /** The number of the block */
} F2FMessageJob;


/** raw data, this will be used to sent big packages */
typedef struct
{
	F2FUID id; /* Id of data package */
	F2FSize count; /* how many packages will be sent in total */
	F2FSize nr; /* which package is this */
} F2FMessageRaw;

///** text data */
//typedef struct
//{
//} F2FMessageText;

#define F2FMaxPeerListInfoEntries ((F2FMaxMessageSize - sizeof(F2FMessageHeader) - sizeof(F2FSize))/sizeof(F2FUID))
/** Infors about known peers */
typedef struct
{
	F2FSize count; // nr of entries
	F2FUID ids[ F2FMaxPeerListInfoEntries ];
} F2FMessagePeerListInfo;


#ifdef __cplusplus
    }
#endif

#endif /*F2FMESSAGETYPES_H_*/
