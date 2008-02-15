package ee.ut.f2f.comm.udp;

import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.core.CommunicationFailedException;

public class UDPCommProvider implements CommunicationProvider, Activity
{
	private UDPCommProvider()
	{
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
		new UDPCommInitiator().start();
	}
	
	private static UDPCommProvider udpCommProvider = null;
	public static UDPCommProvider getInstance()
	{
		if (udpCommProvider != null) return udpCommProvider;
		synchronized (UDPCommProvider.class)
		{
			if (udpCommProvider != null) return udpCommProvider;
			return (udpCommProvider = new UDPCommProvider());
		}
	}
	public String getActivityName() 
	{
		return "UDPCommProvider";
	}
	public Activity getParentActivity()
	{
		return null;
	}
	
	public int getWeight()
	{
		return CommunicationProvider.UDP_COMM_WEIGHT;
	}
	
	public void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException
	{
		// TODO Auto-generated method stub
	}
}
