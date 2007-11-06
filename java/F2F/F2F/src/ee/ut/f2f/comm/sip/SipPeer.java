/**
 * 
 */
package ee.ut.f2f.comm.sip;

import java.io.IOException;
import java.net.Socket;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.impl.gui.*;

import org.osgi.framework.ServiceReference;
import org.p2psockets.P2PSocket;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.F2FTestsMessageListener;
import ee.ut.f2f.util.SipMsgListener;

class SipPeer implements Peer
{
	private SipCommunicationLayer commLayer;
	private String ID;
	private String displayName;
	private Socket outSocket;
	private SipObjectOutput oo;
	private SipObjectInput oi;
	private OperationSetBasicInstantMessaging m_im;
	private Contact m_sipContact;
	
	public SipPeer(SipCommunicationLayer layer, String sID, String displayName, Contact c)
	{
		if (c != null) {
		  m_sipContact = c;		  
		  m_im = (OperationSetBasicInstantMessaging) m_sipContact.getProtocolProvider()
              .getOperationSet(OperationSetBasicInstantMessaging.class);
		  m_im.addMessageListener(SipMsgListener.getInstance());			 
		  System.out.println("Contact added for peer: " + c.getDisplayName() + " - " + c.getAddress());		 
		}
		this.commLayer = layer;	
		this.ID = sID;
		this.displayName = displayName;
	}
	
	/*Message msg = m_im.createMessage("Hello f2f-world!");	
	im.sendInstantMessage(contact, msg);
	im.addMessageListener(new F2FTestsMessageListener() );*/
	
	public CommunicationLayer getCommunicationLayer()
	{
		return this.commLayer;
	}
	
	public String getDisplayName() { return displayName; }
	
	
	public String getID()
	{
		return ID;
	}
	
	
	public Contact getContact() {
		return m_sipContact;
	}

	public boolean isOnline()
	{
		return true;
	}
/*
	public synchronized void sendMessage(Object message)
			throws CommunicationFailedException
	{
		try
		{
			getOo().writeObject(message);
			F2FDebug.println("\t\tSent message '" + message + "' to '" + getID() + "'");
		}
		catch (IOException e)
		{
			throw new CommunicationFailedException(e);
		}
	}
*/		
	public synchronized void sendMessage(Object message) throws CommunicationFailedException {
		System.out.println("Trying to send message '" + message + "' to " + getContact().getDisplayName());
		if (getContact() != null) {			
			Message msg = m_im.createMessage((String)message);	
			m_im.sendInstantMessage(getContact(), msg);
			F2FDebug.println("\t\tSent message '" + (String)message + "' to '" + getContact().getDisplayName() + "'");
		}
		else
			F2FDebug.println("Contact not found for: " + this);
		
	}	
	
	private SipObjectOutput getOo() throws IOException
	{
		if(oo == null)
		{
			//oo = new ObjectOutputStream(getOutSocket().getOutputStream());
			oo = new SipObjectOutput(getOutSocket().getOutputStream());
			// handshake
			oo.writeObject(SipCommunicationLayer.SIP_LAYER_NETWORK_PASSWORD);
			//F2FDebug.println("\t\tSent password '" + SipCommunicationLayer.SIP_LAYER_NETWORK_PASSWORD + "'");
			// Writing peer name into the outpustream for the other side to initialize the connection.
			String uid = commLayer.getLocalPeer().getID();
			oo.writeObject(uid);
			oo.writeObject(commLayer.getLocalPeer().getDisplayName());
		}
		return oo;
	}
	
	private Socket getOutSocket() throws IOException
	{
		if(outSocket == null)
		{
			outSocket = new P2PSocket(ID, SipCommunicationLayer.SIP_LAYER_NETWORK_PORT);
		}
		return outSocket;
	}

	public void setOi(SipObjectInput oi_)
	{
		this.oi = oi_;
		new Thread(new Runnable()
		{
			public void run()
			{
				while(true)
				{
					try
					{
						Object message = oi.readObject();
						F2FDebug.println("\t\tReceived a message from"
								+ " '" + ID + "'. Message: '" + message + "'.");
						for(CommunicationListener listener: commLayer.getListeners())
						{
							listener.messageRecieved(message, SipPeer.this);
						}
					}
					catch (Exception e)
					{
						F2FDebug.println("\t\tError reading object from '" + ID + "'! " + e);
					}
				}
			}
		}).start();
	}
	
	public String toString() 
	{
		return displayName;
	}
}
