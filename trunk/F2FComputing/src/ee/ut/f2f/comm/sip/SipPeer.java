/**
 * 
 */
package ee.ut.f2f.comm.sip;

import java.io.IOException;
import java.net.Socket;

import org.p2psockets.P2PSocket;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

class SipPeer implements Peer
{
	private SipCommunicationLayer commLayer;
	public CommunicationLayer getCommunicationLayer()
	{
		return this.commLayer;
	}
	
	private String ID;
	private String displayName;
	private Socket outSocket;
	private SipObjectOutput oo;
	private SipObjectInput oi;


	public String getDisplayName() { return displayName; }
	public SipPeer(SipCommunicationLayer layer, String sID, String displayName)
	{
		this.commLayer = layer;
		this.ID = sID;
		this.displayName = displayName;
	}
	
	public String getID()
	{
		return ID;
	}

	public boolean isOnline()
	{
		return true;
	}

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
