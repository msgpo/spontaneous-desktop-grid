package ee.ut.f2f.comm.udp;

import java.util.HashMap;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.PeerPresenceListener;
import ee.ut.f2f.util.stun.LocalStunInfo;

public class UDPCommInitiator extends Thread implements PeerPresenceListener
{	
	public UDPCommInitiator()
	{
		F2FComputing.addPeerPresenceListener(this);
	}
	
	private static boolean initialized = false;
	static boolean isInitialized() { return initialized; }
	
	public void run()
	{		
		//ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		// start a thread that gets local STUN info
		LocalStunInfo localStun = LocalStunInfo.getInstance();
		localStun.updateSTUNInfo();
		while (localStun.isUpdating())
		{
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}
		
		//ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
		initialized = true;
		// check if some TCP tests have to be started
		// (that is if some peers are known already)
		for (F2FPeer peer: F2FComputing.getPeers())
		{
			if (F2FComputing.getLocalPeer().equals(peer)) continue;
			peerContacted(peer);
		}
	}
	
	private HashMap<F2FPeer, UDPTester> udpTesters = new HashMap<F2FPeer, UDPTester>();
	public void peerContacted(F2FPeer peer)
	{
		if (udpTesters.containsKey(peer)) return;
		synchronized (udpTesters)
		{
			if (udpTesters.containsKey(peer)) return;
			UDPTester test = new UDPTester(peer);
			udpTesters.put(peer, test);
			test.start();
		}
	}

	public void peerUnContacted(F2FPeer peer)
	{
		synchronized (udpTesters)
		{
			udpTesters.remove(peer);
		}
	}
	
}
