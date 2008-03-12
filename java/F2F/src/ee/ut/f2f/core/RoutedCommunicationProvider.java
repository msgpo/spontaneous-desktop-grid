package ee.ut.f2f.core;

import java.util.UUID;

import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.util.logging.Logger;

class RoutedCommunicationProvider implements CommunicationProvider
{
	private static final Logger logger = Logger.getLogger(RoutedCommunicationProvider.class);
	
	private F2FPeer masterPeer = null;
	private UUID destinationPeerID = null;
	
	RoutedCommunicationProvider(F2FPeer masterPeer, UUID uuid)
	{
		this.masterPeer = masterPeer;
		destinationPeerID = uuid;
	}

	public int getWeight()
	{
		return 5;
	}

	public void sendMessage(UUID destinationPeer, Object message)
			throws CommunicationFailedException
	{
		if (destinationPeer.equals(destinationPeerID))
		{
			RoutedMessage routedMessage = 
				new RoutedMessage(
					RoutedMessage.Type.ROUTE,
					destinationPeer,
					message);
			masterPeer.sendMessage(routedMessage);
		}
		else logger.error("wrong usage of RoutedCommunicationProvider::sendMessage");
	}

	public void sendMessageBlocking(UUID destinationPeer, Object message,
			long timeout, boolean countTimeout)
			throws CommunicationFailedException, InterruptedException
	{
		if (destinationPeer.equals(destinationPeerID))
		{
			RoutedMessage routedMessage = 
				new RoutedMessage(
					RoutedMessage.Type.ROUTE_BLOCKING,
					destinationPeer,
					message);
			F2FPeer peer = F2FComputing.getPeer(destinationPeer);
			peer.routeReport = false;
			masterPeer.sendMessageBlocking(routedMessage);
			// now the message has reached the master but we still
			// have to wait until it has reached the final destination
			synchronized (peer)
			{
				if (countTimeout) peer.wait(timeout);
				else peer.wait();
			}
			if (!peer.routeReport)
				throw new MessageNotDeliveredException(message);
		}
		else logger.error("wrong usage of RoutedCommunicationProvider::sendMessageBlocking");
	}
}
