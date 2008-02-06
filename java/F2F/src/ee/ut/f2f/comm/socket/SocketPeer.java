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
import java.net.SocketException;
import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.CustomObjectInputStream;
import ee.ut.f2f.util.logging.Logger;

class SocketPeer implements Activity
{
	final private static Logger log = Logger.getLogger(SocketPeer.class);
	
	
	private SocketCommunicationProvider scProvider = null;
	public SocketCommunicationProvider getCommunicationLayer()
	{
		return this.scProvider;
	}
	
	protected InetSocketAddress socketAddress = null;
	private Socket outSocket = null;
	private ObjectOutput oo = null;
	private ObjectInput oi = null;
	private UUID id = null;
	
	SocketPeer(UUID id, SocketCommunicationProvider layer, InetSocketAddress socketAddress, boolean bIntroduce) throws IOException
	{
		this.id = id;
		this.scProvider = layer;
		this.socketAddress = socketAddress;
		if (bIntroduce) getOo();
	}

	public synchronized void sendMessage(Object message)
			throws CommunicationFailedException
	{
		try
		{
			getOo().writeObject(message);
		}
		catch (IOException e)
		{
			throw new CommunicationFailedException(e);
		}
	}
	
	void setOo(ObjectOutput oo)
	{
		this.oo = oo;
	}

	private ObjectOutput getOo() throws IOException 
	{
		if(oo == null)
		{
			oo = new ObjectOutputStream(getOutSocket().getOutputStream());
			// Writing peer ID into the output stream for the other side to initialize the connection.
			UUID uid = F2FComputing.getLocalPeer().getID();
			oo.writeObject(uid);
			//Client starting listening server respond
			oi = new CustomObjectInputStream(outSocket.getInputStream());
			runSocketThread();
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
		runSocketThread();
	}

	private void runSocketThread() {
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.STARTED, "start receiving messages"));
					//log.debug(getActivityName() + " Remote socket [" + outSocket.getRemoteSocketAddress() + "]");
					//log.debug(getActivityName() + " Local Bind [" + outSocket.getLocalAddress().getHostAddress() + ":" + outSocket.getLocalPort() + "]");
					while(true)
					{
						try
						{
							Object message = oi.readObject();
							//log.debug("\t\tReceived message from id [" + getID() + "] ip [" +
							//		 outSocket.getRemoteSocketAddress() + ":" + "]"  + "'. Message: '" + message + "'.");
							F2FComputing.messageRecieved(message, id);
						}
						catch (ClassNotFoundException e)
						{
							log.debug("Error reading object from id [" + id + "] ip [" +
									 outSocket.getRemoteSocketAddress() + "]" + e);
						}
					}
				}
				catch (SocketException e)
				{
					if (e.getMessage().equals("Connection reset"))
					{
						ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
								ActivityEvent.Type.FINISHED, "remote peer closed connection"));
					}
					else
					{
						ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
								ActivityEvent.Type.FAILED, e.toString()));
						throw new RuntimeException(e);
					}
				}
				catch (IOException e)
				{
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.FAILED, e.toString()));
					throw new RuntimeException(e);
				}
				catch (RuntimeException e)
				{
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.FAILED, e.toString()));
					throw e;
				}
				finally
				{
					try
					{
						if (oi != null) oi.close();
						oi = null;
						if (oo != null) oo.close();
						oo = null;
						if (outSocket != null) outSocket.close();
						outSocket = null;
					} catch (Exception e) {}
					log.debug(getActivityName() + " Stopping listening to Peer id [" + id + "]");
					scProvider.removeFriend(id);
				}
			}
		}).start();
	}
	
	public String toString() 
	{
		return id.toString();
	}

	public String getActivityName()
	{
		return "TCP Listening Thread, Peer id [" + id + "]";
	}

	public Activity getParentActivity()
	{
		return scProvider;
	}
}
