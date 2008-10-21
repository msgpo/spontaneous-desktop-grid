###########################################################################
#   Filename: headlessclient.py
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
# Headless jabber client
# This client allows to run a headless (without gui) client for the
# f2f network
# in a first instance, it can take and submit jobs (but only one)
# it will be used to develop a client which can be submitted to real grids
# in a second instance it also should be able to submit jobs with it
# TODO: Replace jabber.py with http://pyxmpp.jajcus.net/
############################################################################

# Imports
import sys
import os
from threading import Thread

# Add path for jabber module
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], ".." )))
from jabberpy import jabber

from string import split

# Add path for f2fcore module
#sys.path.insert(1, os.path.realpath(
#            os.path.join( sys.path[0], "..","F2FCore" )))
#sys.path.insert(1, os.path.realpath(
#            os.path.join( sys.path[0], "..", "..","F2FCore" )))
#sys.path.insert(1, os.path.realpath(
#            os.path.join( sys.path[0], "..", "..", "..","F2FCore" )))
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], "..", "..", "..","F2FCore","build" )))
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], "..", "..", "..","F2FCore","build","libf2fcore" )))

import f2f
import f2f.adapter
import f2fcore

con = None
friendlist = None
jobterminated = False # (job has not been executed)
#global con
#global friendlist

#global messageStack
messageStack=[]
MaxMessageStackSize=1024
def receiveMessageCB(con, msg):
    if msg.getBody(): # If message not empty
        print "Receiving:",msg.getBody(),"Len:",len(msg.getBody())
        if len(messageStack) > MaxMessageStackSize: # Don't let stack grow too big
            messageStack.pop() # forget one message
        messageStack.append(msg)

# Work through the messageStack and hand these messages over to the F2FCore
def evaluateReceivedIMMessages():
    #  all on toplevel sendOutSendIMBuffer() # flush the send buffer, if something is in there
    if len(messageStack) > 0:
        msg = messageStack[0] # get the top
        msgfrom = str(msg.getFrom())
        try:
            localPeerId = friendlist.index(msgfrom)
        except ValueError: # new friend
            localPeerId = len(friendlist)
            friendlist.append(msgfrom)
        # send message to f2fcore, TODO: check result
        body = str(msg.getBody())
        if f2fcore.f2fNotifyCoreWithReceived( localPeerId, msgfrom,
                                           body, len(body) ) == f2fcore.F2FErrOK :
            del messageStack[0] # only remove, if successfull, else we have to try again later
        # all on toplevel sendOutSendIMBuffer() # flush the send buffer, if here is an answer

def presenceCB(con, prs):
    """Called when a presence is received"""
    who = str(prs.getFrom())
    type = prs.getType()
    if type == None: type = 'available'

    # subscription request: 
    # - accept their subscription
    # - send request for subscription to their presence
    if type == 'subscribe':
        print "subscribe request from %s" % (who)
        con.send(jabber.Presence(to=who, type='subscribed'))
        con.send(jabber.Presence(to=who, type='subscribe'))

    # unsubscription request: 
    # - accept their unsubscription
    # - send request for unsubscription to their presence
    elif type == 'unsubscribe':
        print "unsubscribe request from %s" % (who)
        con.send(jabber.Presence(to=who, type='unsubscribed'))
        con.send(jabber.Presence(to=who, type='unsubscribe'))

    elif type == 'subscribed':
        print "we are now subscribed to %s" % (who)

    elif type == 'unsubscribed':
        print "we are now unsubscribed to %s"  % (who)

    elif type == 'available':
        print "%s is available (%s / %s)" % \
                       (who, prs.getShow(), prs.getStatus())
    elif type == 'unavailable':
        print "%s is unavailable (%s / %s)" % \
                       (who, prs.getShow(), prs.getStatus())

def iqCB(con,iq):
    """Called when an iq is recieved, we just let the library handle it at the moment"""
    pass

def disconnectedCB(con):
    print "Ouch, network error"
    sys.exit(1)

def sendMessage(localpeerid,messagetxt):
    global friendlist
    destcontact = friendlist[localpeerid]
    msg = jabber.Message(destcontact, messagetxt)
    msg.setType('chat')
    print "Sending:", msg.getBody(), "Len:", len(msg.getBody())
    con.send(msg)

# There is no risk calling this function to block any queue
def sendOutSendIMBuffer():
    while (True):
        nextpeer = f2fcore.f2fSendIMBufferGetNextLocalPeerID()
        if( nextpeer < 0 ): break
        sendMessage( nextpeer, f2fcore.f2fSendIMBufferGetBuffer() )
        
def runjob(group,peer,job):
    global jobterminated
    varlist=globals()
    # define two variables local to the job as
    # its global variables
    varlist['f2fGroup'] = group
    varlist['f2fInitiator'] = peer
    exec(job,varlist,varlist) # very insecure!!!
    jobterminated = True

