package ee.ut.f2f.comm.udp;

import ee.ut.f2f.activity.Activity;

public class UDPCommProvider implements Activity
{
	private UDPCommProvider()
	{
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
}
