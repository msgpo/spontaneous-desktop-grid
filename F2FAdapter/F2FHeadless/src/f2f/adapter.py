###########################################################################
#   Filename: adapter.py
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
#   methods simplifying the construction of a new f2f adapter
###########################################################################

import os
import sys
import random

import f2fcore

# Add path for f2f
#sys.path.insert(1, os.path.realpath(
#            os.path.join( sys.path[0], "..", ".." ) ) )

from f2f import Peer, Group
import f2f

# Global variables
mypeer = None

def init( name, key ):
    global mypeer
    mypeerid_cptr = f2fcore.f2fInit( name, key )
    # seed randomness
    random.setstate((2,tuple([f2fcore.f2fRandom() for _ in range(624)])+(624,),None))
    mypeer = Peer(mypeerid_cptr)
    return mypeer

def createGroup( groupname, jobarchivepath ):
    groupid_cptr = f2fcore.f2fCreateGroup( groupname )
    return Group(groupid_cptr, jobarchivepath )

# return the peer representing myself
def myPeer():
    return mypeer

def showPeerList():
    print "Peerlist:"
    for index in range(f2fcore.f2fPeerListGetSize()):
        peer = f2fcore.f2fPeerListGetPeer(index)
        print "Peerid:", f2fcore.f2fPeerGetUIDHi(peer),\
            ",", f2fcore.f2fPeerGetUIDLo(peer), "localpeerid:",\
            f2fcore.f2fPeerGetLocalPeerId(peer)
    print
