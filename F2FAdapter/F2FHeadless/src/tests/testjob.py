# f2f job example
# Monte-Carlo Pi calculation
import f2f
import threading
import random
from time import sleep, time

PointsToGather = 10000000 # How many points to gather

print "Monte Carlo Pi"
print
myself = f2f.myPeer()
print "myGroupUid:", f2fGroup.getUid()
print "myUid:", myself.getUid()
print "myInitiator (Peer):", f2fInitiator.getUid()
print

# global vars
countlock = threading.Lock()
pointcount = 1L # How many points have been count (don't start with 0 for division)
hitcount = 1L # How many hits
terminate = False

def master():
    global terminate
    resultpointcount = 0L
    resulthitcount = 0L
    starttime=time()
    def showresult():
        print "Master: %s of %s Points."%(resultpointcount, PointsToGather),\
            "Master: Current Pi is:", float(resulthitcount * 4L) / float(resultpointcount)
    while( resultpointcount < PointsToGather ):
        answer = f2f.receive()
        if answer != None:
            (group,src,data) = answer
            if group.equals(f2fGroup):
                if isinstance(data,tuple):
                    (points,hits) = data
                    print "Master: Received from %s: %s"\
                        %(src.getUid(),data)
                    resultpointcount += points
                    resulthitcount += hits
                    showresult()
                    f2f.release()
        else:
            f2f.release()
    # Send terminate to all clients
    terminate = True # also inform local slave
    for peer in f2fGroup.getPeers():
        peer.send(f2fGroup, "terminate")
    print "Master: Endresult:"
    showresult()
    print "Master: This took %s seconds"%(time()-starttime)

#### Slave specific
# wait until term-signal is sent
def waitterminate():
    global terminate
    while(not terminate):
        (grp,src,data) = f2f.receive()
        if grp.equals(f2fGroup) and src.equals(f2fInitiator) and data == "terminate":
            terminate = True
            f2f.release()
        sleep(0.01)

def findpoints():
    global pointcount, hitcount, terminate
    while(not terminate):
        (x,y) = (random.random(),random.random())
        countlock.acquire()
        if x**2 + y**2 < 1.0: hitcount += 1
        pointcount += 1
        countlock.release()

    
def slave():
    global pointcount, hitcount, terminate
    threading.Thread(target=findpoints).start()
    threading.Thread(target=waitterminate).start()
    while(not terminate):
        countlock.acquire()
        sendtuple = (pointcount, hitcount)
        (pointcount,hitcount) = (0,0)
        countlock.release()
        print "Slave: Sending", sendtuple
        f2fInitiator.send(f2fGroup,sendtuple)
        sleep(3.0) # results every second

##### Main program (select, master/slave)
if( myself.equals(f2fInitiator) ):
    print "I am master (initiator)."
    print
    # take part as slave
    threading.Thread(target=master).start()
    slave()
else:
    print "I am slave (not initiator)."
    print
    slave()
