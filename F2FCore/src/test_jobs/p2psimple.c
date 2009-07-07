
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "f2ftypes.h"
#include "f2fadapterreceivebuffer.h"
#include "f2fmain.h"
#include "f2fcore.h"
#include "f2fticketrequest.h"


int main() {

	printf("P2P Simple Example with JIT\n\n\n");
	//myself = f2f.myPeer()
	//printf("myGroupUid: %d", (int)getgrou§);
	//printf("myUid: %d", (int)myself.getUid());
	//printf("myInitiator (Peer): %d", (int)f2fInitiator.getUid());


	long a = 120493048;

	while(f2fPeerListGetSize() <= 1) {

		sleep(5);
		printf("waiting peers count %d ...\n",f2fPeerListGetSize());

	}
	int i;
	for(i = 0; i < (int)f2fPeerListGetSize(); i++) {

		F2FPeer *peer = f2fPeerListGetPeer(i);
		int peerId = (int)(peer->localPeerId);
		printf("peer %d : %d\n",i,peerId);

		f2fPeerSendRaw(peer->groups[0], peer, "terminate", strlen("terminate"));

	}
	return 0;
}
