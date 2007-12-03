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

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.logging.Logger;

class SocketPeer implements Activity
{
	final private static Logger log = Logger.getLogger(SocketPeer.class);
	
	
	private SocketCommunicationProvider layer;
	public SocketCommunicationProvider getCommunicationLayer()
	{
		return this.layer;
	}
	
	protected InetSocketAddress socketAddress;
	private Socket outSocket;
	private ObjectOutput oo;
	private ObjectInput oi;
	
	SocketPeer(SocketCommunicationProvider layer, InetSocketAddress socketAddress)
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
		String id = F2FComputingGUI.controller.getStunInfoTableModel().getByLocalIp(socketAddress.getAddress().getHostAddress()).getId();
		if (id == null) throw new NullPointerException("No F2FPeer id found by ip in StunInfoTableModel");
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
			log.debug("\t\tSent message '" + message + "' to '" + getID() + "'");
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
		runSocketThread();
	}

	private void runSocketThread() {
		new Thread(new Runnable()
		{
			public void run()
			{
				try {
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.STARTED, "start receiving messages"));
					while(true)
					{
						try
						{
							Object message = oi.readObject();
							log.debug("\t\tReceived message from"
									+ " '" + socketAddress + "'. Message: '" + message + "'.");
	//						TODO
	//						for(CommunicationListener listener: layer.getListeners())
	//						{
	//							listener.messageRecieved(message, SocketPeer.this);
	//						}
						}
						catch (ClassNotFoundException e)
						{
							log.debug("\t\tError reading object from '"+socketAddress+"'" + e);
						}
					}
					//ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
					//		ActivityEvent.Type.FINISHED, "end receiving messages"));
				} catch (IOException e) {
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.FAILED, e.toString()));
					throw new RuntimeException(e);
				} catch (RuntimeException e) {
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.FAILED, e.toString()));
					throw e;
				}
			}
		}).start();
	}
	
	public String toString() 
	{
		return getID();
	}

	public String getDisplayName() { return getID(); }

	public String getActivityName() {
		return "Peer "+socketAddress;
	}

	public Activity getParentActivity() {
		return layer;
	}
}
