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
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;

public class TCPTester extends Thread implements Activity{
	
	final private static Logger log = Logger.getLogger(TCPTester.class);
	
	//statuses
	final private static int THREAD_INIT = 29;
	final private static int THREAD_STARTED = 30;
	final private static int CONNECTION_FAILURE = 31;
	final private static int AWAITING_ORDERS = 32;
	final private static int TRYING_TO_CONNECT = 33;
	final private static int LISTENING_CONNECTIONS = 34;
	final private static int UNABLE_TO_CONNECT = 35;
	final private static int CONNECTED = 36;
	final private static int ACCEPTED_CONNECTION = 37;
	final private static int THREAD_STOPPED = 0;
	
	private int status = -1;
	private int connectTo = F2FPeer.DEFAULT_SOCKET_COMMUNICATION_PORT;
	private SocketCommunicationProvider scp = null;
	private F2FPeer remotePeer = null;
	
	public TCPTester(F2FPeer peer)
	{
		super("TCPTester [" + peer.getID() + "]");
		remotePeer = peer;
		status = THREAD_INIT;
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
		if(status == AWAITING_ORDERS)
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
		if (status == LISTENING_CONNECTIONS)
		{
			this.interrupt();
		}
		else if (status == ACCEPTED_CONNECTION || status == CONNECTED)
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
		status = THREAD_STARTED;
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		log.debug(getActivityName() + "started");
		
		// check local STUN info
		if(F2FComputing.getLocalPeer().getSTUNInfo() == null)
		{
			log.error(this + " local STUN info is null, stopping TCPTester");
			status = THREAD_STOPPED;
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
			return;
		}
		
		status = AWAITING_ORDERS;
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
			status = TRYING_TO_CONNECT;
			log.debug(getActivityName() + " received command tryConnectTo [" + remotePeer.getSTUNInfo().getLocalIp() + ":" + connectTo + "]");
			
			//scp.removeFriend(stunInfo.getId());
			//scp.addFriend(stunInfo.getLocalIp(), connectTo);
			try
			{
				scp.sendMessage(remotePeer.getID(), new F2FMessage(F2FMessage.Type.TCP,null,null,null,null));
				status = CONNECTED;
				log.debug(getActivityName() + " Connected To [" + remotePeer.getSTUNInfo().getLocalIp() + ":" + connectTo + "]");
			}
			catch (CommunicationFailedException ex)
			{
				log.error(getActivityName() + " unable to Connect To [" + remotePeer.getSTUNInfo().getLocalIp() + ":" + connectTo + "]", ex);
				status = UNABLE_TO_CONNECT;
			}
		}
		
		if (status == CONNECTED)
		{
			ping(1);
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
		}
		else
		{
			if (status == AWAITING_ORDERS) 
				log.debug(getActivityName() + " AWAITING_ORDERS timeout, send tryConnectTo command ");
			if (status == UNABLE_TO_CONNECT) 
				log.debug(getActivityName() + " UNABLE_TO_CONNECT, sending tryConnectTo command ");
			
			status = LISTENING_CONNECTIONS;
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
				status = THREAD_STOPPED;
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
				return;
			}

			status = LISTENING_CONNECTIONS;
			try
			{
				Thread.sleep(120 * 1000);
			}
			catch (InterruptedException ex)
			{
				status = ACCEPTED_CONNECTION;
				log.debug(getActivityName() + " accepted connection from peer [" + remotePeer.getID()  + "]");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
				return;
			}
			
			log.debug(getActivityName() + " LISTENING_CONNECTIONS, no connections");
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
		
	public String getPeerId(){
		return remotePeer.getID().toString();
	}
	
	public int getStatus(){
		return status;
	}
	
	public String getActivityName() {
		return this.getName() + " thread ";
	}

	public Activity getParentActivity() {
		return null;
	}
	
	public String toString(){
		return getActivityName() + "status [" + toString(this.status) + "]";
	}
	
	private String toString(int code){
		switch(status) {
		case THREAD_STARTED : return "Thread started";
		case THREAD_STOPPED : return "Thread stopped";
		case CONNECTION_FAILURE : return "Connection Failure";
		case TRYING_TO_CONNECT : return "Trying connect to";
		case LISTENING_CONNECTIONS : return "Listening Connections";
		case CONNECTED : return "Connected";
		case ACCEPTED_CONNECTION : return "Accepted connection";
		case UNABLE_TO_CONNECT : return "Unable to connect";
		default : return "Unknown: " + status;
	}
	}
}
