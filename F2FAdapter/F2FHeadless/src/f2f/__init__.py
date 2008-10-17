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
# Add path for f2fcore module
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], "..","F2FCore" )))
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], "..", "..","F2FCore" )))
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], "..", "..", "..","F2FCore" )))
import f2fcore

class Peer:
    __id_cptr = None
    def __init__(self,cptr):
        self.__id_cptr = cptr
    def getUid(self):
        return ( f2fcore.f2fPeerGetUIDHi(self.__id_cptr),\
                 f2fcore.f2fPeerGetUIDLo(self.__id_cptr) )
    def getLocalPeerId(self):
        return f2fcore.f2fPeerGetLocalPeerId(self.__id_cptr)
    def getGroups(self):
        return []
    def equals(self, otherpeer):
        return self.getUid() == otherpeer.getUid()
    # send data to this peer, block until sent
    def send(self, group, obj):
        serialdata = pickle.dumps(obj, pickle.HIGHEST_PROTOCOL)
        while(1):
            error = f2fcore.f2fPeerSendRaw( group.getCPtr(), 
                                        self.__id_cptr, 
                                        serialdata, 
                                        len(serialdata) )
            if error == f2fcore.F2FErrOK:
                break
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
            peerlist.append(Peer(f2fcore.f2fPeerListGetPeer(index)))
        return peerlist


# Receive data (block until received)
def receive():
    while(1):
        if f2fcore.f2fReceiveBufferIsFilled():
            if f2fcore.f2fReceiveBufferIsRaw():
                content = f2fcore.f2fReceiveBufferGetContent(4096)
                break
        sleep(0.01)
    obj = pickle.loads(content)
    answer = (Group(f2fcore.f2fReceiveBufferGetGroup(), "unknown"),
              Peer(f2fcore.f2fReceiveBufferGetSourcePeer()),
              obj)
    return answer

# release the content buffer
def release():
    f2fcore.f2fReceiveBufferRelease()

def myPeer():
    from adapter import myPeer as adapterMyPeer # avoid cyclic imports
    return adapterMyPeer()

# return random numbers from the initialized c-mersenne twister
def random32Bit():
    return f2fcore.f2fRandom()

# get a random float between 0 and 1 (created from 2 64bit integers)
# 2147483647L not 2147483648L as random32Bit never 0
def randomDouble():
    bignr1=((2147483648L+random32Bit())<<32)+(2147483647L+random32Bit())
    bignr2=((2147483648L+random32Bit())<<32)+(2147483647L+random32Bit())
    if(bignr1<bignr2): return float(bignr1)/float(bignr2)
    else: return float(bignr2)/float(bignr1)
