package ee.ut.f2f.comm.udp;

import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FMessageListener;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.stun.LocalStunInfo;
import ee.ut.f2f.util.stun.StunInfo;

public class UDPTester extends Thread implements Activity, F2FMessageListener
{
	final private static Logger log = Logger.getLogger(UDPTester.class);
	final private static int MAX_UDP_CONNECTIONS = 1;
	final private static int MAX_BINDING_ERRORS = 20;
	final private static int DEFAULT_WAITING_TIMEOUT = 600;
	
	private F2FPeer remotePeer = null;
	
	private enum Status
	{
		INIT,
		WAITING_STUN_INFO,
		GOT_STUN_INFO,
		INIT_UDP_TESTS,
		STOPPING
		
	}
	private Status status = null;
	
	public UDPTester(F2FPeer peer)
	{
		super("UDPTester [" + peer.getDisplayName() + "]");
		remotePeer = peer;
		status = Status.INIT;
	}
	
	private StunInfo remoteStunInfo = null;
	private StunInfo localStunInfo = LocalStunInfo.getInstance().getStunInfo();
	private Map<UUID,UDPConnection> udpConnections = new HashMap<UUID,UDPConnection>();
	private UDPConnection runningTest = null;
	
	public StunInfo getRemoteStunInfo(){
		return remoteStunInfo;
	}
	
	public int getUdpConnections(){
		return udpConnections.size();
	}
	
	public Status getStatus(){
		return this.status;
	}
	
	public UDPConnection getRunningTest(){
		return this.runningTest;
	}
	
	public void setRunningTest(UDPConnection udpTest){
		this.runningTest = udpTest;
	}
	
	public void resetRunningTest(){
		this.runningTest = null;
	}
	
	public void addConnection(UDPConnection udpConnection){
		if (udpConnection == null) throw new NullPointerException("udpConnection == null");
		this.udpConnections.put(udpConnection.getConnectionId(),udpConnection);
        // if the first connection is made notify the Core about it
        // it means UDP connection can be used!
        //TODO
	}
	
	public void removeConnection(UDPConnection udpConnection){
		if (udpConnection == null) throw new NullPointerException("udpConnection == null");
		this.udpConnections.remove(udpConnection.getConnectionId());
        // if the last connection is removed notify the Core about it
        // it means UDP connection can not be used!
        //TODO
	}
	
	public void sendUDPTestMessage(UDPTestMessage udpTestMessage) throws CommunicationFailedException{
		remotePeer.sendMessage(udpTestMessage);
	}
	
	public void messageReceived(Object message, F2FPeer sender)
	{
		if (sender.equals(remotePeer))
		{
			if (message instanceof UDPTestMessage)
				receivedUDPTestMessage((UDPTestMessage)message);
			else
				log.warn("UDPTester.messageRecieved() handles only UDPTestMessage");
		}
	}
	
	private void receivedUDPTestMessage(UDPTestMessage msg)
	{
		if (status == Status.INIT)
		{
			if (msg.type == UDPTestMessage.Type.INIT);
				status = Status.WAITING_STUN_INFO;
		}
		else if (status == Status.WAITING_STUN_INFO)
		{
			if (msg.type == UDPTestMessage.Type.STUN_INFO)
			{
				remoteStunInfo = msg.stunInfo; 
				status = Status.GOT_STUN_INFO;
			}
		} else if (this.status == Status.INIT_UDP_TESTS) {
			if(msg.type == UDPTestMessage.Type.MAPPED_ADDRESS) {
				if (runningTest != null) runningTest.receivedUDPTestMessage(msg);
			} else if (msg.type == UDPTestMessage.Type.CONNECTION_ID) {
				this.runningTest.setConnectionId(msg.id);
				log.debug("NEW UDP Connection Established ID ["
							+ this.runningTest.getConnectionId().toString()
							+ "]");
			} else if (msg.type == UDPTestMessage.Type.RECEIVED_PING) {
				runningTest.receivedUDPTestMessage(msg);
			} else {
				log.error(" " + getName() + " Illegal message type [" + msg.type.toString() + "] at this moment"
							+ " [" + this.status + "]");
			}
		} else {	
			log.error(" " + getName() + " Illegal message type [" + msg.type.toString() + "] at this moment"
					+ " [" + this.status + "]");
		}
	}
	
