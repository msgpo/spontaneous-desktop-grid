/**
 * 
 */
package ee.ut.f2f.comm.socket;

import java.io.IOException;
import java.io.ObjectInput;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.util.CustomObjectInputStream;
import ee.ut.f2f.util.logging.Logger;

public class SocketCommunicationProvider implements CommunicationProvider, Activity
{
	private final Logger log = Logger.getLogger(SocketCommunicationProvider.class);

	/**
	 * Name that identifies Socket communication layer.
	 */
	private static final String SOCKET_LAYER_ID = "F2FSocketCommLayer";
	
	private Collection<SocketPeer> peers = new ArrayList<SocketPeer>();
	private SocketPeer localPeer;

	/**
	 * Creates the communication layer with fixed count of friends.
	 * @throws CommunicationInitException 
	 */
	public SocketCommunicationProvider(InetSocketAddress localPeerAddr) throws CommunicationInitException
	{
		this.localPeer = new SocketPeer(this, localPeerAddr);
		new Thread(new SocketListener(this.localPeer.socketAddress)).start();
	}

	public void addFriend(InetSocketAddress friend){
		SocketPeer sPeer = new SocketPeer(this, friend);
		if(!peers.contains(sPeer)){
			peers.add(sPeer);
		}
	}
	
	public void addFriend(String ip, int port){
		InetSocketAddress inetSoc = new InetSocketAddress(ip, port);
		addFriend(inetSoc);
	}
	
	public boolean removeFriend(String id){
		for(SocketPeer sPeer : peers){
			if (sPeer.getID().equals(id)){
				if(peers.remove(sPeer)){
					log.debug("Removed SocketPeer by id [" + id + "]");
					return true;
				}
			}
		}
		log.debug("No SocketPeers found by id [" + id + "], nothing removed");
		return false;
	}
	
	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationLayer#findPeerByID(java.lang.String)
	 */
	public SocketPeer findPeerByID(String sID) throws CommunicationFailedException {
		for(SocketPeer peer: peers)
		{
			if(sID.equals(peer.getID())) return peer;
		}
		return null;
	}

	public SocketPeer getLocalPeer() {
		return localPeer;
	}

	public String getID()
	{
		return SOCKET_LAYER_ID;
	}
	
	private class SocketListener implements Runnable
	{
		/*
		 * This socket listenes for incoming connections from other peers.
		 */
		private ServerSocket serverSocket;
		
		SocketListener(int port) throws CommunicationInitException
		{
			try
			{
				serverSocket = new ServerSocket(port);
			}
			catch (IOException e)
			{
				throw new CommunicationInitException("Could not create server socket! ", e);
			}
		}
		
		public SocketListener(InetSocketAddress inetSoc) throws CommunicationInitException {
			try {
				serverSocket = new ServerSocket();
				serverSocket.bind(inetSoc);
			} catch (IOException e) {
				log.error("Unable to bind ServerSocket to  local [" + inetSoc.getAddress().getHostAddress() + ":" + inetSoc.getPort() + "]" , e);
				throw new CommunicationInitException("Could not create server socket! ", e);
			}
			
		}
		
		public void run()
		{
			ActivityEvent event;
			try {
				event = new ActivityEvent(SocketCommunicationProvider.this, 
						ActivityEvent.Type.STARTED, "Listening to incoming connections");
				ActivityManager.getDefault().emitEvent(event);
				
				//
				acceptConnections();
				
				event = new ActivityEvent(SocketCommunicationProvider.this, 
						ActivityEvent.Type.FINISHED, "Finished listening to incoming connections");
				ActivityManager.getDefault().emitEvent(event);
			} catch (RuntimeException e) {
				event = new ActivityEvent(SocketCommunicationProvider.this, 
						ActivityEvent.Type.FAILED, e.toString());
				ActivityManager.getDefault().emitEvent(event);
				throw e;
			}
		}

		private void acceptConnections() {
			while(true) {
				try {
					// wait while someone tries to connect
					Socket socket = serverSocket.accept();
					ObjectInput oi = new CustomObjectInputStream(socket.getInputStream());
					
					// the first message has to be the ID of remote peer
					String remoteID = (String)oi.readObject();
					//TODO: inform Core that new peer is found
					// and accept the connection even if the peer is not in the peer list!
					log.debug("\t\tSearching for peer by ID '"+remoteID+"'");
					SocketPeer peer = (SocketPeer) findPeerByID(remoteID);
					if (peer != null)
					{
						// start listening for messages from the peer
						peer.setOi(oi);
						log.debug("\t\tAccepted remote connection from '"+remoteID+"'");
					}
					else
					{
						oi.close();
						socket.close();
						log.debug("\t\tUID '"+remoteID+"' is unknown. Closed the connection.");
					}
				} catch (Exception e) {
					log.debug("\t\tProblems creating the socket communication: " + e);
					e.printStackTrace();
				}
			}
		}
	}

	public boolean isLocalPeerID(String ID)
	{
		return localPeer.getID().equals(ID);
	}

	public String[] getLocalPeerIDs()
	{
		return new String[]{ localPeer.getID() };
	}

	public void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException {
		log.debug("Sending Message to SocketPeer [" + destinationPeer.toString() + "]");
		for(SocketPeer sPeer : peers){
			if(sPeer.getID().equals(destinationPeer.toString())){
				log.debug("Found destination SocketPeer by id [" + destinationPeer.toString() + "]");
				sPeer.sendMessage(message);
				log.debug("Sent message to SocketPeer [" + destinationPeer.toString() + "]");
			}
		}
		log.debug("No SocketPeers found by id [" + destinationPeer.toString() + "], nothing send");
	}

	public String getActivityName() {
		return "Socket communication";
	}

	public Activity getParentActivity() {
		return null;
	}
}
