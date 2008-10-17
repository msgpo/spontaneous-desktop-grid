# f2f job example
import f2f

print "Hello world f2f!"

myself = f2f.myPeer()

print "myUid:", myself.getUid()
print "myGroupUid", f2fGroup.getUid()
print "myInitiator (Peer)", f2fInitiator.getUid()

if( myself.equals(f2fInitiator) ):
    print "I am master."
else:
    print "I am slave."