def evaluateReceiveBuffer():
    if f2fcore.f2fReceiveBufferDataAvailable():
        #if f2fcore.f2fReceiveBufferIsRaw():
        #    print "Buffer is binary, content not shown."
        #    f2fcore.f2fReceiveBufferRelease()
        if f2fcore.f2fReceiveBufferIsText():
            print "Buffer is text. Content:", \
               f2fcore.f2fReceiveBufferGetContent(4096)
            f2fcore.f2fReceiveBufferRelease()
        if f2fcore.f2fReceiveBufferIsJob():
            myGroup = f2f.Group(f2fcore.f2fReceiveBufferGetGroup(),"unknown") # TODO: make sure it is not unknown
            myInitiator = f2f.Peer(f2fcore.f2fReceiveBufferGetSourcePeer())
            job = f2fcore.f2fReceiveJob(4096).strip() + "\n"  # make sure it ends with a new line and no blanks
            # TODO: make sure to execute only one
            #print "Job:",job,":Jobend"
            jobcompiled = compile( job, '<f2f job>', 'exec')
            #jobcompiled = job
            jobslavethread = Thread(target=runjob,args=(myGroup,myInitiator,jobcompiled))
            print "Starting job***************"
            #exec(jobcompiled)
            jobslavethread.start()
            f2fcore.f2fReceiveBufferRelease()

def f2fHeadless(servername, username, password, resource, friendlistlocal, groupname, jobarchive):
    #con = jabber.Client(host=servername,debug=jabber.DBG_ALWAYS ,log=sys.stderr)
    global con
    global friendlist
    con = jabber.Client(host=servername,log=None)
    friendlist = friendlistlocal
    
    try:
        con.connect()
    except IOError, e:
        print "Couldn't connect: %s" % e
        sys.exit(0)
    else:
        print "Connected"

    # Register fCallbacks for jabber            
    con.registerHandler('message',receiveMessageCB)
    #con.registerHandler('presence',presenceCB)
    #con.registerHandler('iq',iqCB)
    con.setDisconnectHandler(disconnectedCB)
    
    if con.auth(username,password,resource):
        print "Logged in as %s to server %s" % ( username, servername )
    else:
        print "eek -> ", con.lastErr, con.lastErrCode
        sys.exit(1)
    
    #con.requestRoster()
    con.sendInitPresence()
    
    # Initialize f2f
    mypeer = f2f.adapter.init(username + '@' + servername + '/' + resource, "")
    
    if( groupname ): # a job shall be submitted
        mygroup = f2f.adapter.createGroup( groupname, jobarchive )
        # Invite all the friends to this group
        for index in range(len(friendlist)):
            mygroup.invitePeer( index, friendlist[index], "headless invite", "" )
            # check sendIMBuffer (to send away the created invitemessage)
            sendOutSendIMBuffer()
        # Start the job
        mygroup.submitJob()
        ## Start the master
        #jobfile = open(jobarchive)
        #exec(jobfile) # very insecure
        #jobfile.close()
        #jobslavethread = Thread(target=master)
        #jobslavethread.start()
    else:
        mygroup = None
        
    # show path
    #print "Path:",  os.path.realpath(".")
    
    # big loop
    from time import sleep, time
    oldtime = time()
    while(not jobterminated ):
        con.process(0.01) 
        #sleep (1)
        #while( f2fcore.f2fReceiveBufferDataAvailable() ):
        #while( f2fcore.f2fSend() == f2fcore.F2FErrBufferStillFull ):
        sendOutSendIMBuffer() # flush the IM send buffer
        f2fcore.f2fSend() # Send on the internal network
        # not needed at the moment f2fcore.f2fReceive() # Receive on the internal network (fill evtl. internal receive buffer)
        f2fcore.f2fReceiveBufferParse() # internally evaluate  the buffer, could fill send buffers
        sendOutSendIMBuffer() # flush the IM send buffer
        f2fcore.f2fSend() # Send on the internal network
        evaluateReceiveBuffer() # Evtl. there is something still in the buffer for the adapter
        evaluateReceivedIMMessages()
        sendOutSendIMBuffer() # flush the IM send buffer
        f2fcore.f2fSend() # Send on the internal network
        f2fcore.f2fTicketRequestGrant() # For a start grant all, TODO: secure this!
        newtime = time()
        if newtime-oldtime > 10:
            oldtime = newtime
            f2f.adapter.showPeerList()
            #if( groupname ):
            #    f2fcore.f2fGroupSendText( mygroupid, "Hello World!" )
            
def main():
    def usage():
        print "%s: f2f headless client. " % sys.argv[0]
        print "usage:"
        print "%s <server> <username> <password> <resource>"   % sys.argv[0]
        print "<friend1>,<friend2>,... [<groupname> <job-archive>]"
        print "            - Connect to server and login."
        print "              Allow the specified friends to use this resource."
        print "              If group and job are specified,"
        print "              add all friends to this group and submit job."
        sys.exit(0)
    
    # check usage and read parameters
    if len(sys.argv) < 5: usage()
    servername = sys.argv[1]
    username = sys.argv[2]
    password = sys.argv[3]
    resource = sys.argv[4]

    if len(sys.argv) >= 6:
        friendlist = split(sys.argv[5],',')
    else:
        friendlist = []
    if len(sys.argv) >= 7:
        if len(sys.argv) == 8:
            groupname = sys.argv[6]
            jobarchive = sys.argv[7]
        else:
            usage()
    else:
        friendlist = []
        groupname = ""
        jobarchive = ""
    f2fheadless(servername, username, password, resource, friendlist, groupname, jobarchive)
    
    
# allow this file to be called as module
if __name__ == "__main__":
    main()

# TODO: Move functions to f2f package
