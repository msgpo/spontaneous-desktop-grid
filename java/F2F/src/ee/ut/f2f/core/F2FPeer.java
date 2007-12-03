package ee.ut.f2f.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.comm.socket.SocketCommunicationProvider;
import ee.ut.f2f.util.F2FMessage;
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
	F2FPeer()
	{
		this.id = UUID.randomUUID();
	}
	F2FPeer(String displayName)
	{
		this.id = UUID.randomUUID();
		this.displayName = displayName;
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
	public void addCommProvider(CommunicationProvider comm)
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
		logger.debug("Sending F2FMessage to [" + this.getID().toString() + "]");
		//Loopback
		if(this.id.equals(F2FComputing.getLocalPeer().getID()))
		{
			F2FComputing.messageRecieved(message, this.getID());
			return;
		}
		
		SocketCommunicationProvider scp = getSocketCommunicationProvider();
		if(scp != null){
				logger.debug("Using SocketCommunicationProvider for F2FPeer [" + this.getID() + "]");
				try{
					scp.sendMessage(this.getID(), message);
					logger.debug("Succesfully sent message, using SocketCommunicationProvider for F2FPeer [" + this.getID() + "]");
					return;
				} catch (CommunicationFailedException e){
					logger.error("Unable to send message, using SocketCommunicationProvider for F2FPeer [" + this.getID() + "]",e);
					logger.error("Using SipCommunicationProvider for F2FPeer [" + this.getID() + "]");
				}
		} else {
			logger.error("SocketCommunicationProvider is null, using SipCommunicationProvider for F2FPeer [" + this.getID() + "]");
		}
		
		for (CommunicationProvider commProvider: commProviders)
		{
			try
			{	
				if(!(commProvider instanceof SocketCommunicationProvider)){	
					commProvider.sendMessage(id, message);
				}
			}
			catch (Exception e)
			{
				logger.warn("Error sending message to "+id+" through "+commProvider);
				// try again with different communication provider
				continue;
			}
			// return if message was sent successfully
			return;
		}
		// throw an exception if message is not sent
		throw new CommunicationFailedException();
	}
	
	private SocketCommunicationProvider getSocketCommunicationProvider(){
		for(CommunicationProvider commProv : commProviders){
			if (commProv instanceof SocketCommunicationProvider){
				return (SocketCommunicationProvider) commProv;
			}
		}
		return null;
	}
}