package ee.ut.f2f.comm.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.PeerPresenceListener;
import ee.ut.f2f.util.F2FProperties;
import ee.ut.f2f.util.LocalAddresses;
import ee.ut.f2f.util.logging.Logger;

class TCPCommInitiator extends Thread implements Activity, PeerPresenceListener
{
	private final static Logger log = Logger.getLogger(TCPCommInitiator.class);
	
	TCPCommInitiator()
	{
		F2FComputing.addPeerPresenceListener(this);
	}
	
	private static boolean initialized = false;
	static boolean isInitialized() { return initialized; }
	
	public void run()
	{		
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		// start a thread that initializes each network interface
		List<Thread> initThreads = new ArrayList<Thread>();
		for (InetAddress address: LocalAddresses.getInstance().getLocalIPv4Addresses())
		{
			Thread thread = new SocketCommProviderInitThread(address);
			initThreads.add(thread);
			thread.start();
		}
		// wait until all the threads have finished
		for (Thread thread: initThreads)
		{
			try
			{
				thread.join();
			}
			catch (InterruptedException e)
			{
			}
		}
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
		initialized = true;
		// check if some TCP tests have to be started
		// (that is if some peers are known already)
		for (F2FPeer peer: F2FComputing.getPeers())
		{
			if (F2FComputing.getLocalPeer().equals(peer)) continue;
			peerContacted(peer);
		}
	}
	
	private class SocketCommProviderInitThread extends Thread implements Activity
	{
		private InetAddress address = null;
		private SocketCommProviderInitThread(InetAddress address)
		{
			super("SocketCommInitThread [" + address.getHostAddress() + "]");
			this.address = address;
		}
		
		public void run()
		{
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
			InetSocketAddress inetSoc = new InetSocketAddress(address, F2FProperties.getF2FProperties().getCommLayerProperties().iSocketCommunicationDefaultPort);
			try
			{
				TCPCommunicationProvider.getInstance().addServerSocket(inetSoc);
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
			}
			catch (IOException e)
			{
				log.warn(e.getMessage());
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
				return;
			}
		}

		public String getActivityName()
		{
			return getName();
		}

		public Activity getParentActivity()
		{
			return TCPCommInitiator.this;
		}
	}

	public String getActivityName()
	{
		return "SocketCommInitiator";
	}
	public Activity getParentActivity()
	{
		return TCPCommunicationProvider.getInstance();
	}

	private HashMap<F2FPeer, TCPTester> tcpTesters = new HashMap<F2FPeer, TCPTester>();
	public void peerContacted(F2FPeer peer)
	{
		// do not start TCP tests before local IP info is resolved and
		// server socket(s) started
		if (!isInitialized()) return;
		
		if (tcpTesters.containsKey(peer)) return;
		synchronized (tcpTesters)
		{
			if (tcpTesters.containsKey(peer)) return;
			TCPTester test = new TCPTester(peer);
			tcpTesters.put(peer, test);
			test.start();
		}
	}

	public void peerUnContacted(F2FPeer peer)
	{
		synchronized (tcpTesters)
		{
			tcpTesters.remove(peer);
		}
	}
}
