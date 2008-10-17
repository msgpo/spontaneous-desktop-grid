# f2f job example
# Monte-Carlo Pi calculation
import f2f
import threading
from time import sleep, time

PointsToGather = 1000000 # How many points to gather

print "Monte Carlo Pi"
print
myself = f2f.myPeer()
print "myGroupUid:", f2fGroup.getUid()
print "myUid:", myself.getUid()
print "myInitiator (Peer):", f2fInitiator.getUid()
print

# global vars
pointcount = 1L # How many points have been count (don't start with 0 for division)
hitcount = 1L # How many hits
terminate = False

def master():
    resultpointcount = 0L
    resulthitcount = 0L
    def showresult():
        print "%s of %s Points."%(resultpointcount, PointsToGather),\
            "Current Pi is:", float(resulthitcount * 4L) / float(resultpointcount)
    while( resultpointcount < PointsToGather ):
        recv = f2f.receive()
        if recv[0].equals(f2fGroup):
            resultpointcount += recv[2][0]
            resulthitcount += recv[2][1]
            showresult()
    # Send terminate to all clients
    # continue here!!!
    print "Endresult:"
    showresult()

#### Slave specific
# wait until term-signal is sent
def waitterminate():
    global terminate
    recv = f2f.receive()
    if recv[0].equals(f2fGroup) and recv[1].equals(myself) and recv[2] == "terminate":
        terminate = True

def findpoints():
    global pointcount, hitcount, terminate
    while(not terminate):
        (x,y) = (f2f.randomDouble(),f2f.randomDouble())
        if x**2 + y**2 < 1.0: hitcount += 1
        pointcount += 1
    
def slave():
    global pointcount, hitcount, terminate
    threading.Thread(target=findpoints).start()
    threading.Thread(target=waitterminate).start()
    while(not terminate):
        sendtuple = (pointcount, hitcount)
        (pointcount,hitcount) = (0,0)
        print "Sending", sendtuple
        f2fInitiator.send(f2fGroup,sendtuple)
        sleep(1.0) # results every second

##### Main program (select, master/slave)
if( myself.equals(f2fInitiator) ):
    print "I am master (initiator)."
    print
    threading.Thread(target=slave).start() # take part as slave
    master()
else:
    print "I am slave (not initiator)."
    print
    slave()
