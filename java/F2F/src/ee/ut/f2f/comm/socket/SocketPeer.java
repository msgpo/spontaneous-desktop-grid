/**
 * 
 */
package ee.ut.f2f.comm.socket;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

class SocketPeer implements Peer
{

	private SocketCommunicationLayer layer;
	public CommunicationLayer getCommunicationLayer()
	{
		return this.layer;
	}
	
	protected InetSocketAddress socketAddress;
	private Socket outSocket;
	private ObjectOutput oo;
	private ObjectInput oi;
	
	SocketPeer(SocketCommunicationLayer layer, InetSocketAddress socketAddress)
	{
		this.layer = layer;
		this.socketAddress = socketAddress;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ee.ut.f2f.comm.Peer#getID()
	 */
	public String getID()
	{
		// Get the ID as <hostname>:<port>
		String id = socketAddress.getAddress().getHostName()+":"+socketAddress.getPort();
		
		// If restulted ID has also IP with its hostname, then resolve this only into hostname.
		if (id.indexOf('/') != -1)
		{
			String hostname = id.substring(0, id.indexOf('/'));
			String port = id.substring(id.indexOf('/')+1, id.length());
			id = hostname + ":" + port;
		}
		
		return id;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.Peer#sendMessage(ee.ut.f2f.comm.Message)
	 */
	public synchronized void sendMessage(Object message)
			throws CommunicationFailedException
	{
		try {
			getOo().writeObject(message);
			F2FDebug.println("\t\tSent message '" + message + "' to '" + getID() + "'");
		} catch (IOException e) {
			throw new CommunicationFailedException(e);
		}
	}

	private ObjectOutput getOo() throws IOException 
	{
		if(oo == null)
		{
			oo = new ObjectOutputStream(getOutSocket().getOutputStream());
			// Writing peer name into the outpustream for the other side to initialize the connection.
			String uid = layer.getLocalPeer().getID();
			oo.writeObject(uid);
		}
		return oo;
	}
	
	private Socket getOutSocket() throws IOException 
	{
		if(outSocket == null) 
		{
			outSocket = new Socket(socketAddress.getAddress(), socketAddress.getPort()); 
		}
		return outSocket;
	}

	void setOi(ObjectInput oi_)
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
						F2FDebug.println("\t\tReceived message from"
								+ " '" + socketAddress + "'. Message: '" + message + "'.");
						for(CommunicationListener listener: layer.getListeners())
						{
							listener.messageRecieved(message, SocketPeer.this);
						}
					}
					catch (Exception e)
					{
						F2FDebug.println("\t\tError reading object from '"+socketAddress+"'" + e);
					}
				}
			}
		}).start();
	}
	
	public String toString() 
	{
		return getID();
	}

	public String getDisplayName() { return getID(); }
}
