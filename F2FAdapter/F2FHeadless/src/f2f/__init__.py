###########################################################################
#   Filename: __init__.py
#   Author: ulno
###########################################################################
#   Copyright (C) 2008 by Ulrich Norbisrath 
#   devel@mail.ulno.net   
#                                                                         
#   This program is free software; you can redistribute it and/or modify  
#   it under the terms of the GNU Library General Public License as       
#   published by the Free Software Foundation; either version 2 of the    
#   License, or (at your option) any later version.                       
#                                                                         
#   This program is distributed in the hope that it will be useful,       
#   but WITHOUT ANY WARRANTY; without even the implied warranty of        
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
#   GNU General Public License for more details.                          
#                                                                         
#   You should have received a copy of the GNU Library General Public     
#   License along with this program; if not, write to the                 
#   Free Software Foundation, Inc.,                                       
#   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
###########################################################################
#   Description:
#   f2f computing support functions for Python
###########################################################################

import pickle
from time import sleep
import sys
import os
import random

import f2fcore

# seed the random from f2f-random generator
# TODO: implement this

localsendstack=[]

class Peer:
    __id_cptr = None
    def __init__(self,cptr=None,id=None):
        if id:
            (idhi,idlo) = id
            cptr = f2fcore.f2fPeerListFindPeer(idhi, idlo);
        if cptr:
            self.__id_cptr = cptr
    def getUid(self):
        return ( f2fcore.f2fPeerGetUIDHi(self.__id_cptr),\
                 f2fcore.f2fPeerGetUIDLo(self.__id_cptr) )
    def getLocalPeerId(self):
        return f2fcore.f2fPeerGetLocalPeerId(self.__id_cptr)
    def getGroups(self):
        return []
    def getCPtr(self): # should be only used internal
        return self.__id_cptr;
    def equals(self, otherpeer):
        return self.getUid() == otherpeer.getUid()
    # send data to this peer, block until sent
    def send(self, group, obj):
        localsendstack.insert(0, (self, group, obj))
        
def sendOutSendStack():
    error = f2fcore.F2FErrOK
    while(len(localsendstack)>0):
        (peer,group,obj) = localsendstack.pop()
        serialdata = pickle.dumps(obj, 1) # TODO: think if we could take HIGHEST_PROTOCOL
        while(1):
            error = f2fcore.f2fPeerSendRaw( group.getCPtr(), 
                                        peer.getCPtr(), 
                                        serialdata, 
                                        len(serialdata) )
            if error == f2fcore.F2FErrOK:
                break
            print "Trouble sending data."
            sleep(0.01)
    return error

class Group:
    __id_cptr = None
    __jobarchivepath = None
    def __init__(self,cptr,jobarchivepath):
        self.__id_cptr = cptr
        self.__jobarchivepath = jobarchivepath
    def getCPtr(self): # should be only used internal
        return self.__id_cptr;
    def getUid(self):
        return ( f2fcore.f2fGroupGetUIDHi(self.__id_cptr),\
                 f2fcore.f2fGroupGetUIDLo(self.__id_cptr) )
    def invitePeer(self, localpeerid, identifier, inviteMessage, otherPeersPublicKey ):
        f2fcore.f2fGroupRegisterPeer( self.__id_cptr, localpeerid, identifier, \
                                      inviteMessage, otherPeersPublicKey )
        # TODO: check error and fire exception
    def submitJob(self):
        f2fcore.f2fGroupSubmitJob( self.__id_cptr, self.__jobarchivepath )
        # TODO: check error and fire exception
    def equals(self, othergroup):
        return self.getUid() == othergroup.getUid()
    # Get all the peers in this group
    def getPeers(self):
        peerlist=[]
        # not very thread save (TODO: think to make in safer)
        for index in range(f2fcore.f2fPeerListGetSize()):
            peerptr = f2fcore.f2fPeerListGetPeer(index)
            if f2fcore.f2fPeerIsActive( peerptr ):
                peerlist.append(Peer(peerptr))
        return peerlist

receiveStack = []
# get from headlessprogram received data
def pushReceiveData( obj ):
    receiveStack.insert(0,obj)

# Receive data (block until received)
def receive():
    while len(receiveStack) == 0:
        sleep(0.01)
    return receiveStack.pop()

# release the content buffer
#def release():
#    f2fcore.f2fReceiveBufferRelease()

def myPeer():
    from adapter import myPeer as adapterMyPeer # avoid cyclic imports
    return adapterMyPeer()

# use randomness of python, it is seeded in init
# return random numbers from the initialized c-mersenne twister
#def random32Bit():
#    return f2fcore.f2fRandom()

# get a random float between 0 and 1 (created from 2 64bit integers)
# 2147483647L not 2147483648L as random32Bit never 0
#def randomDouble():
#    bignr1=((2147483648L+random32Bit())<<32)+(2147483647L+random32Bit())
#    bignr2=((2147483648L+random32Bit())<<32)+(2147483647L+random32Bit())
#    if(bignr1<bignr2): return float(bignr1)/float(bignr2)
#    else: return float(bignr2)/float(bignr1)
