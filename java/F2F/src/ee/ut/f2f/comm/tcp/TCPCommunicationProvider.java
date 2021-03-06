/**
 * 
 */
package ee.ut.f2f.comm.tcp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.JobCustomObjectInputStream;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.logging.Logger;

public class TCPCommunicationProvider implements CommunicationProvider, Activity
{
	private final Logger log = Logger.getLogger(TCPCommunicationProvider.class);

	/**
	 * Name that identifies Socket communication layer.
	 */
	private static final String SOCKET_LAYER_ID = "F2FSocketCommLayer";
	
	private Hashtable<UUID, TCPPeer> peers = new Hashtable<UUID, TCPPeer>();
	
	/**
	 * Creates the communication layer with fixed count of friends.
	 */
	private static TCPCommunicationProvider tCPCommunicationProvider = null;
	private TCPCommunicationProvider()
	{
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
		new TCPCommInitiator().start();
	}

	void addFriend(UUID id, InetSocketAddress friend, boolean bIntroduce) throws IOException
	{
		if (peers.containsKey(id)) return;
		synchronized (peers)
		{
			if (peers.containsKey(id)) return;
			TCPPeer peer = new TCPPeer(id, this, friend, bIntroduce);
			peers.put(id, peer);
			F2FComputing.peerContacted(id, id.toString(), this);
		}
	}
	void removeFriend(UUID id)
	{
		synchronized (peers)
		{
			if (!peers.containsKey(id)) return;
			peers.remove(id);
			F2FComputing.peerUnContacted(id, this);
		}
	}
	
	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationLayer#findPeerByID(java.lang.String)
	 */
	public TCPPeer findPeerByID(UUID id)
	{
		synchronized (peers)
		{
			if (peers.containsKey(id))
				return peers.get(id);
		}
		return null;
	}

	public String getID()
	{
		return SOCKET_LAYER_ID;
	}
	
	private class SocketListener extends Thread implements Activity
	{
		/*
		 * This socket listenes for incoming connections from other peers.
		 */
		private ServerSocket serverSocket;
		private InetSocketAddress socketAddress = null;
		
		SocketListener(InetSocketAddress inetSoc) throws IOException
		{
			serverSocket = new ServerSocket();
			for (int i = 1; i < 11; i++)
			{
				try
				{
					serverSocket.bind(inetSoc);
					break;
				}
				catch (BindException e)
				{
					log.warn("Unable to bind ServerSocket to  local [" + inetSoc.getAddress().getHostAddress() + ":" + inetSoc.getPort() + "]");
					if (i == 10) throw e;
					inetSoc = new InetSocketAddress(inetSoc.getAddress(), inetSoc.getPort() + i);
				}
			}
			setName("SocketListener ["+ inetSoc.getAddress().getHostAddress() + ":" + inetSoc.getPort() + "]");
			socketAddress = inetSoc;
		}
		
		public void run()
		{
			ActivityEvent event;
			try
			{
				event = new ActivityEvent(this, ActivityEvent.Type.STARTED, 
						"Listening to incoming connections");
				ActivityManager.getDefault().emitEvent(event);
				
				// accept incoming connections from other peers
				acceptConnections();
				
				event = new ActivityEvent(this,	ActivityEvent.Type.FINISHED, 
						"Finished listening to incoming connections");
				ActivityManager.getDefault().emitEvent(event);
			}
			catch (RuntimeException e)
			{
				event = new ActivityEvent(this,	ActivityEvent.Type.FAILED, 
						e.toString());
				ActivityManager.getDefault().emitEvent(event);
				throw e;
			}
		}

