package ee.ut.f2f.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.util.F2FDebug;

public class F2FPeer
{
	private UUID id = null;
	public UUID getID() { return id; }
	
	private String displayName = null;
	public String getDisplayName() { return displayName; }
	public String toString() { return displayName; }

	/**
	 * Used only for the local peer creation.
	 */
	F2FPeer()
	{
		this.id = UUID.randomUUID();
	}
	
	/**
	 * Used for a remote peer creation.
	 * @param id
	 * @param displayName
	 */
	F2FPeer(UUID id, String displayName)
	{
		this.id = id;
		this.displayName = displayName;
		this.commProviders = new ArrayList<CommunicationProvider>();
	}
	
	boolean isContactable()
	{
		return commProviders.size() > 0;
	}
	
	private Collection<CommunicationProvider> commProviders = null;
	void addCommProvider(CommunicationProvider comm)
	{
		synchronized (commProviders)
		{
			if (!commProviders.contains(comm))
				commProviders.add(comm);
		}
	}
	void removeCommProvider(CommunicationProvider comm)
	{
		synchronized (commProviders)
		{
			if (commProviders.contains(comm))
				commProviders.remove(comm);
		}
	}
	public void sendMessage(Object message) throws CommunicationFailedException
	{
		for (CommunicationProvider commProvider: commProviders)
		{
			try
			{
				commProvider.sendMessage(id, message);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				// try again with different communication provider
				continue;
			}
			// return if message was sent successfully
			return;
		}
		// throw an exception if message is not sent
		throw new CommunicationFailedException();
	}
}