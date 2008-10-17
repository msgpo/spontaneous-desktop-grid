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

class Group:
    __id_cptr = None
    __jobarchivepath = None
    def __init__(self,cptr,jobarchivepath):
        self.__id_cptr = cptr
        self.__jobarchivepath = jobarchivepath
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

def myPeer():
    from adapter import myPeer as adapterMyPeer # avoid cyclic imports
    return adapterMyPeer()
