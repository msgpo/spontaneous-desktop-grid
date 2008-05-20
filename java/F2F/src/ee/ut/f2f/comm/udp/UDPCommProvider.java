package ee.ut.f2f.comm.udp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
	
    private Map<UUID, Collection<UDPConnection>> udpConnections = Collections.synchronizedMap(new HashMap<UUID, Collection<UDPConnection>>());
    private UDPConnection getConnection(UUID id)
    {
    	if (!udpConnections.containsKey(id)) return null;
        return udpConnections.get(id).iterator().next();
    }
    
	public void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException
	{
        UDPConnection conn = getConnection(destinationPeer);
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
        UDPConnection conn = getConnection(destinationPeer);
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
	
	void addConnection(UDPConnection udpConnection)
	{
		if (udpConnection == null) return;
		boolean newPeer = false;
		if (!udpConnections.containsKey(udpConnection.remotePeer.getID()))
		{
			newPeer = true;
			udpConnections.put(udpConnection.remotePeer.getID(), new LinkedList<UDPConnection>());
		}
		udpConnections.get(udpConnection.remotePeer.getID()).add(udpConnection);
        // if the first connection is made notify the Core about it
        // it means UDP connection can be used!
        if (newPeer)
        	F2FComputing.peerContacted(udpConnection.remotePeer.getID(), "", this);
    }    
	
	void removeConnection(UDPConnection udpConnection)
	{
		if (udpConnection == null) return;
		if (!udpConnections.containsKey(udpConnection.remotePeer.getID())) return;
		
		udpConnections.get(udpConnection.remotePeer.getID()).remove(udpConnection);
		if (udpConnections.get(udpConnection.remotePeer.getID()).isEmpty())
			udpConnections.remove(udpConnection.remotePeer.getID());
		// if the last connection was removed notify the Core about it
        // it means UDP connection can not be used any more
        if (!udpConnections.containsKey(udpConnection.remotePeer.getID()))
			F2FComputing.peerUnContacted(udpConnection.remotePeer.getID(), this);
	}
}
