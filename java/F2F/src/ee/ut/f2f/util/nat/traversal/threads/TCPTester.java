package ee.ut.f2f.util.nat.traversal.threads;

import java.util.Random;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.socket.SocketCommunicationProvider;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.ui.model.StunInfoTableItem;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;

public class TCPTester extends Thread implements Activity{
	
	final private static Logger log = Logger.getLogger(TCPTester.class);
	
	//statuses
	private enum Status
	{
		CONNECTION_FAILURE,
		AWAITING_ORDERS,
		TRYING_TO_CONNECT,
		LISTENING_CONNECTIONS,
		UNABLE_TO_CONNECT,
		CONNECTED,
		ACCEPTED_CONNECTION,
	}
	
	private Status status = null;
	private int connectTo = F2FPeer.DEFAULT_SOCKET_COMMUNICATION_PORT;
	private SocketCommunicationProvider scp = null;
	private F2FPeer remotePeer = null;
	
	/**
	 * Constructs new TCP connection tester thread.
	 * 
	 * @param peer The remote peer where to connect.
	 */
	public TCPTester(F2FPeer peer)
	{
		super("TCPTester [" + peer.getID() + "]");
		remotePeer = peer;
		//scp = F2FComputingGUI.natMessageProcessor.getConnectionManager().getSocketCommunicationProvider();
		//scp.removeFriend(peer.getID());
		//scp.addFriend(peer.getSTUNInfo().getLocalIp(), connectTo);
		/*TODO: remove, this is not the place where this should be done and
		 * comm provider may only be added to a peer via F2FComputing.peerContacted() method
		F2FPeer f2fpeer = F2FComputingGUI.controller.getFriendModel().getF2FPeerById(peerId);
		f2fpeer.removeCommProvider(scp);
		f2fpeer.addCommProvider(scp);
		*/
	}	
		
	public void tryConnectTo(Integer port)
	{
		if(status == Status.AWAITING_ORDERS)
		{
			if(port != null && port.intValue() > 0)
			{
				this.connectTo = port.intValue();
			}
			this.interrupt();
		}
		else
		{
			log.error("tryConnectTo(" + port.intValue() + ") called at illegal status: " + toString());
		}
	}
	
	public void receivedTCPTest(Object obj)
	{
		if (status == Status.LISTENING_CONNECTIONS)
		{
			this.interrupt();
		}
		else if (status == Status.ACCEPTED_CONNECTION || status == Status.CONNECTED)
		{
			int n = ((Integer) obj).intValue();
			
			if(n > 5)
			{
				log.debug(getActivityName() + "TCP is Established");
				//TODO: put this info into StunInfo
				//StunInfoTableItem sinft = (StunInfoTableItem) F2FComputingGUI.controller.getStunInfoTableModel().get(peerId);
				//sinft.setTcpConnectivity(true);
				//F2FComputingGUI.controller.getStunInfoTableModel().update(sinft);
			}
			
			log.debug(getActivityName() + " received ping [" + n + "]");
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {}
			
			ping(n+1);
		}
		else
		{	
			log.error("receivedTCPTest at illegal moment: " + toString());
		}
	}
	
