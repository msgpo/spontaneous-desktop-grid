package ee.ut.f2f.comm.socket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.logging.Logger;

public class SocketCommInitiator extends Thread implements Activity
{
	private final static Logger log = Logger.getLogger(SocketCommInitiator.class);
	
	public final static int DEFAULT_SOCKET_COMMUNICATION_PORT = 13000;
	
	public SocketCommInitiator()
	{
	}
	
	private static boolean initialized = false;
	static boolean isInitialized() { return initialized; }
	
	public void run()
	{
		// wait 1 second so that UI would initialize
		// events are not shown otherwize
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e1)	{}
		
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		// start a thread that initializes each network interface
		List<Thread> initThreads = new ArrayList<Thread>();
		for (InetAddress address: F2FComputing.getLocalPeer().getLocalIPs())
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
			peer.initiateTCPTester();
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
			InetSocketAddress inetSoc = new InetSocketAddress(address, DEFAULT_SOCKET_COMMUNICATION_PORT);
			try
			{
				SocketCommunicationProvider.getInstance().addServerSocket(inetSoc);
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
			}
			catch (CommunicationInitException e)
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
			return SocketCommInitiator.this;
		}
	}

	public String getActivityName()
	{
		return "SocketCommInitiator";
	}
	public Activity getParentActivity()
	{
		return null;
	}
}