	public void stopTesting(){
		log.info("Received stop signal, closing all testing and established connections");
		this.status = Status.STOPPING;
		if (runningTest != null) runningTest.close();
		for (UDPConnection udpc : this.udpConnections.values()){
			udpc.close();
		}
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FINISHED));
		log.debug(getActivityName() + "test process stopped");
	}
	
	public void run()
	{
		// do not start UDP tests before UDPComm providers are initialized
		if (!UDPCommInitiator.isInitialized()) return;
		
		F2FComputing.addMessageListener(UDPTestMessage.class, this);
		// just for information catch any exceptions that may occur
		try
		{
			testProcess();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.warn(getActivityName() + e.getMessage());
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, e.getMessage()));
		}
		//TODO: F2FComputing.removeMessageListener(UDPTestMessage.class, this);
	}
	
	private void testProcess() throws CommunicationFailedException
	{
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		log.debug(getActivityName() + "started");
		
		// make sure that other peer has started the test too
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.CHANGED, "waiting for init"));
		Thread thread = new Thread()
		{
			public void run()
			{
				for (int i = 0; i < DEFAULT_WAITING_TIMEOUT; i++)
				{
					try {
						remotePeer.sendMessage(new UDPTestMessage());
						if (status != Status.INIT) return;
						Thread.sleep(1000);
					} catch (Exception e) {}
				}
			}
		};
		thread.start();
		while (true)
		{
			try {
				thread.join();
				break;
			} catch (InterruptedException e) {}
		}
		if (status == Status.INIT)
		{
			log.error("timeout while waiting for init from remote UDP test thread");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, "timeout while waiting for init from remote UDP test thread"));
			// stop INIT sender
			status = null;
			return;
		}
		
		// exchange the STUN info
		remotePeer.sendMessage(
			new UDPTestMessage(localStunInfo));
		// wait at most 30 seconds for remote addresses
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.CHANGED, "waiting for addresses"));
		for (int i = 0; i < DEFAULT_WAITING_TIMEOUT; i++)
		{
			if (status == Status.GOT_STUN_INFO) break;
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}
		if (status != Status.GOT_STUN_INFO)
		{
			log.error("timeout while waiting for remote STUN info");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, "timeout while waiting for remote STUN info"));
			return;
		}
		if (remoteStunInfo == null)
		{
			log.error("remoteStunInfo == null");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED, "remoteStunInfo == null"));
			return;
		}
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.CHANGED, "got remote STUN info"));
		log.debug("got remote STUN info: " + remoteStunInfo);
		
		//now remote and local STUN info is known -> try to establish UDP connection!
		log.debug("Local :\n"
					+ "\tisOpenAccess : " + localStunInfo.isOpenAccess() + "\n"
					+ "\tisBlockedUDP : " + localStunInfo.isBlockedUDP() + "\n"
					+ "\tisSymmetricUDPFirewall : " + localStunInfo.isSymmetricUDPFirewall() + "\n"
					+ "\tisFullCone() : " + localStunInfo.isFullCone() + "\n"
					+ "\tisRestrictedCone : " + localStunInfo.isRestrictedCone() + "\n"
					+ "\tisPortRestrictedCone : " + localStunInfo.isPortRestrictedCone() + "\n"
					+ "\tisSymmetricCone : " + localStunInfo.isSymmetricCone() + "\n");
		log.debug("Remote :\n"
				+ "\tisOpenAccess : " + remoteStunInfo.isOpenAccess() + "\n"
				+ "\tisBlockedUDP : " + remoteStunInfo.isBlockedUDP() + "\n"
				+ "\tisSymmetricUDPFirewall : " + remoteStunInfo.isSymmetricUDPFirewall() + "\n"
				+ "\tisFullCone() : " + remoteStunInfo.isFullCone() + "\n"
				+ "\tisRestrictedCone : " + remoteStunInfo.isRestrictedCone() + "\n"
				+ "\tisPortRestrictedCone : " + remoteStunInfo.isPortRestrictedCone() + "\n"
				+ "\tisSymmetricCone : " + remoteStunInfo.isSymmetricCone() + "\n");
		//decide which test should be run
		//if one of the sides has blocked UDP
		if(remoteStunInfo.isBlockedUDP() || localStunInfo.isBlockedUDP()){
			//UDP communication is impossible -> UDP blocked on one of the sides
			log.warn("UDP communication impossible, stopping test thread");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(
					this,ActivityEvent.Type.FINISHED, "UDP Communication impossible"));
			return;
		}
		
		//if one of the sides has open access
		/*if(remoteStunInfo.isOpenAccess() || localStunInfo.isOpenAccess()){
			//UDP communication not needed -> At least one of the sides has open access 
			//TCP communication should be possible
			log.warn("UDP communication not needed -> At least one of the sides has open access - > " 
					+ "TCP communication should be possible, stopping test thread");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(
					this,ActivityEvent.Type.FINISHED, "UDP Communication not needed"));
			return;
		}*/
		
		initUDPTests();
	
	}

	public String getActivityName()
	{
		return getName();
	}
	public Activity getParentActivity()
	{
		return null;
	}
	
	private void initUDPTests() throws CommunicationFailedException{
		//if (localIp == null ) throw new NullPointerException("localIp == null");
		//if ("".equals(localIp)) throw new IllegalArgumentException("localIp == \"\"");
		
		log.debug("Initiating UDP Tests");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(
				this,ActivityEvent.Type.CHANGED, 
				"Initiating UDP Tests"));
		
		this.status = Status.INIT_UDP_TESTS;
		
		InetAddress localIas = null;
		InetAddress remoteIas = null;
		try {
			localIas = InetAddress.getByName(localStunInfo.getLocalIp());
			remoteIas = InetAddress.getByName(remoteStunInfo.getPublicIp());
		} catch (UnknownHostException e1) {
			log.error("Unable to resolve InetAddress",e1);
		}
		if (localIas == null){
			log.error("Local InetAddress is not resolved");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(
					this,ActivityEvent.Type.FAILED, 
					"Local InetAddress is not resolved"));
			return;
		}
		if (remoteIas == null){
			log.error("Remote InetAddress is not resolved");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(
					this,ActivityEvent.Type.FAILED, 
					"Remote InetAddress is not resolved"));
			return;
		}
		
		while(udpConnections.size() < MAX_UDP_CONNECTIONS && this.status != Status.STOPPING){
			DatagramSocket ds = null;
			for (int j = 0; j < MAX_BINDING_ERRORS; j++) {
				try {
					int p = 49152 + (int) Math.round(Math.random()*16383);
					ds = new DatagramSocket(new InetSocketAddress(localIas, p));
					if (ds != null && ds.isBound()){
						log.debug("DatagrammSocket is bound on localAddress ["
								   + ds.getLocalAddress().getHostAddress()
								   + ":"
								   + ds.getLocalPort()
								   + "]");
						break;
					}
				} catch (SocketException e) {
					log.error(
						"Unable to bind DatagramSocket on localAddress ["
								+ ds.getLocalAddress().getHostAddress()
								+ ":" 
								+ ds.getLocalPort() + "]", e);
					//destruct DatagramSocket
					ds = null;
				}//catch
			}//for -bindings
			if (ds == null || !ds.isBound()){
				log.error("Timeout binding DatagramSocket on localIp [" 
						   + localIas.getHostAddress() + "]");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(
						this,ActivityEvent.Type.FAILED, 
						   "Timeout binding DatagramSocket on localIp [" 
						   + localIas.getHostAddress() + "]"));
				return;
			}
			//try to set reuse=true address on socket
			try{
				ds.setReuseAddress(true);
				if (!ds.getReuseAddress()){
					throw new SocketException("reuseAddress==false");
				}
			} catch (SocketException e){
				log.error("Unable to set reuse address on bound socket [" 
						   + ds.getLocalAddress().getHostAddress()
						   + ":"
						   + ds.getLocalPort()+ "]",e);
				ActivityManager.getDefault().emitEvent(new ActivityEvent(
						this,ActivityEvent.Type.FAILED, 
						   "Unable to set reuse address on bound socket [" 
						   + ds.getLocalAddress().getHostAddress()
						   + ":"
						   + ds.getLocalPort()+ "]"));
				return;
			}
			
			//try to start UDP test
			try{
				this.runningTest = new UDPConnection(ds, // localSocket
								   remoteIas, // remote IP
								   		this  // parent Thread
				);
				runningTest.start();
				while(true){
					if (this.runningTest == null){
						break;
					}
					try { 
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				}
			} catch (NoSuchAlgorithmException e){
				log.error("Unable to start UDP test ["
							+ ds.getLocalAddress().getHostAddress()
							+ ":"
							+ ds.getLocalPort()
							+ "] -> ["
							+ remoteIas.getHostAddress()
							+ "]"
							,e);
				ActivityManager.getDefault().emitEvent(new ActivityEvent(
						this,ActivityEvent.Type.FAILED, 
							"Unable to start UDP test ["
								+ ds.getLocalAddress().getHostAddress()
								+ ":"
								+ ds.getLocalPort()
								+ "] -> ["
								+ remoteIas.getHostAddress()
								+ "]"));
				return;
			}
		}//for -udpConnections
	}
}

