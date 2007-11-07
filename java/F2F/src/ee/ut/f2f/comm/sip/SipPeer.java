/**
 * 
 */
package ee.ut.f2f.comm.sip;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

class SipPeer implements Peer
{
	private String ID = null;
	private String displayName = null;
	private OperationSetBasicInstantMessaging im = null;
	private Contact contact = null;

	public SipPeer(String sID, String displayName)
	{
		this.ID = sID;
		this.displayName = displayName;
	}
	
	public SipPeer(Contact c)
	{
		if (c == null) throw new RuntimeException("SipPeer created with Contact == NULL!");
		contact = c;		  
		im = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
    		.getOperationSet(OperationSetBasicInstantMessaging.class);
		ID = c.getAddress();
		displayName = c.getDisplayName();
	}
	
	public CommunicationLayer getCommunicationLayer()
	{
		return SipCommunicationLayer.getInstance();
	}
	
	public String getDisplayName() { return displayName; }
	
	
	public String getID()
	{
		return ID;
	}
	
	public boolean isOnline()
	{
		return true;
	}
	
	public synchronized void sendMessage(Object message) throws CommunicationFailedException
	{
		if (contact != null)
			SipCommunicationLayer.sendIMmessage(im, contact, message);
		else
			F2FDebug.println("\t\t ERROR (contact == null) sendMessage() called on peer " + displayName);
	}
	
	public String toString() 
	{
		return displayName;
	}
}
