/**
 * 
 */
package ee.ut.f2f.comm.socket;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.threads.TCPTester;

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
	
	public String getID()
	{
		String id = F2FComputingGUI.controller.getStunInfoTableModel().getByLocalIp(socketAddress.getAddress().getHostAddress()).getId();
		if (id == null) throw new NullPointerException("No F2FPeer id found by ip in StunInfoTableModel");
		return id;
	}

	public synchronized void sendMessage(Object message)
			throws CommunicationFailedException
	{
		try {
			getOo().writeObject(message);
			log.debug("\t\tSent message '" + message + "' to id [" + getID() + "] ip [" + outSocket.getInetAddress().getHostAddress() + ":" + outSocket.getPort() + "]");
		} catch (IOException e) {
			throw new CommunicationFailedException(e);
		}
	}
	
	public void setOo(ObjectOutput oo) {
		this.oo = oo;
	}

	private ObjectOutput getOo() throws IOException 
	{
		if(oo == null)
		{
			oo = new ObjectOutputStream(getOutSocket().getOutputStream());
			// Writing peer name into the output stream for the other side to initialize the connection.
			String uid = layer.getLocalPeer().getID();
			oo.writeObject(uid);
			//Client starting listening server respond
			oi = new ObjectInputStream(outSocket.getInputStream());
			runSocketThread();
		}
		return oo;
	}
	
	public void setOutSocket(Socket soc) {
		outSocket = soc;
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
					log.debug(getActivityName() + " Starting listening to Peer id [" + getID() + "]");
					log.debug(getActivityName() + " Remote socket [" + outSocket.getRemoteSocketAddress() + "]");
					log.debug(getActivityName() + " Local Bind [" + outSocket.getLocalAddress().getHostAddress() + ":" + outSocket.getLocalPort() + "]");
					//TODO: exit this thread when the peer is not used any more + 
					//      close used sockets
					while(true)
					{
						try
						{
							Object message = oi.readObject();
							log.debug("\t\tReceived message from id [" + getID() + "] ip [" +
									 outSocket.getRemoteSocketAddress() + ":" + "]"  + "'. Message: '" + message + "'.");
							/*TODO: remove, this should not be done here
							if(message instanceof F2FMessage && ((F2FMessage) message).getType().equals(F2FMessage.Type.TCP)){
								log.debug("Message Type TCP Test forwarding to TCPTester");
								TCPTester tester = F2FComputingGUI.natMessageProcessor.getConnectionManager().getTCPTester(getID().toString());
								if (tester != null){
									tester.receivedTCPTest(((F2FMessage) message).getData());
								}
							} else {
							*/
								F2FComputing.messageRecieved(message, UUID.fromString(getID()));
							//}
						}
						catch (ClassNotFoundException e)
						{
							log.debug("\t\tError reading object from id [" + getID() + "] ip [" +
									 outSocket.getRemoteSocketAddress() + "]" + e);
						}
					}
				} catch (IOException e) {
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.FAILED, e.toString()));
					throw new RuntimeException(e);
				} catch (RuntimeException e) {
					ActivityManager.getDefault().emitEvent(new ActivityEvent(SocketPeer.this,
							ActivityEvent.Type.FAILED, e.toString()));
					throw e;
				} finally {
					log.debug(getActivityName() + " Stopping listening to Peer id [" + getID() + "]");
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
		return "Socket Listening Thread, Peer id [" + getID() + "]";
	}

	public Activity getParentActivity() {
		return layer;
	}
}
