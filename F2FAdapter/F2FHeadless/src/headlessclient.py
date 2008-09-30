# Headless jabber client
# TODO: Replace jabber.py with http://pyxmpp.jajcus.net/

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

def usage():
    print "%s: f2f headless client. " % sys.argv[0]
    print "usage:"
    print "%s <server> <username> <password> <resource>"   % sys.argv[0]
    print "<friend1> <friend2> ..."
    print "            - connect to server and login   "
    print "              allow the specified friends to use this resource"
    sys.exit(0)

# check usage
if len(sys.argv) < 5: usage()
servername = sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
resource = sys.argv[4]

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

# Initialize f2f
mypeerid = f2fcore.f2fInit( username +'@' + servername + '/' + resource, "")

print f2fcore.f2fPeerGetUIDHi(mypeerid)
print f2fcore.f2fPeerGetUIDLo(mypeerid)
print f2fcore.f2fPeerGetLocalPeerId(mypeerid)

# big loop
from time import sleep
while(1):
    con.process(0) 
    sleep (1)
    