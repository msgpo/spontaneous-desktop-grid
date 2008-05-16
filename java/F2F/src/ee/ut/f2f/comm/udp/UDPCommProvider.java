package ee.ut.f2f.comm.udp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.logging.Logger;

public class UDPCommProvider implements CommunicationProvider, Activity
{
    private static final Logger logger = Logger.getLogger(UDPCommProvider.class);
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
	
    private Map<UUID, UDPConnection> udpConnections = Collections.synchronizedMap(new HashMap<UUID, UDPConnection>());
    void setConnection(UUID id, UDPConnection con)
    {
        if (id == null) return;
        if (con == null)
        {
            udpConnections.remove(id);
            F2FComputing.peerUnContacted(id, this);
            return;
        }
        boolean newPeer = !udpConnections.containsKey(id);
        udpConnections.put(id, con);
        if (newPeer)
        {
            F2FComputing.peerContacted(id, "", this);
        }
    }
    UDPConnection getConnection(UUID id)
    {
        return udpConnections.get(id);
    }
    
	public void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException
	{
        UDPConnection conn = udpConnections.get(destinationPeer);
        if (conn == null)
            throw new CommunicationFailedException("Can not use UDP connection to " + destinationPeer);
        //conn.sendMessage(message);
        try
        {
            conn.sendMessage(message);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("UDP: error in sendMessage() while sending a message to peer " + destinationPeer, e);
            throw new CommunicationFailedException(e);
        }
	}
	public void sendMessageBlocking(UUID destinationPeer, Object message, long timeout, boolean countTimeout) throws CommunicationFailedException, InterruptedException
	{
        UDPConnection conn = udpConnections.get(destinationPeer);
        if (conn == null)
            throw new CommunicationFailedException("Can not use UDP connection to " + destinationPeer);
        //conn.sendMessageBlocking(message, timeout, countTimeout);
        try
        {
            conn.sendMessageBlocking(message, timeout, countTimeout);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("UDP: error in sendMessageBlocking() while sending a message to peer " + destinationPeer, e);
            throw new CommunicationFailedException(e);
        }
	}
}
