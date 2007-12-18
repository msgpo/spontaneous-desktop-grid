package ee.ut.f2f.util.nat.traversal.threads;

import java.util.Random;
import java.util.UUID;

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
import ee.ut.f2f.util.nat.traversal.NatMessage;
import ee.ut.f2f.util.nat.traversal.StunInfo;

public class TCPTester extends Thread implements Activity{
	
	final private static Logger log = Logger.getLogger(TCPTester.class);
	
	//statuses
	final private static int THREAD_STARTED = 30;
	final private static int CONNECTION_FAILURE = 31;
	final private static int AWAITING_ORDERS = 32;
	final private static int TRYING_TO_CONNECT = 33;
	final private static int LISTENING_CONNECTIONS = 34;
	final private static int UNABLE_TO_CONNECT = 35;
	final private static int CONNECTED = 36;
	final private static int ACCEPTED_CONNECTION = 37;
	final private static int THREAD_STOPPED = 0;
	
	private String peerId = null;
	private int status = THREAD_STOPPED;
	private int connectTo;
	private SocketCommunicationProvider scp;
	
	public TCPTester (String peerId){
		super("TCPTester [" + peerId + "]");
		if (peerId == null) throw new NullPointerException("Peer id is null");
		this.peerId = peerId;
		this.connectTo = F2FComputingGUI.natMessageProcessor.getConnectionManager().getScPort();
		StunInfo sinf = F2FComputingGUI.controller.getStunInfoTableModel().get(peerId);
		scp = F2FComputingGUI.natMessageProcessor.getConnectionManager().getSocketCommunicationProvider();
		scp.removeFriend(peerId);
		scp.addFriend(sinf.getLocalIp(), connectTo);
		F2FPeer f2fpeer = F2FComputingGUI.controller.getFriendModel().getF2FPeerById(peerId);
		f2fpeer.removeCommProvider(scp);
		f2fpeer.addCommProvider(scp);
	}	
	
	
	
	public void tryConnectTo(Integer port){
		if(status == AWAITING_ORDERS){
			if(port != null && port.intValue() > 0){
				this.connectTo = port.intValue();
			}
			this.interrupt();
		} else {
			log.error("(tryConnectTo(" + port.intValue() + ")) illegal status" + this);
		}
	}
	
	public void receivedTCPTest(Object obj){
		if(status == LISTENING_CONNECTIONS){
			this.interrupt();
		} else if (status == ACCEPTED_CONNECTION || status == CONNECTED) {
			int n = ((Integer) obj).intValue();
			
			if(n > 5){
				log.debug(getActivityName() + "TCP is Established");
				StunInfoTableItem sinft = (StunInfoTableItem) F2FComputingGUI.controller.getStunInfoTableModel().get(peerId);
				sinft.setTcpConnectivity(true);
				F2FComputingGUI.controller.getStunInfoTableModel().update(sinft);
			}
			
			log.debug(getActivityName() + " received ping [" + n + "]");
			try{
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				log.error(getActivityName() + "interrupted sleep", ex);
			}
			ping(n+1);
		} else {	
			log.error("receivedTCPTest(Object obj) illegal status" + this);
		}
	}
	
	public void run(){
		status = THREAD_STARTED;
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		log.debug(getActivityName() + "started");
		//local stunInfo check
		stunInfoCheck();
		if(status == THREAD_STOPPED){
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
			return;
		}
		
		status = AWAITING_ORDERS;
		waitOrders(120);
		if(status == CONNECTED){
			ping(1);
		} else {
			if(status == AWAITING_ORDERS) 
				log.debug(getActivityName() + " awaiting orders timeout, sending tryConnectTo command ");
			if(status == UNABLE_TO_CONNECT) 
				log.debug(getActivityName() + " unable to connect, sending tryConnectTo command ");
			status = LISTENING_CONNECTIONS;
			log.debug(getActivityName() + " listening for incoming connections ");
			NatMessage nmsg = new NatMessage(F2FComputing.getLocalPeer().getID().toString(),
										 peerId,
										 NatMessage.COMMAND_TRY_CONNECT_TO,
										 new Integer(connectTo));
			F2FComputingGUI.natMessageProcessor.sendNatMessage(nmsg);
			listenIncomingTCPConnections(120);
			if(status == LISTENING_CONNECTIONS)
				log.debug(getActivityName() + " listening timeout, no connections, sending tryConnectTo command ");
			if(status == ACCEPTED_CONNECTION){
				log.debug(getActivityName() + " accepted connection from peer [" + peerId  + "]");
				//
			}
		}
		log.debug(getActivityName() + " unable to estblish TCP communication");
	}
	
	private void ping(int n){
		scp = F2FComputingGUI.natMessageProcessor.getConnectionManager().getSocketCommunicationProvider();
				try{
					scp.sendMessage(UUID.fromString(peerId), new F2FMessage(F2FMessage.Type.TCP, null, null, null, new Integer(n)));
					log.debug(getActivityName() + " SENT PING" + n);
				} catch (CommunicationFailedException ex){
					log.error(getActivityName() + " unable to send ping, connection failure", ex);
					log.debug(getActivityName() + "TCP unreliable");
					StunInfoTableItem sinft = (StunInfoTableItem) F2FComputingGUI.controller.getStunInfoTableModel().get(peerId);
					sinft.setTcpConnectivity(false);
					F2FComputingGUI.controller.getStunInfoTableModel().update(sinft);
				}	
	}
	
	private void listenIncomingTCPConnections(int sec){
		status = LISTENING_CONNECTIONS;
		log.debug(getActivityName() + " listening [" + sec + " sec] for incoming TCP connections from [" + peerId + "]");
		try{
			Thread.sleep(sec*1000);
		} catch (InterruptedException ex){
			status = ACCEPTED_CONNECTION;
		}
	}
	
	private void waitOrders(int sec){
		status = AWAITING_ORDERS;
		try{
			Random rnd = new Random();
			int n = rnd.nextInt(sec);
			log.debug(getActivityName() + " awaiting orders [" + n + "] sec");
			if(n > 0){
				Thread.sleep(n*1000);
			}
		} catch (InterruptedException e){
			status = TRYING_TO_CONNECT;
			//
			tryConnectTo();
		}
	}
	
	private void tryConnectTo(){
		StunInfo sinf = F2FComputingGUI.controller.getStunInfoTableModel().get(peerId);
		log.debug(getActivityName() + " received command tryConnectTo [" + sinf.getLocalIp() + ":" + connectTo + "]");
		scp = F2FComputingGUI.natMessageProcessor.getConnectionManager().getSocketCommunicationProvider();
		scp.removeFriend(peerId);
		scp.addFriend(sinf.getLocalIp(), connectTo);
		try{
			scp.sendMessage(UUID.fromString(peerId), new F2FMessage(F2FMessage.Type.TCP,null,null,null,null));
			status = CONNECTED;
			log.debug(getActivityName() + " Connected To [" + sinf.getLocalIp() + ":" + connectTo + "]");
		} catch (CommunicationFailedException ex){
			log.error(getActivityName() + " unable to Connect To [" + sinf.getLocalIp() + ":" + connectTo + "]", ex);
			status = UNABLE_TO_CONNECT;
		}
	}
	
	private void stunInfoCheck(){
		log.debug(getActivityName() + "checking local stun info ...");
		StunInfo sinf = F2FComputingGUI.natMessageProcessor.getConnectionManager().getLocalStunInfo(false);
		if(sinf == null){
			log.warn(this + " local stun info is null, stopping thread");
			status = THREAD_STOPPED;
		}
	}
	
	public String getPeerId(){
		return this.peerId;
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
