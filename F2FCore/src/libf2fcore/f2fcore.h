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

#include "f2fconfig.h"
#include "f2ftypes.h"
#include "f2fpeer.h"
#include "f2fgroup.h"
#include "f2fmessagetypes.h"
#include "f2fadapterreceivebuffer.h"
#include "f2fcore.h"
#include "f2fjit_wrapper.h"


#ifdef __cplusplus
extern "C"
    {
#endif

/** return the Error code of the last problematic operation */
F2FError f2fGetErrorCode( );

/** Do the initialization - especially create a random seed and get your own PeerID
 * Must be called first.
 * Sets the name of this peer (for example "Ulrich Norbisrath's peer") and the public key.
 * In case of failure this function will return null and set the error code */
F2FPeer * f2fInit( const F2FString myName, const F2FString myPublicKey );

/** As a next step, the user has to create a new F2FGroup, in which his/her Job can be
 * computeted.
 * This group gets a name, which should be displayed in the invitation of clients (other peers).
 * The created group is returned. If an error occured null is returned and the errorcode is set */
F2FGroup * f2fCreateGroup( const F2FString groupname );

/** Finally friends (other peers) can be added to this group. This function triggers
 * the registration to ask the specified peer to join a F2F Computing group.
 * This means f2fFillIMSendBuffer
 * If we know his public key, we can send it as a challenge. He would then also get our publickey,
 * which could be compared to a allready cached one, to create an own challenge, and later used to
 * do encrypted and authenticated communication. Of course our own peerid from f2fInit is also
 * included.
 * - localPeerId will be the id used to send an IM message to this friend, it has to be managed
 * in the middle layer
 * - identifier can be the name in the addressbook or one of the addresses including the protocol,
 * example: "test@jabber.xyz (XMPP)"
 * This function will call the SendMethodIP-function*/
F2FError f2fGroupRegisterPeer( /*out*/ F2FGroup *group, const F2FWord32 localPeerId,
		const F2FString identifier, const F2FString inviteMessage,
		const F2FString otherPeersPublicKey );

/** unregister the peer again, must be in group
 * changes group and peer (because of the embedded lists) */
F2FError f2fGroupUnregisterPeer( F2FGroup *group, F2FPeer *peer );

/** Return size of a peerlist in a group */
F2FSize f2fGroupGetPeerListSize( const F2FGroup *group );

/** Return a pointer to a peer of a group */
F2FPeer * f2fGroupGetPeerFromList( const F2FGroup *group,
		F2FWord32 peerindex );

/** Getter for GroupUID */
F2FWord32 f2fGroupGetUIDHi(const F2FGroup * group);

/** Getter for GroupUID */
F2FWord32 f2fGroupGetUIDLo(const F2FGroup * group);

/** Forward messages from the IM program to the core.
 * To avoid letting the receivebuffer get too full
 * f2fReceive should be called to be able to clear
 * the receive buffers.
 * The messages have to start with the right header and must be base64 encoded to be detectable.
 * If any other message is passed here, the function will return F2FErrNotF2FMessage.
 * We send here the local peerid and the local identifier as this peer might not
 * be in our peer list.
 *
 * @param F2FString identifier - display name of contact in list
 * @param F2FWord32 localPeerId - local reference for Peer
 *
 * */
F2FError f2fForward( const F2FWord32 localPeerId,
		const F2FString identifier, const F2FString message,
		const F2FSize size );

/** check if a returned size  is valid */
static inline int f2fSizeValid( const F2FSize size )
{ return size >= 0; }

/* Send a text message to all group members */
F2FError f2fGroupSendText( F2FGroup *group, const F2FString message );

/** tries to receive a message. If succesful, this gives a pointer to a receive-message.
 * If not, the returned messagepointer will be NULL and F2FErrNothingAvail will be in F2FError.
 * This routine must be called on a regulary interval - it can't be used in parallel to
 * the other methods here in this interface. */
F2FAdapterReceiveMessage * f2fReceiveMessage();

/** This will return F2FErrBufferFull, if there is still data to receive available, F2FErrOk, if not.
 * It furthermore releases the buffer.
 * The function should then be called directly again (after processing the received data) */
F2FError f2fMessageRelease( F2FAdapterReceiveMessage *msg );

/** return 1, if there is data in the ReceiveBuffer available */
int f2fMessageAvailable();

/** Return the next peer id of the buffer where data has to be sent and
 * decrease list
 * return -1 if there is nothing to send */
F2FWord32 f2fMessageGetNextLocalPeerID( F2FAdapterReceiveMessage *msg );

/** return 1, if the data in the ReceiveBuffer is to be forwarded */
int f2fMessageIsForward( F2FAdapterReceiveMessage *msg );

/** return 1, if the data in the ReceiveBuffer is to be received by the adapter */
int f2fMessageIsReceive( F2FAdapterReceiveMessage *msg );

/** return 1, if the data in the ReceiveBuffer is binary data */
int f2fMessageIsRaw( F2FAdapterReceiveMessage *msg );

/** return 1, if the data in the ReceiveBuffer is text data */
int f2fMessageIsText( F2FAdapterReceiveMessage *msg );

/** return 1, if the data in the ReceiveBuffer is a job */
int f2fMessageIsJob( F2FAdapterReceiveMessage *msg );

/** get the group of the received data */
F2FGroup * f2fMessageGetGroup( F2FAdapterReceiveMessage *msg );

/** get received Source peer */
F2FPeer * f2fMessageGetSourcePeer( F2FAdapterReceiveMessage *msg );

/** get received destination peer */
F2FPeer * f2fMessageGetDestPeer( F2FAdapterReceiveMessage *msg );

/** get size of current buffer */
F2FSize f2fMessageGetSize( F2FAdapterReceiveMessage *msg );

/** get a pointer to the content of the buffer */
char * f2fMessageGetContentPtr( F2FAdapterReceiveMessage *msg );

#ifdef SWIG
%cstring_output_withsize(char *content, int *maxlen);
#endif
void f2fMessageGetContent(F2FAdapterReceiveMessage *msg, char *content, int *maxlen);

/** get the 0 terminated sendIMBuffer */
char * f2fMessageGetText( F2FAdapterReceiveMessage *msg );

/* analog to the last function (f2fReceiveBufferGetContent) receives a job */
void f2fMessageGetJob( F2FAdapterReceiveMessage *msg, char *content, int *maxlen );

/** Return a random number from the seeded mersenne twister */
F2FWord32 f2fRandom();
/** Return a random number from the seeded mersenne twister, but not 0 */
F2FWord32 f2fRandomNotNull();

/* some getters for f2fpeers and f2fpeerlist*/
F2FWord32 f2fPeerGetUIDLo( const F2FPeer *peer );
F2FWord32 f2fPeerGetUIDHi( const F2FPeer *peer );
F2FWord32 f2fPeerGetLocalPeerId( const F2FPeer *peer );
F2FWord32 f2fPeerIsActive( const F2FPeer *peer );
/** Return size of the general peerlist */
F2FSize f2fPeerListGetSize();
/** Return a pointer to a peer in the global peerlist */
F2FPeer * f2fPeerListGetPeer( F2FWord32 peerindex );

/** Fill send buffer with data for all group members */
F2FError f2fGroupSendData( F2FGroup *group,
		const char * message, F2FSize len );
/** Fill send buffer with a text message for all group members */
F2FError f2fGroupSendText( F2FGroup *group, const F2FString message );

/** Send data to specific peer in a group */
/*F2FError f2fPeerSendData( F2FGroup *group, F2FPeer *peer,
		F2FMessageType type,
		const char *data, const F2FWord32 dataLen );*/

/** Fill send buffer for a specific peer in a group with raw data */
F2FError f2fPeerSendRaw( F2FGroup *group, F2FPeer *peer,
		const char *data, const F2FWord32 dataLen );

/** submit a job to a f2f group
 * This will first ask for allowance tickets from
 * every client in the job group. If at a later point
 * (when the job is already started) clients (more slaves)
 * are added to the group, they will be directly asked for a ticket.
 * If tickets are received back, the job will be sent to these clients.
 * The Job must be available as a local file in a special archive format
 */
F2FError f2fGroupSubmitJob( F2FGroup *group, const char * jobpath );

/** distribute file */
F2FError f2fGroupDistributeFile( const char * publishname,
		const char * filepath );

/** distribute data in distr. hash table */
F2FError f2fGroupDistributeData( const char * publishname,
		char * memorypool, F2FSize len );

/** Try to find the exact peer. If it does not exist, return NULL. Else return the peer */
F2FPeer * f2fPeerListFindPeer( const F2FWord32 uidhi, const F2FWord32 uidlo );

void f2fJITRun(char *filepath);

#ifdef __cplusplus
    }
#endif

#endif /*F2FCORE_H_*/