	public void run()
	{
		try
		{
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		log.debug(getActivityName() + "started");
		
		// check local STUN info (the test thread should not start before it is known)
		if(F2FComputing.getLocalPeer().getSTUNInfo() == null)
		{
			status = Status.CONNECTION_FAILURE;
			log.error("local STUN info is null, stopping TCP test thread");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
			return;
		}
		
		// exchange the port number that has to be used
		
		status = Status.AWAITING_ORDERS;
		try
		{
			Random rnd = new Random();
			int n = rnd.nextInt(120);
			log.debug(getActivityName() + " awaiting orders [" + n + "] sec");
			if (n > 0) Thread.sleep(n*1000);
		}
		catch (InterruptedException e)
		{
			// the other peer sent COMMAND_TRY_CONNECT_TO 
			status = Status.TRYING_TO_CONNECT;
			log.debug(getActivityName() + " received command tryConnectTo [" + remotePeer.getSTUNInfo().getLocalIp() + ":" + connectTo + "]");
			
			//scp.removeFriend(stunInfo.getId());
			//scp.addFriend(stunInfo.getLocalIp(), connectTo);
			try
			{
				scp.sendMessage(remotePeer.getID(), new F2FMessage(F2FMessage.Type.TCP,null,null,null,null));
				status = Status.CONNECTED;
				log.debug(getActivityName() + " Connected To [" + remotePeer.getSTUNInfo().getLocalIp() + ":" + connectTo + "]");
			}
			catch (CommunicationFailedException ex)
			{
				log.error(getActivityName() + " unable to Connect To [" + remotePeer.getSTUNInfo().getLocalIp() + ":" + connectTo + "]", ex);
				status = Status.UNABLE_TO_CONNECT;
			}
		}
		
		if (status == Status.CONNECTED)
		{
			ping(1);
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
		}
		else
		{
			if (status == Status.AWAITING_ORDERS) 
				log.debug(getActivityName() + " AWAITING_ORDERS timeout, send tryConnectTo command ");
			if (status == Status.UNABLE_TO_CONNECT) 
				log.debug(getActivityName() + " UNABLE_TO_CONNECT, sending tryConnectTo command ");
			
			status = Status.LISTENING_CONNECTIONS;
			log.debug(getActivityName() + " send COMMAND_TRY_CONNECT_TO");
			F2FMessage msg = 
				new F2FMessage(F2FMessage.Type.TRY_CONNECT_TO,
						null,
						null,
						null,
						connectTo);
			try
			{
				remotePeer.sendMessage(msg);
			}
			catch (CommunicationFailedException e)
			{
				log.debug(this + " IM connection to remote peer was lost");
				status = Status.CONNECTION_FAILURE;
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
				return;
			}

			status = Status.LISTENING_CONNECTIONS;
			try
			{
				Thread.sleep(120 * 1000);
			}
			catch (InterruptedException ex)
			{
				status = Status.ACCEPTED_CONNECTION;
				log.debug(getActivityName() + " accepted connection from peer [" + remotePeer.getID()  + "]");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
				return;
			}
			
			log.debug(getActivityName() + " LISTENING_CONNECTIONS, no connections");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
		}
		}
		catch (Exception e)
		{
			log.error(getActivityName() + " error: " + e.getMessage());
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
		}
	}
	
	private void ping(int n)
	{
		try
		{
			scp.sendMessage(remotePeer.getID(), new F2FMessage(F2FMessage.Type.TCP, null, null, null, new Integer(n)));
			log.debug(getActivityName() + " SENT PING" + n);
		}
		catch (CommunicationFailedException ex)
		{
			log.error(getActivityName() + " unable to send ping, connection failure", ex);
			log.debug(getActivityName() + "TCP unreliable");
			StunInfoTableItem sinft = (StunInfoTableItem) F2FComputingGUI.controller.getStunInfoTableModel().get(remotePeer.getID().toString());
			sinft.setTcpConnectivity(false);
			F2FComputingGUI.controller.getStunInfoTableModel().update(sinft);
		}
	}
	
	public String getActivityName() {
		return this.getName() + " thread ";
	}

	public Activity getParentActivity() {
		return null;
	}
	
	public String toString()
	{
		return getActivityName() + "status [" + toString(status) + "]";
	}
	
	private String toString(Status status)
	{
		switch(status)
		{
		case CONNECTION_FAILURE : return "Connection Failure";
		case TRYING_TO_CONNECT : return "Trying connect to";
		case LISTENING_CONNECTIONS : return "Listening Connections";
		case CONNECTED : return "Connected";
		case ACCEPTED_CONNECTION : return "Accepted connection";
		case UNABLE_TO_CONNECT : return "Unable to connect";
		default : return "Unknown status: " + status;
		}
	}
}
