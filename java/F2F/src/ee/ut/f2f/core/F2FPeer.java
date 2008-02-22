package ee.ut.f2f.core;

import java.util.ArrayList;
import java.util.UUID;

import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.util.logging.Logger;

public class F2FPeer
{
	private static final Logger logger = Logger.getLogger(F2FPeer.class);
	
	private UUID id = null;
	public UUID getID() { return id; }
	
	private String displayName = null;
	public String getDisplayName() { return displayName; }
	public String toString() { return displayName; }

	/**
	 * Used only for the local peer creation.
	 */	
	F2FPeer(UUID id, String displayName)
	{
		this.id = id;
		this.displayName = displayName;
	}
	
	/**
	 * Used for a remote peer creation.
	 * @param id
	 * @param displayName
	 */
	F2FPeer(UUID id, String displayName, CommunicationProvider provider)
	{
		this.id = id;
		this.displayName = displayName;
		this.commProviders = new ArrayList<CommunicationProvider>();
		addCommProvider(provider);
	}
	
	boolean isContactable()
	{
		return commProviders.size() > 0;
	}
	
	private ArrayList<CommunicationProvider> commProviders = null;
	/**
	 * 
	 * @param comm The provider to add
	 * @return The place in queue where the new provider was added, or -1 if already present
	 * or -2 if comm is null.
	 */
	int addCommProvider(CommunicationProvider comm)
	{
		if (comm == null) return -2;
		synchronized (commProviders)
		{
			if (!commProviders.contains(comm))
			{
				int place = 0;
				for (; place < commProviders.size(); place++)
					if (commProviders.get(place).getWeight() <= comm.getWeight()) break;
				commProviders.add(place, comm);
				return place;
			}
			return -1;
		}
	}
	void removeCommProvider(CommunicationProvider comm)
	{
		synchronized (commProviders)
		{
			if (commProviders.contains(comm))
			{
				commProviders.remove(comm);
				//logger.debug("Removed CommunicationProvider [" + comm + "] from F2FPeer [" + this.getID().toString() + "]");
			}
			else
			{
				//logger.debug("CommunicationProvider [" + comm + "] not found in list, nothing removed");
			}
			//TODO: remove, if it is not needed
			//if (commProviders.size() == 1 && commProviders.contains(getSocketCommunicationProvider()))
			//	commProviders.remove(getSocketCommunicationProvider());
		}
	}
	public void sendMessage(Object message) throws CommunicationFailedException
	{
		
		
		// Loopback
		if (this.id.equals(F2FComputing.getLocalPeer().getID()))
		{
			logger.debug("Sending F2FMessage to MYSELF - loopback");
			F2FComputing.messageReceived(message, this.getID());			
			return;
		}
		
		// try to send the message to the receiver
		// use high-weight comm providers before low-weight ones
		for (int i = 0; i < commProviders.size(); i++)
		{
			CommunicationProvider commProvider = commProviders.get(i);
			try
			{	
				commProvider.sendMessage(id, message);
				logger.info("TO "+getDisplayName()+"("+commProvider.getWeight()+")"+": "+message);
			}
			catch (Exception e)
			{
				logger.warn("Error sending message to "+getDisplayName()+" through "+commProvider.getClass());
				e.printStackTrace();
				// try again with different communication provider
				continue;
			}
			// return if message was sent successfully
			return;
		}
		// throw an exception if message is not sent
		throw new CommunicationFailedException("Peer " + getDisplayName() + " is not reachable!");
	}
}