		private void acceptConnections()
		{
			while(true)
			{
				try
				{
					// wait while someone tries to connect
					Socket socket = serverSocket.accept();
					// use JobCustomObjectInputStream to deserialize objects in
					// a communication provider, because otherwise custom classes
					// of a job can not be deserialized
					ObjectInput oi = new JobCustomObjectInputStream(socket.getInputStream());
					ObjectOutput oo = new ObjectOutputStream(socket.getOutputStream());
					log.debug("\t\tAccepted socket from IP: '"+socket.getInetAddress().getHostAddress()+"' port: "+ socket.getPort());
					log.debug("\t\tBinded socket on local IP: '"+socket.getLocalAddress().getHostAddress()+"' port: "+ socket.getLocalPort());
					Object message = oi.readObject();
					if (message instanceof TCPTestPacket)
					{
						TCPTestPacket testPacket = (TCPTestPacket)message;
						if (testPacket.address.equals(socketAddress))
						{
							log.debug("received TCPTest packet, sending reply");
							oo.writeObject(testPacket);
						}
						else
						{
							log.warn("received TCPTest packet with wrong address");
						}
						oi.close();
						oo.close();
						socket.close();
						continue;
					}
					// the first message has to be the ID of remote peer
					UUID remoteID = (UUID)message;
					synchronized (peers)
					{
						if (peers.containsKey(remoteID))
						{
							oi.close();
							oo.close();
							socket.close();
							log.warn("socket peer is already known");
							continue;
						}
						TCPPeer peer = new TCPPeer(remoteID, TCPCommunicationProvider.this, null, false);
						// start listening for messages from the peer
						peer.setOo(oo);
						peer.setOi(oi);
						peers.put(remoteID, peer);
						F2FComputing.peerContacted(remoteID, socket.getInetAddress().getHostAddress()+":"+ socket.getPort(), TCPCommunicationProvider.this);
						log.debug("Accepted TCP connection from '"+socket.getInetAddress().getHostAddress()+":"+ socket.getPort()+"'");
					}
				}
				catch (Exception e)
				{
					log.warn("Problems creating the socket communication: " + e);
					e.printStackTrace();
				}
			}
		}

		public String getActivityName()
		{
			return getName();
		}

		public Activity getParentActivity()
		{
			return TCPCommunicationProvider.this;
		}
	}

	public void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException
	{
		TCPPeer peer = null;
		synchronized (peers)
		{
			peer = peers.get(destinationPeer);
		}
		if (peer == null)
			throw new CommunicationFailedException("socket peer wasn't found - id: " + destinationPeer);
		peer.sendMessage(message);
	}
	
	public void sendMessageBlocking(UUID destinationPeer, Object message, long timeout, boolean countTimeout) throws CommunicationFailedException, InterruptedException
	{
		TCPPeer peer = null;
		synchronized (peers)
		{
			peer = peers.get(destinationPeer);
		}
		if (peer == null)
			throw new CommunicationFailedException("socket peer wasn't found - id: " + destinationPeer);
		peer.sendMessageBlocking(message, timeout, countTimeout);
	}

	public String getActivityName()
	{
		return "SocketCommProvider";
	}

	public Activity getParentActivity()
	{
		return null;
	}

	public int getWeight()
	{
		return CommunicationProvider.TCP_COMM_WEIGHT;
	}

	public static TCPCommunicationProvider getInstance()
	{
		if (tCPCommunicationProvider == null)
		{
			synchronized (TCPCommunicationProvider.class)
			{
				if (tCPCommunicationProvider == null)
					tCPCommunicationProvider = new TCPCommunicationProvider();
			}
		}
		return tCPCommunicationProvider;
	}

	private Collection<InetSocketAddress> serverSockets = new ArrayList<InetSocketAddress>();
	Collection<InetSocketAddress> getServerSocketAddresses() { return serverSockets; } 
	public void addServerSocket(InetSocketAddress inetSoc) throws IOException
	{
		SocketListener socketListener = new SocketListener(inetSoc);
		serverSockets.add(socketListener.socketAddress);
		
		new Thread(socketListener).start();
	}

	
	private void addFriend(final InetSocketAddress address)
	{
		new Thread()
		{
			public void run()
			{
				try
				{
					Socket socket = new Socket(address.getAddress(), address.getPort());
					ObjectOutput oo = new ObjectOutputStream(socket.getOutputStream());
					ObjectInput oi = new JobCustomObjectInputStream(socket.getInputStream());
					// send local peer's ID
					oo.writeObject(F2FComputing.getLocalPeer().getID());
					UUID remoteID = (UUID)oi.readObject();
					synchronized (peers)
					{
						if (peers.containsKey(remoteID))
						{
							oi.close();
							oo.close();
							socket.close();
							log.warn("socket peer is already known");
							return;
						}
						TCPPeer peer = new TCPPeer(remoteID, TCPCommunicationProvider.this, null, false);
						// start listening for messages from the peer
						peer.setOo(oo);
						peer.setOi(oi);
						peers.put(remoteID, peer);
						F2FComputing.peerContacted(remoteID, address.getAddress().getHostAddress() +":"+ address.getPort(), TCPCommunicationProvider.this);
						log.debug("Accepted remote connection from '"+remoteID+"'");
					}
				}
				catch (Exception e)
				{
					log.warn("could not find Socket peer at " + address.getAddress().getHostAddress() +":"+ address.getPort());
				}
			}
		}.start();
	}
	public void addFriends(Collection<InetSocketAddress> friends)
	{
		for (InetSocketAddress address: friends)
			addFriend(address);
	}
}