class UDPTestMessage implements Serializable
{
	private static final long serialVersionUID = 8503336434324780827L;
	
	enum Type
	{
		INIT,
		STUN_INFO,
		MAPPED_ADDRESS,
		CONNECTION_ID,
		RECEIVED_PING
	}
	
	Type type = null;
	UDPTestMessage()
	{
		type = Type.INIT;
	}
	
	UDPTestMessage(Type type){
		this.type = type;
	}
	
	StunInfo stunInfo = null;
	UUID id = null;
	InetSocketAddress mappedAddress = null;
	Integer portMappingRule = null;
	
	UDPTestMessage(UUID id){
		this.id = id;
		this.type = Type.CONNECTION_ID;
	}
	
	UDPTestMessage (InetSocketAddress mAddress, Integer mRule){
		this.mappedAddress = mAddress;
		this.portMappingRule = mRule;
		this.type = Type.MAPPED_ADDRESS;
	}
	
	UDPTestMessage(InetSocketAddress mAddress){
		this.mappedAddress = mAddress;
		this.type = Type.MAPPED_ADDRESS;
	}
	
	UDPTestMessage(StunInfo stunInfo)
	{
		type = Type.STUN_INFO;
		this.stunInfo = stunInfo;
	}
	
	public String toString()
	{
		String s = "UDPTestMessage [" + type.toString() + "]";
		return s;
	}
}
