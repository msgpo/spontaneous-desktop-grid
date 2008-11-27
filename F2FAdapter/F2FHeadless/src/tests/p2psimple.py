# f2f job example
# Monte-Carlo Pi calculation
import f2f
import threading
from time import sleep, time

print "P2P Simple Example"
print
myself = f2f.myPeer()
print "myGroupUid:", f2fGroup.getUid()
print "myUid:", myself.getUid()
print "myInitiator (Peer):", f2fInitiator.getUid()
print

# global vars
terminate = False

#### Slave specific
def receivethread():
    global terminate
    while(not terminate):
        (grp,src,data) = f2f.receive()
        if grp.equals(f2fGroup) and src.equals(f2fInitiator) and data == "terminate":
            terminate = True
        else:
            print "Received:", data

def slave():
    global terminate
    while True:
        (grp,src,data) = f2f.receive()
        if grp.equals(f2fGroup):
            break;
        print "Got data from wrong group:", grp.getUid(), data 
    myid = int(data)
    print "Ok, I have number %s."%data
    threading.Thread(target=receivethread).start() # start terminate=thread
    while(not terminate):
        for peer in f2fGroup.getPeers():
            if not peer.equals(myself):
                greetings = "Greetings from F2F Id: %s."%myid
                print "Sending --%s-- to %s."%(greetings,peer.getUid())
                peer.send(f2fGroup, greetings)
        sleep(5.0) # results every second

#### master specific
def master():
    global terminate
    index = 1
    while len(f2fGroup.getPeers())<2:
          print "Waiting for enough peers."
          sleep(5)
    for peer in f2fGroup.getPeers():
        peer.send(f2fGroup, index)
        index += 1
    print "Master: switching to be slave now."
    threading.Thread(target=slave).start()
    print "Master: sleeping for 30 seconds."
    sleep(30) # sleep and then fire terminate
    print "Master: sending terminate signal."
    for peer in f2fGroup.getPeers():
        peer.send(f2fGroup, "terminate")
    sleep(10) # make sure this is sent out

##### Main program (select, master/slave)
if( myself.equals(f2fInitiator) ):
    print "I am master (initiator)."
    print
    master()
else:
    print "I am slave (not initiator)."
    print
    slave()
