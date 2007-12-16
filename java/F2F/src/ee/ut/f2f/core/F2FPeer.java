package ee.ut.f2f.core;

import java.util.ArrayList;
import java.util.Collection;
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
	
	private Collection<CommunicationProvider> commProviders = null;
	public void addCommProvider(CommunicationProvider comm)
	{
		synchronized (commProviders)
		{
			if (!commProviders.contains(comm))
				commProviders.add(comm);
		}
	}
	public void removeCommProvider(CommunicationProvider comm)
	{
		synchronized (commProviders)
		{
			if (commProviders.contains(comm)){
				commProviders.remove(comm);
				logger.debug("Removed CommunicationProvider [" + comm + "] from F2FPeer [" + this.getID().toString() + "]");
			} else {
				logger.debug("CommunicationProvider [" + comm + "] not found in list, nothing removed");
			}
			if (commProviders.size() == 1 && commProviders.contains(getSocketCommunicationProvider()))
				commProviders.remove(getSocketCommunicationProvider());
		}
	}
	public void sendMessage(Object message) throws CommunicationFailedException
	{
		logger.info("Send message: " + ((F2FMessage)message).getData() + ", to: " + getDisplayName());
		
		//Loopback
		if(this.id.equals(F2FComputing.getLocalPeer().getID()))
		{
			logger.debug("Sending F2FMessage to MYSELF - loopback");
			F2FComputing.messageRecieved(message, this.getID());			
			return;
		}
		
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
					logger.error("Unable to send message, using SocketCommunicationProvider for F2FPeer [" + this.getID() + "]",e);
					if (((F2FMessage) message).getType().equals(F2FMessage.Type.TCP)){
						logger.error("Failed testing TCP connectivity with F2FPeer [" + this.getID() + "]");
						return;
					} else {
						logger.error("Using SipCommunicationProvider for F2FPeer [" + this.getID() + "]");
					}
				}
			} else {
				logger.error("SocketCommunicationProvider is null, using SipCommunicationProvider for F2FPeer [" + this.getID() + "]");
			}
		}
		
		for (CommunicationProvider commProvider: commProviders)
		{
			try
			{	
				if(!(commProvider instanceof SocketCommunicationProvider)){	
					logger.debug("Using SipCommucationProvider sending F2FMessage to [" + this.getID().toString() + "]");
					commProvider.sendMessage(id, message);
					logger.debug("Message successfully sent using SipCommunicationProvider");
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
	
	public boolean removeCommunicationProvider(CommunicationProvider communicationProvider){
		if (communicationProvider == null) throw new NullPointerException("CommunicationProvider is null");
		if(commProviders.contains(communicationProvider)){
				try{
					return commProviders.remove(communicationProvider);
				} finally {
					logger.debug("Removed CommunicationProvider [" + communicationProvider + "] from F2FPeer [" + this.getID().toString() + "]");
				}
		} else {
			logger.debug("CommunicationProvider [" + communicationProvider + "] not found in list, nothing removed");
			return false;
		}
	}
}