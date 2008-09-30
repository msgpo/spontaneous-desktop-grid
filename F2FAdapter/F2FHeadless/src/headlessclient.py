# Author: Ulrich Norbisrath (ulno)
# Headless jabber client
# This client allows to run a headless (without gui) client for the
# f2f network
# in a first instance, it can only take jobs
# it will be used to develop a client which can be submitted to real grids
# in a second instance it also should be able to submit jobs with it
# TODO: Replace jabber.py with http://pyxmpp.jajcus.net/

# Imports
import sys
import os
# Add path for f2fcore module
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], "..", "..","F2FCore" )))
import f2fcore

# Add path for jabber module
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], ".." )))
from jabberpy import jabber

from string import split

def usage():
    print "%s: f2f headless client. " % sys.argv[0]
    print "usage:"
    print "%s <server> <username> <password> <resource>"   % sys.argv[0]
    print "<friend1>,<friend2>,... [<groupname> <job-archive>]"
    print "            - connect to server and login   "
    print "              allow the specified friends to use this resource"
    print "              if group and job are specified,"
    print "              add all friends to this group and submit job."
    sys.exit(0)

# check usage
if len(sys.argv) < 5: usage()
servername = sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
resource = sys.argv[4]
global friendlist
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
    friendlist = ""
    jobarchive = ""

#con = jabber.Client(host=servername,debug=jabber.DBG_ALWAYS ,log=sys.stderr)
con = jabber.Client(host=servername,log=None)

try:
    con.connect()
except IOError, e:
    print "Couldn't connect: %s" % e
    sys.exit(0)
else:
    print "Connected"

def messageCB(con, msg):
    if msg.getBody(): ## Dont show blank messages ##
        print msg.getFrom()
        print msg.getBody()

def presenceCB(con, prs):
    """Called when a presence is recieved"""
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
            
con.registerHandler('message',messageCB)
#con.registerHandler('presence',presenceCB)
#con.registerHandler('iq',iqCB)
#con.setDisconnectHandler(disconnectedCB)

if con.auth(username,password,resource):
    print "Logged in as %s to server %s" % ( username, servername )
else:
    print "eek -> ", con.lastErr, con.lastErrCode
    sys.exit(1)

#con.requestRoster()
con.sendInitPresence()

def sendMessage(localpeerid,messagetxt):
    destcontact = friendlist[localpeerid]
    msg = jabber.Message(destcontact, messagetxt)
    msg.setType('chat')
    con.send(msg)
    
def sendOutSendIMBuffer():
    while (True):
        nextpeer=f2fcore.f2fSendIMBufferGetNextLocalPeerID()
        if( nextpeer < 0 ): break
        sendMessage( nextpeer, f2fcore.f2fSendIMBufferGetBuffer() )

# Initialize f2f
mypeerid = f2fcore.f2fInit( username +'@' + servername + '/' + resource, "")
if( groupname ): # a job shall be submitted
    mygroupid = f2fcore.f2fCreateGroup( groupname )
    # Invite all the friends to this group
    for index in range(len(friendlist)):
        f2fcore.f2fGroupRegisterPeer( mygroupid, index, friendlist[index], "headless invite", "" )
        # check sendbuffer (to send away the created invitemessage
        sendOutSendIMBuffer()
else:
    mygroupid = None
        
print f2fcore.f2fPeerGetUIDHi(mypeerid)
print f2fcore.f2fPeerGetUIDLo(mypeerid)
print f2fcore.f2fPeerGetLocalPeerId(mypeerid)



# big loop
from time import sleep
while(1):
    con.process(0) 
    sleep (1)
    