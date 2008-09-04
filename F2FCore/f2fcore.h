/***************************************************************************
 *   Filename: f2fcore.h
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
 *   This is the F2FCore interface, which will be used by the Instant
 *   Messenger specific Middlelayer (F2FAdapter)
 ***************************************************************************/

#ifndef F2FCORE_H_
#define F2FCORE_H_

#include <stdint.h> // We need to know what 32bit-word is, so we can still run on 64 and more

#include "f2fcommunicationprovider.h"

typedef int32_t F2FWord32;

typedef char *F2FString; // The string of F2F on C layer is just an array of characters terminated with 0

/** Maximum of name strings */
#define F2FMaxNameLength 255

/** Number of maximum groups */
#define F2FMaxGroups 128

/** Number of maximum peers */
#define F2FMaxPeers 1024

/** Error return codes */
typedef enum  {
	F2FErrOK = 0,
	F2FErrCreationFailed = -1,
} F2FError;

/** the unique identifier used in F2F 
 * first this will be only 64 bits, which should be ok, if there are about max. 1024 clients in a frid */
typedef struct
{
	F2FWord32 hi;
	F2FWord32 lo;
} F2FUID;

/** A peer is represented by its random 64 bit id */
typedef struct
{
	F2FUID id;
	F2FPeerCommunicationProviderInfo communicationproviderinfo; /** Space for the comm. providers 
	 														   * includes active provider */
} F2FPeer;

/** F2FGroup - a collection of peers solving one designated task (also 64bit random id) */
typedef struct
{
	F2FUID id;
	char name[F2FMaxNameLength]; /** The name of this group, does not need to be unique
								   * might be something like: "Ulno's blender computing group" */	
} F2FGroup;

/** sendMethodIM is a non blocking method, which must be implemented and will be used for doing 
 * the reliable IM communication in F2FCore.
 * this send-method will be called in f2fGroupSendText, f2fGroupPeerSendData, and f2fRegisterPeer 
 * and must be given as a parameter to init */ 
typedef F2FError (*SendMethodIM) ( F2FWord32 localPeerID, F2FString message );

/** Do the initialization - especially create a random seed and get your own PeerID 
 * Must be called first.
 * Gets the name of this peer (for example "Ulrich Norbisrath's peer") and the public key.
 * ALso the IM-sending function of the middle layer has to be specified here */
F2FError f2fInit( const F2FString myName, const F2FString myPublicKey,
		const SendMethodIM sendFunc, /*out*/ F2FPeer *peer );

/** As a next step, the user has to create a new F2FGroup, in which his intenden Job can be
 * computeted.
 * This group gets a name, which should be displayed in the invitation of clients (other peers). */
F2FError f2fcreateGroup( const F2FString groupname, /*out*/ F2FGroup *group );

/** Finally friends (other peers) can be added to this group. This function triggers
 * the registration to ask the specified peer to join a F2F Computing group 
 * If we know his public key, we can send it as a challenge. He would then also get our publickey,
 * which could be compared to a allready cached one, to create an own challenge, and later used to
 * do encrypted and authenticated communication. Of course our own peerid from f2fInit is also
 * included.
 * - localPeerId will be the id used to send an IM message to this friend, it has to be managed
 * in the middle layer
 * - identifier can be the name in the addressbook or one of the addresses including the protocol,
 * example: "test@jabber.xyz (XMPP)" 
 * This function will call the SendMethodIP-function*/
F2FError f2fGroupRegisterPeer( const F2FGroup *group, const F2FWord32 localPeerId,
		const F2FString identifier,	const F2FString otherPeersPublicKey );

/** Will create a list of pointers to all the peers in a group, the list must have 
 * the size of maxsize and be a list of pointers, the last listentry might be null,
 * if smaller than maxsize, th eactual size created in the list is also returned.
 * If there was an error -1 is returned. */
F2FSize f2fGroupCreatePeerList( const F2FGroup *group, const F2FWord32 maxsize, 
		/* out */ F2FPeer **peerlist );

/** TODO: maybe we need also a list of all peers to generate the topology graph */

/** unregister the peer again, must before appear in group */
F2FError f2fGroupUnregisterPeer( const F2FGroup *group, const F2FPeer *peer );

/** hand over messages from the IM program to the core */
F2FError f2fNotifyCoreWithReceived( const F2FPeer *fromPeer, const F2FString message );

F2FError f2fGroupAddPeer( const F2FGroup *group, const F2FPeer *peer ); // Add the peer to the group

F2FError f2fGroupRemovePeer( const F2FGroup *group, const F2FPeer *peer ); // Remove peer from group

typedef long F2FSize; // a length of something (is signed as it could be negative in an error case)

/** Give a list of all peer-ids in a group */
F2FSize f2fGroupPeerList( F2FGroup *group, const F2FPeer ** peerlist);

inline int f2fSizeValid( F2FSize size ) // check if a returned size it is valid
{ return size >= 0; }

/* Send a text message to all group members */
F2FError f2fGroupSendText( F2FGroup *group, F2FString message ); 

/** Send data to a peer in this group */
F2FError f2fGroupPeerSendData( F2FGroup *group, F2FPeer *peer, 
		F2FString data, F2FWord32 dataLen,
		SendMethodIM sendFuncPtr );

/** gets peeridList and corresponding messageList,
 *  returns how many of these pairs are available
 *  if nothing was received the return value is -1 
 *  receive may have to be called often, if kernel does not run as thread */
F2FSize f2fGroupReceive( F2FPeer *peerList, F2FString *messageList );
/* check with F2FSizeValid */

#endif /*F2FCORE_H_*/
