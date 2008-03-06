/**
 * 
 */
package ee.ut.f2f.comm.tcp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.BlockingMessage;
import ee.ut.f2f.comm.BlockingReply;
import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.JobCustomObjectInputStream;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.MessageNotDeliveredException;
import ee.ut.f2f.util.logging.Logger;

class TCPPeer implements Activity
{
	final private static Logger log = Logger.getLogger(TCPPeer.class);
	
	
	private TCPCommunicationProvider scProvider = null;
	public TCPCommunicationProvider getCommunicationLayer()
	{
		return this.scProvider;
	}
	
	protected InetSocketAddress socketAddress = null;
	private Socket outSocket = null;
	private ObjectOutput oo = null;
	private ObjectInput oi = null;
	private UUID id = null;
	
	TCPPeer(UUID id, TCPCommunicationProvider layer, InetSocketAddress socketAddress, boolean bIntroduce) throws IOException
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
			oi = new JobCustomObjectInputStream(outSocket.getInputStream());
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
					ActivityManager.getDefault().emitEvent(new ActivityEvent(TCPPeer.this,
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
							if (message instanceof BlockingMessage)
							{
								BlockingMessage msg = (BlockingMessage) message;
								F2FComputing.messageReceived(msg.data, id);
								sendMessage(new BlockingReply(msg));
							}
							else if (message instanceof BlockingReply)
							{
								BlockingReply msg = (BlockingReply) message;
								if (blockingMessages.containsKey(msg.ID))
								{
									BlockingMessage blockMsg = blockingMessages.get(msg.ID);
									blockingMessages.remove(msg.ID);
									synchronized (blockMsg)
									{
										//log.debug(msg.ID +" end WAIT "+System.currentTimeMillis());
										blockMsg.notify();
									}
								}
							}
							else F2FComputing.messageReceived(message, id);
						}
						catch (ClassNotFoundException e)
						{
							log.debug("Error reading object from id [" + id + "] ip [" +
									 outSocket.getRemoteSocketAddress() + "]" + e);
						}
					}
				}
				catch (Exception e){e.printStackTrace();}
				try
				{
					if (oi != null) oi.close();
					oi = null;
					if (oo != null) oo.close();
					oo = null;
					if (outSocket != null) outSocket.close();
					outSocket = null;
				} catch (Exception e) {}
				log.debug("Stopping listening to Peer id [" + id + "]");
				scProvider.removeFriend(id);
				ActivityManager.getDefault().emitEvent(new ActivityEvent(TCPPeer.this,
						ActivityEvent.Type.FINISHED, "connection closed"));
			}
		}).start();
	}
	
	public String toString() 
	{
		return id.toString();
	}

	public String getActivityName()
	{
		return "TCP conn to " + 
			(F2FComputing.getPeer(id) != null? F2FComputing.getPeer(id).getDisplayName(): id);
	}

	public Activity getParentActivity()
	{
		return scProvider;
	}

	private long msgID = 0;
	private synchronized long getMsgID() { return ++msgID; }
	private HashMap<Long, BlockingMessage> blockingMessages = new HashMap<Long, BlockingMessage>();
	
	public void sendMessageBlocking(Object message, long timeout, boolean countTimeout)
		throws CommunicationFailedException, InterruptedException
	{
		if (countTimeout && timeout <= 0) throw new MessageNotDeliveredException(message);
		
		BlockingMessage msg = new BlockingMessage(message, getMsgID());
		blockingMessages.put(msg.ID, msg);
		// start to wait for the confirmation before the message is sent out
		// this ensures that the reply is not received before the wait is called
		BlockingMessageThread t = new BlockingMessageThread(msg, timeout, countTimeout);
		t.start();
		// wait until the waiting thread has started before sending the message out
		while (!t.startedWaiting ||
			   !(t.getState() != Thread.State.WAITING || 
				 t.getState() != Thread.State.TIMED_WAITING)) Thread.sleep(5);
		sendMessage(msg);
		// wait until the confirmation is received
		t.join();
		// throw an exception if it occurred
		if (t.interruptEx != null) throw t.interruptEx;
		if (t.notDeliveredEx != null) throw t.notDeliveredEx;
	}

	private class BlockingMessageThread extends Thread
	{
		BlockingMessage msg;
		long timeout;
		boolean countTimeout;
		InterruptedException interruptEx = null;
		MessageNotDeliveredException notDeliveredEx = null;
		boolean startedWaiting = false;
		
		BlockingMessageThread(BlockingMessage msg, long timeout, boolean countTimeout)
		{
			this.msg = msg;
			this.timeout = timeout;
			this.countTimeout = countTimeout;
		}
		
		public void run()
		{
			synchronized(msg)
			{
				try {
					//log.debug(msg.ID + " start WAIT "+System.currentTimeMillis() + " - "+msg.data);
					startedWaiting = true;
					if (countTimeout)
						msg.wait(timeout);
					else msg.wait(0);
				} catch (InterruptedException ex) {
					interruptEx = ex;
					return;
				}
				if (blockingMessages.containsKey(msg.ID))
				{
					blockingMessages.remove(msg.ID);
					notDeliveredEx = new MessageNotDeliveredException(msg.data);
				}
			}				
		}
	}
}
