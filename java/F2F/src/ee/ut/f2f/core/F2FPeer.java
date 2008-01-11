package ee.ut.f2f.core;

import java.util.ArrayList;
import java.util.UUID;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.comm.socket.SocketCommunicationProvider;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.ui.model.StunInfoTableItem;
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
	public F2FPeer(String displayName)
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
			} else {
				//logger.debug("CommunicationProvider [" + comm + "] not found in list, nothing removed");
			}
			//TODO: remove, if it is not needed
			//if (commProviders.size() == 1 && commProviders.contains(getSocketCommunicationProvider()))
			//	commProviders.remove(getSocketCommunicationProvider());
		}
	}
	public void sendMessage(Object message) throws CommunicationFailedException
	{
		logger.info("Send message: " + ((F2FMessage)message).getData() + ", to: " + getDisplayName());
		
		// Loopback
		if (this.id.equals(F2FComputing.getLocalPeer().getID()))
		{
			logger.debug("Sending F2FMessage to MYSELF - loopback");
			F2FComputing.messageRecieved(message, this.getID());			
			return;
		}
		
		/*TODO: remove
		//Sending Other Messages
		StunInfoTableItem sinft = (StunInfoTableItem)F2FComputingGUI.controller.getStunInfoTableModel().get(this.getID().toString());
		if (sinft != null && sinft.isTcpConnectivityTested() && sinft.canConnectViaTCP()){
			logger.debug("F2FPeer [" + getID().toString() + "] tcp tested [" + sinft.isTcpConnectivityTested() + "] can use tcp [" + sinft.canConnectViaTCP() + "]");
			SocketCommunicationProvider scp = getSocketCommunicationProvider();
			if(scp != null){
				logger.debug("Using SocketCommunicationProvider for F2FPeer [" + this.getID() + "]");
				try{
					if(((F2FMessage) message).getType().equals(F2FMessage.Type.NAT)){
						logger.debug("Message is type of NatMessage, sending through SipCommunication");
					} else {
						scp.sendMessage(this.getID(), message);
						logger.debug("Succesfully sent message, using SocketCommunicationProvider for F2FPeer [" + this.getID() + "]");
						return;
					}
				} catch (CommunicationFailedException e){
					logger.error("Unable to send message using SocketCommunicationProvider for F2FPeer [" + this.getID() + "]",e);
				}
			} else {
				logger.error("SocketCommunicationProvider is null, for F2FPeer [" + this.getID() + "]");
			}
		}
		*/
		
		// try to send the message to the receiver
		// use high-weight comm providers before low-weight ones
		for (int i = 0; i < commProviders.size(); i++)
		{
			CommunicationProvider commProvider = commProviders.get(i);
			try
			{	
				//logger.debug("Using SipCommucationProvider sending F2FMessage to [" + this.getID().toString() + "]");
				commProvider.sendMessage(id, message);
				//logger.debug("Message successfully sent using SipCommunicationProvider");
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
	/*TODO: remove
	private SocketCommunicationProvider getSocketCommunicationProvider(){
		for(CommunicationProvider commProv : commProviders){
			if (commProv instanceof SocketCommunicationProvider){
				return (SocketCommunicationProvider) commProv;
			}
		}
		return null;
	}
	*/
}