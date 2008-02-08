package ee.ut.f2f.comm.udp;

import java.io.Serializable;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FMessageListener;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.stun.LocalStunInfo;
import ee.ut.f2f.util.stun.StunInfo;

public class UDPTester extends Thread implements Activity, F2FMessageListener
{
	final private static Logger log = Logger.getLogger(UDPTester.class);
	
	private F2FPeer remotePeer = null;
	
	private enum Status
	{
		INIT,
		WAITING_STUN_INFO,
		GOT_STUN_INFO
	}
	private Status status = null;
	
	public UDPTester(F2FPeer peer)
	{
		super("UDPTester [" + peer.getDisplayName() + "]");
		remotePeer = peer;
		status = Status.INIT;
	}
	
	private StunInfo remoteStunInfo = null; 
	public void messageReceived(Object message, F2FPeer sender)
	{
		if (sender.equals(remotePeer))
		{
			if (message instanceof UDPTestMessage)
				receivedUDPTestMessage((UDPTestMessage)message);
			else
				log.warn("UDPTester.messageRecieved() handles only UDPTestMessage");
		}
	}
	
	private void receivedUDPTestMessage(UDPTestMessage msg)
	{
		if (status == Status.INIT)
		{
			if (msg.type == UDPTestMessage.Type.INIT);
				status = Status.WAITING_STUN_INFO;
		}
		else if (status == Status.WAITING_STUN_INFO)
		{
			if (msg.type == UDPTestMessage.Type.STUN_INFO)
			{
				remoteStunInfo = msg.stunInfo; 
				status = Status.GOT_STUN_INFO;
			}
		}
		else if (msg.type != UDPTestMessage.Type.INIT)
		{	
			log.error("receivedTCPTestMessage() at illegal moment: " + toString());
		}
	}
	
	public void run()
	{
		// do not start TCP tests before SocketComm providers are initialized
		if (!UDPCommInitiator.isInitialized()) return;
		
		F2FComputing.addMessageListener(UDPTestMessage.class, this);
		// just for information catch any exceptions that may occur
		try
		{			
			testProcess();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.warn(getActivityName() + e.getMessage());
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, e.getMessage()));
		}
		F2FComputing.removeMessageListener(UDPTestMessage.class, this);
	}
	
	private void testProcess() throws CommunicationFailedException
	{
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		log.debug(getActivityName() + "started");
		
		// make sure that other peer has started the test too
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.CHANGED, "waiting for init"));
		new Thread()
		{
			public void run()
			{
				while (true)
				{
					try {
						remotePeer.sendMessage(new UDPTestMessage());
						Thread.sleep(1000);
					} catch (Exception e) {}
					if (status != Status.INIT) return;
				}
			}
		}.start();
		for (int i = 0; i < 60; i++)
		{
			if (status != Status.INIT) break;
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}
		if (status == Status.INIT)
		{
			log.error("timeout while waiting for init from remote UDP test thread");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, "timeout while waiting for init from remote UDP test thread"));
			// stop INIT sender
			status = null;
			return;
		}
		
		// exchange the STUN info
		remotePeer.sendMessage(
			new UDPTestMessage(LocalStunInfo.getInstance().getStunInfo()));
		// wait at most 30 seconds for remote addresses
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.CHANGED, "waiting for addresses"));
		for (int i = 0; i < 60; i++)
		{
			if (status == Status.GOT_STUN_INFO) break;
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}
		if (status != Status.GOT_STUN_INFO)
		{
			log.error("timeout while waiting for remote STUN info");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, "timeout while waiting for remote STUN info"));
			return;
		}
		if (remoteStunInfo == null)
		{
			log.error("remoteStunInfo == null");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, "remoteStunInfo == null"));
			return;
		}
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.CHANGED, "got remote STUN info"));
		log.debug("got remote STUN info: " + remoteStunInfo);
	}

	public String getActivityName()
	{
		return getName();
	}
	public Activity getParentActivity()
	{
		return null;
	}
}

class UDPTestMessage implements Serializable
{
	private static final long serialVersionUID = 8503336434324780827L;
	
	enum Type
	{
		INIT,
		STUN_INFO
	}
	
	Type type = null;
	UDPTestMessage()
	{
		type = Type.INIT;
	}
	
	StunInfo stunInfo = null;
	UDPTestMessage(StunInfo stunInfo)
	{
		type = Type.STUN_INFO;
		this.stunInfo = stunInfo;
	}
	
	public String toString()
	{
		String s = "UDPTestMessage ";
		if (type == Type.INIT) s += "INIT";
		else if (type == Type.STUN_INFO) s += "STUN_INFO";
		return s;
	}
}
