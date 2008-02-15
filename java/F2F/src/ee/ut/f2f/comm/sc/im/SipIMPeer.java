/**
 * 
 */
package ee.ut.f2f.comm.sc.im;

import net.java.sip.communicator.service.protocol.Contact;

import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.util.F2FDebug;

class SipIMPeer
{
	private String ID = null;
	private String displayName = null;
	private Contact contact = null;

	public SipIMPeer(String sID, String displayName)
	{
		this.ID = sID;
		this.displayName = displayName;
	}
	
	public SipIMPeer(Contact c)
	{
		if (c == null) throw new RuntimeException("SipPeer created with Contact == NULL!");
		contact = c;		  
		ID = c.getAddress();
		displayName = c.getDisplayName();
	}
	
	public String getDisplayName() { return displayName; }
	
	
	public String getID()
	{
		return ID;
	}
	
	public synchronized void sendMessage(Object message) throws CommunicationFailedException
	{
		if (contact != null)
			SipIMCommunicationProvider.sendIMmessage(contact, message);
		else
			F2FDebug.println("\t\t ERROR (contact == null) sendMessage() called on peer " + displayName);
	}
	
	public String toString() 
	{
		return displayName;
	}
}
