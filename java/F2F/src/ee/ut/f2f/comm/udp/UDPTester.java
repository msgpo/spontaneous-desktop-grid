package ee.ut.f2f.comm.udp;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.attribute.MessageAttributeInterface.MessageAttributeType;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.header.MessageHeaderInterface.MessageHeaderType;
import de.javawi.jstun.util.UtilityException;

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
	
	//SO Timeouts
	private final static int RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT = 1000;
	private final static int HOLE_PUNCHING_SO_TIMEOUT = 10000;
		
	//
	private final static int DEFAULT_PORT_MAPPING_RULE = 1;  
	
	private F2FPeer remotePeer = null;
	F2FPeer getRemotePeer() { return remotePeer; }
	
	private enum Status
	{
		INIT,
		WAITING_STUN_INFO,
		GOT_STUN_INFO,
		GOT_MAPPED_ADDRESS,
		CONNECTION_ESTABLISHED,
		CLOSING
	}
	private Status status = null;
	
	public UDPTester(F2FPeer peer)
	{
		super("UDPTester [" + peer.getDisplayName() + "]");
		remotePeer = peer;
		setStatus(Status.INIT);
	}
	
	private StunInfo remoteStunInfo = null;
	private StunInfo localStunInfo = LocalStunInfo.getInstance().getStunInfo();
			
	private Status setStatus(Status status)
	{
		log.info("UDPTester set status " + status);
		return this.status = status;
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
				setStatus(Status.WAITING_STUN_INFO);
		}
		else if (status == Status.WAITING_STUN_INFO)
		{
			if (msg.type == UDPTestMessage.Type.STUN_INFO)
			{
				remoteStunInfo = msg.stunInfo; 
				setStatus(Status.GOT_STUN_INFO);
			}
		}
		else if (this.status == Status.GOT_STUN_INFO)
		{
			if(msg.type == UDPTestMessage.Type.MAPPED_ADDRESS)
            {
                if (msg.mappedAddress == null) return;
				this.remoteMappedAddress = msg.mappedAddress;
				this.remotePortMappingRule = msg.portMappingRule;
				setStatus(Status.GOT_MAPPED_ADDRESS);
			}
            else
            {
				log.warn("Illegal message type at this moment [" + msg.type + "]"
							+ " status [" + this.status + "]");
			}
		}
        else if (this.status == Status.GOT_MAPPED_ADDRESS)
        {
			if (msg.type == UDPTestMessage.Type.RECEIVED_PING)
            {
				setStatus(Status.CONNECTION_ESTABLISHED);
			}
            else if (msg.type == UDPTestMessage.Type.MAPPED_ADDRESS)
            {
				this.remoteMappedAddress = msg.mappedAddress;
			}
            else
            {
				log.warn(" " + getName() 
							 + " Illegal message type at this moment [" 
							 + msg.type + "]"
							 + " status [" + this.status + "]");
			}
        }
        else if (this.status == Status.CONNECTION_ESTABLISHED)
        {
        	/*if (udpm.type == UDPTestMessage.Type.CONNECTION_ID)
            {
        		this.remoteConnectionId = udpm.id;
        	}*/
 		}
        else
        {
			log.warn(" " + getName() 
						 + " Illegal message type at this moment [" 
						 + msg.type + "]"
						 + " status [" + this.status + "]");
		}
	}
	
	public void stopTesting()
	{
		log.info("Received stop signal, closing all testing and established connections");
		setStatus(Status.CLOSING);
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
			setStatus(null);
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
		
		if (this.status != Status.CONNECTION_ESTABLISHED)
        {
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FAILED,
					"UDP test failed"));
			log.warn(" Test Failed");
        }
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.FINISHED));
	
	}

	public String getActivityName()
	{
		return getName();
	}
	public Activity getParentActivity()
	{
		return null;
	}

	Integer remotePortMappingRule = null;
	InetSocketAddress remoteMappedAddress = null;
	private void initUDPTests() throws CommunicationFailedException{
		//if (localIp == null ) throw new NullPointerException("localIp == null");
		//if ("".equals(localIp)) throw new IllegalArgumentException("localIp == \"\"");
		
		log.debug("Initiating UDP Tests");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(
				this,ActivityEvent.Type.CHANGED, 
				"Initiating UDP Tests"));
		
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
		
		int createdConnections = 0;
		while(createdConnections < MAX_UDP_CONNECTIONS && this.status != Status.CLOSING)
		{
			setStatus(Status.GOT_STUN_INFO);
			DatagramSocket localSocket = null;
			Integer localPortMappingRule = null;
			remotePortMappingRule = null;
			InetSocketAddress localMappedAddress = null;
			remoteMappedAddress = null;
			for (int j = 0; j < MAX_BINDING_ERRORS; j++) {
				try {
					int p = 49152 + (int) Math.round(Math.random()*16383);
					localSocket = new DatagramSocket(new InetSocketAddress(localIas, p));
					if (localSocket != null && localSocket.isBound()){
						log.debug("DatagrammSocket is bound on localAddress ["
								   + localSocket.getLocalAddress().getHostAddress()
								   + ":"
								   + localSocket.getLocalPort()
								   + "]");
						break;
					}
				} catch (SocketException e) {
					log.error(
						"Unable to bind DatagramSocket on localAddress ["
								+ localSocket.getLocalAddress().getHostAddress()
								+ ":" 
								+ localSocket.getLocalPort() + "]", e);
					//destruct DatagramSocket
					localSocket = null;
				}//catch
			}//for -bindings
			if (localSocket == null || !localSocket.isBound()){
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
				localSocket.setReuseAddress(true);
				if (!localSocket.getReuseAddress()){
					throw new SocketException("reuseAddress==false");
				}
			} catch (SocketException e){
				log.error("Unable to set reuse address on bound socket [" 
						   + localSocket.getLocalAddress().getHostAddress()
						   + ":"
						   + localSocket.getLocalPort()+ "]",e);
				ActivityManager.getDefault().emitEvent(new ActivityEvent(
						this,ActivityEvent.Type.FAILED, 
						   "Unable to set reuse address on bound socket [" 
						   + localSocket.getLocalAddress().getHostAddress()
						   + ":"
						   + localSocket.getLocalPort()+ "]"));
				return;
			}
			
			
			// if behind Symmetric firewall try to guess the allocated port
			if (LocalStunInfo.getInstance().getStunInfo().isSymmetricCone()){
				while(LocalStunInfo.getInstance().getStunServers(
						localSocket.getLocalAddress()).size() < 4 ){
					try{
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				}
				List<Integer> rules = new ArrayList<Integer>();
				for (int i = 0; i < 5; i++){
					try{
						int rule = portMappingRuleDiscovery(localSocket);
						rules.add(new Integer(rule));
					} catch (PortMappingRuleDiscoveryException e) {
						log.warn("Unable to Discover Port Mapping rule", e);
					}
				}
				log.debug("Discovered Port Allocation Rules :[" + rules + "]");
				localPortMappingRule = getMostFrequentElem(rules);
				if (localPortMappingRule == null){
					localPortMappingRule = DEFAULT_PORT_MAPPING_RULE;
				}
				log.debug("Using discovered Rule [" + localPortMappingRule + "]");
			} else {
				localPortMappingRule = 0;
			}
			// discover mapped IP address and port
			InetSocketAddress ias = null;
			try{
				ias = resolveMappedAddress(localSocket);
			} catch (MappedAddressResolvingException e) {
				log.error("Unable to resolve Mapped Address",e);
			}
			localMappedAddress = new InetSocketAddress(ias.getAddress(),ias.getPort() 
										+ localPortMappingRule);
			
			if( localMappedAddress == null ){
				log.error("Mapped address is not resolved");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
						ActivityEvent.Type.FAILED,
						"Mapped address is not resolved"));
				return;
			}
			
			log.info(getActivityName() 
					   + "Mapped address\t["
					   + localMappedAddress.getAddress().getHostAddress()
					   + ":"
					   + localMappedAddress.getPort()
					   + "]");
			//exchange MappedAddress
			exchangeMappedAddress(localMappedAddress, localPortMappingRule);
			
			if( remoteMappedAddress == null ){
				log.error("RemoteMappedAddress == null");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
						ActivityEvent.Type.FAILED,
						"RemoteMappedAddress == null"));
				return;
			}
			log.debug("RemoteMappedAddress [" 
						+ remoteMappedAddress.getAddress().getHostAddress()
						+ ":"
						+ remoteMappedAddress.getPort()
						+ "]");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.CHANGED,
					"got remoteMappedAddress"));
			
			//start Hole Punching
			punchHole(localSocket);
			if (this.status != Status.CONNECTION_ESTABLISHED)
	        {
				setStatus(Status.CLOSING);
			}
	        else
	        {
				//connection established
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
						ActivityEvent.Type.CHANGED,
						"connection established"));
	        	new Thread(new UDPConnection(localSocket, remoteMappedAddress, remotePeer)).start();
	        	createdConnections++;
			}
		}//for -udpConnections
	}
	
	private int portMappingRuleDiscovery(DatagramSocket localSocket) throws PortMappingRuleDiscoveryException{
		log.debug(getActivityName() 
				+ " "
				+ "Port Mapping Rule Discovery");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Port Mapping Rule Discovery"));
		
		Collection<InetSocketAddress> stunServers = Collections.synchronizedCollection(LocalStunInfo.getInstance().getStunServers(localSocket.getLocalAddress()));
		//Collections.shuffle(stunServers);
		
		Integer rule = null;
		int previousMappedPort = -1;
		
		DatagramSocket socket = null;
		try{
			socket = new DatagramSocket(new InetSocketAddress(localSocket.getLocalAddress(),0));
			if (socket != null && socket.isBound()){
				socket.setReuseAddress(true);
			}
		} catch (SocketException e){
			log.error("Unable to bind DatagramSocket on localIp [" 
							+ socket.getLocalAddress().getHostAddress()
							+ "]");
		}
		if( socket == null ){
			throw new PortMappingRuleDiscoveryException("Unable to bind DatagramSocket on localIp [" 
					+ socket.getLocalAddress().getHostAddress()
					+ "]");
		}
		
		for(InetSocketAddress stunServer : stunServers){
			try {
				log.debug("RESOLVE MAPPED ADDRESS >>>>>>>");
				InetSocketAddress mappedAddress = resolveMappedAddress(socket, stunServer);
				log.debug("RESOLVE MAPPED ADDRESS <<<<<<<");
				if ( rule == null && previousMappedPort == -1){
					previousMappedPort = mappedAddress.getPort();	
				} else if (rule == null && previousMappedPort > -1){ 
					rule = new Integer(mappedAddress.getPort() - previousMappedPort);
					previousMappedPort = mappedAddress.getPort();
				} else if (rule != null && previousMappedPort > -1){
					/*
					log.debug("Port allocation rule: [MappedPort:"
								+ mappedAddress.getPort()
								+ "] [previous Port:"
								+ previousMappedPort
								+ "] [Rule:"
								+ rule.intValue()
								+ "]");
					*/		
					if (rule.intValue() == (mappedAddress.getPort() - previousMappedPort)){
						socket.disconnect();
						socket.close();
						return rule.intValue();
					} else {
						rule = new Integer(mappedAddress.getPort() - previousMappedPort);
						previousMappedPort = mappedAddress.getPort();
					}
				}
				log.debug("Port allocation rule: ["
						+ stunServer.getAddress().getHostAddress()
						+ ":"
						+ stunServer.getPort()
						+ "] <- ["
						+ mappedAddress.getAddress().getHostAddress()
						+ ":"
						+ mappedAddress.getPort()
						+ "] <- ["
						+ socket.getLocalAddress().getHostAddress()
						+ ":"
						+ socket.getLocalPort()
						+ "] rule:"
						+ rule
						+ "]");
			} catch (MappedAddressResolvingException e) {
				continue;
			}
		}
			throw new PortMappingRuleDiscoveryException("Unable to discover Port Mapping rule [" 
					+ socket.getLocalAddress().getHostAddress()
					+ "]");
	}
	
	private InetSocketAddress resolveMappedAddress(DatagramSocket soc, InetSocketAddress stunServer) throws MappedAddressResolvingException{
		byte[] bytes = new byte[0];
		MessageHeader sendMh = null;
		try{
			//create DP content
			sendMh = new MessageHeader(MessageHeaderType.BindingRequest);
			sendMh.generateTransactionID();
			ChangeRequest cr = new ChangeRequest();
			sendMh.addMessageAttribute(cr);
			bytes = sendMh.getBytes();
		} catch (UtilityException e){
			log.error(getActivityName() 
					+ " "
					+ "JSTUN utility exception",e);
			bytes = new byte[0];
			sendMh = null;
		}
		if (bytes.length == 0 || sendMh == null){
			log.error(getActivityName() 
						+ " "
						+ "Unable to create content for DatagramPacket");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FAILED, 
					"Unable to create content for DatagramPacket"));
			throw new MappedAddressResolvingException("Unable to create content for DatagramPacket");
		}
		
		//create DatagramPacket
		DatagramPacket sendPacket = new DatagramPacket(bytes,bytes.length);
		
		try{
			soc.connect(stunServer);
			soc.send(sendPacket);
		} catch (SocketException e){
			log.warn(getActivityName() 
						+ " "
						+"Unable to connect to stun Server ["
						+ stunServer.getAddress().getHostAddress()
						+ ":"
						+ stunServer.getPort()
						+ "] from local Socket ["
						+ soc.getLocalAddress().getHostAddress()
						+ ":"
						+ soc.getLocalPort()
						+ "]",e);
			throw new MappedAddressResolvingException(e);
		} catch (IOException e){
			log.warn(getActivityName()
					+ " "
					+ "Unable to send Packet to StunServer ["
					+ soc.getInetAddress().getHostAddress()
					+ ":"
					+ soc.getPort()
					+ "] from local Socket ["
					+ soc.getLocalAddress().getHostAddress()
					+ ":"
					+ soc.getLocalPort()
					+ "]",e);
			//skip this server
			throw new MappedAddressResolvingException(e);
		}
		
		
		//
		MessageHeader receiveMh = new MessageHeader();
		//listen for incoming packets
		try{	
			DatagramPacket receivePacket = new DatagramPacket(new byte[200], 200);
			//log.debug("STUN SERVER MAPPED ADDRESS REQUEST >>>>>");
			soc.receive(receivePacket);
			//log.debug("STUN SERVER MAPPED ADDRESS REQUEST <<<<<");
			receiveMh = MessageHeader.parseHeader(receivePacket.getData());
		} catch (SocketTimeoutException e){
			log.warn(getActivityName() 
					+ " "
					+ "Socket Timeout waiting answer from ["
					+ soc.getInetAddress().getHostAddress()
					+ ":"
					+ soc.getPort()
					+ "]");
			throw new MappedAddressResolvingException(e);
		} catch (IOException e){
			log.warn(getActivityName() 
					+ " "
					+ "IOException while listening on ["
					+ soc.getInetAddress().getHostAddress()
					+ ":"
					+ soc.getPort()
					+ "]",e);
			throw new MappedAddressResolvingException(e);
		} catch (MessageAttributeParsingException e1) {
			log.warn(getActivityName() 
					+ " " 
					+ "Unable to create content for DatagramPacket");
			throw new MappedAddressResolvingException(e1);
		} catch (MessageHeaderParsingException e2){
			log.warn(getActivityName() 
					+ " " 
					+ "Unable to create content for DatagramPacket");
			throw new MappedAddressResolvingException(e2);
		}
		if(!receiveMh.equalTransactionID(sendMh)){
			log.warn(getActivityName() 
					+ " " 
					+ "Discard wrong packet");
			throw new MappedAddressResolvingException("Discard wrong packet");
		}
		MappedAddress ma = (MappedAddress) receiveMh.getMessageAttribute(MessageAttributeType.MappedAddress);
		if(!(ma == null)){
			InetAddress mappedIas = null;
			int mappedPort = -1;
			try{
				mappedIas = ma.getAddress().getInetAddress();
				mappedPort = ma.getPort();
			} catch (UtilityException e){
				log.error(getActivityName() 
						+ " " 
						+ "JSTUN utility exception",e);
			} catch (UnknownHostException e){
				log.error(getActivityName() 
						+ " " 
						+ e.getMessage());
			}
			if (mappedIas == null || mappedPort <= 0){
				log.error(getActivityName() 
						+ " " 
						+ "Unable to get mapped address from received packet");
				throw new MappedAddressResolvingException("Unable to get mapped address from received packet");
			}
			InetSocketAddress mappedAddress = new InetSocketAddress(mappedIas, mappedPort);
			soc.disconnect();
			return mappedAddress;
		} else {
			log.error(getActivityName() 
					+ " " 
					+ "Unable to get mapped address using stunServer ["
						+ stunServer.getAddress().getHostAddress()
						+ ":"
						+ stunServer.getPort()
						+ "]");
			throw new MappedAddressResolvingException("Unable to get mapped address using stunServer ["
					+ stunServer.getAddress().getHostAddress()
					+ ":"
					+ stunServer.getPort()
					+ "]");
		}
	}
	
	private InetSocketAddress resolveMappedAddress(DatagramSocket soc) throws MappedAddressResolvingException{
		InetSocketAddress mappedAddress = null;
		log.debug(getActivityName() 
				+ " "
				+ "Resolving Mapped Address");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Resolving Mapped Address"));
		
		Collection<InetSocketAddress> stunServers = LocalStunInfo.getInstance().getStunServers(soc.getLocalAddress());
		Collections.shuffle((List<InetSocketAddress>) stunServers);
		//log.info("Got total [" + stunServers.size() + "] STUN server addresses for localIp");
		
		//set socket timeout
		try {
			if (soc.getSoTimeout() != RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT) {
				soc.setSoTimeout(RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT);
			}
		} catch (SocketException e){
			log.error("Unable to set socket timeout to RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT", e);
		}
		
		//loop over stunServers, ask for mapped address
		for(InetSocketAddress stunServer : stunServers){
			mappedAddress = resolveMappedAddress(soc,stunServer);
			if (mappedAddress == null) throw new NullPointerException("mappedAddress == null after resolving");
			break;
		}
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Mapped Address Resolved"));
		return mappedAddress;
	}
	
	private Integer getMostFrequentElem(List<Integer> list){
		Integer mostFrequent = null;
		int count = 0;
		for(Integer i : list){
			int c = Collections.frequency(list, i);
			if(c > count){
				count = c;
				mostFrequent = i;
				if (count > (list.size()/2)){
					break;
				}
			}
		}
		return mostFrequent;
	}
	
	private Integer coneToSymPortRangePing = 0;
	private Integer attackOnRemotePort = 0;
	
	private void punchHole(final DatagramSocket localSocket)
    {
		log.debug(getActivityName() + "Hole Punching");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Hole Punching"));
				
		try {
			if (localSocket.getSoTimeout() != HOLE_PUNCHING_SO_TIMEOUT) {
				localSocket.setSoTimeout(HOLE_PUNCHING_SO_TIMEOUT);
			}
		} catch (SocketException e){
			log.error("Unable to set socket timeout to HOLE_PUNCHING_SO_TIMEOUT", e);
		}
		
		synchronized (attackOnRemotePort) {
			attackOnRemotePort = remoteMappedAddress.getPort();
		}
		synchronized (coneToSymPortRangePing) {
			if (remoteStunInfo.isSymmetricCone() &&
				!LocalStunInfo.getInstance().getStunInfo().isSymmetricCone()) {
				coneToSymPortRangePing = 1;
			}
			//else if ()
		}
		
		final int AFTER_CONNECTION_ESTABLISHED_PING_AMOUNT = 3;
		
		Thread udpListener = new Thread()
        {
			public void run()
            {
				byte[] receiveContent = new byte[UDPPacket.HASH_LENGTH + 1 + 4 + 4];
				while(  status != Status.CLOSING
                        /*UDPConnection.this.status != Status.CONNECTION_ESTABLISHED*/)
                {	
					try
                    {
						DatagramPacket receivePacket = new DatagramPacket(receiveContent,receiveContent.length);
						localSocket.receive(receivePacket);
                        UDPPacket udpp = new UDPPacket(receivePacket.getData());
						if (UDPPacket.PING == udpp.getType())
                        {
							log.debug("Received PING packet from ["
										+ receivePacket.getAddress().getHostAddress()
										+ ":"
										+ receivePacket.getPort()
										+ "]");
							
							if (!(remoteMappedAddress.getAddress().equals(receivePacket.getAddress()) &&
                                  remoteMappedAddress.getPort() == receivePacket.getPort())){	
								//multiple subnetworks case
								//ping received from different remote address
								//change the target IP and port for ping
                                remoteMappedAddress = new InetSocketAddress(receivePacket.getAddress(),
																		receivePacket.getPort());
								//change the currently used target port
								//stop the range attack
								synchronized (attackOnRemotePort ) {
                                    attackOnRemotePort = receivePacket.getPort();
								}
								synchronized (coneToSymPortRangePing) {
                                    coneToSymPortRangePing = 0;
								}
							}
                            
                            // inform the other side that we have received a PING packet
							try 
                            {
								sendUDPTestMessage(new UDPTestMessage(UDPTestMessage.Type.RECEIVED_PING));
								return;
							} catch (CommunicationFailedException e) {}
						} else {
							log.warn("Waited PING, received " + receivePacket);
						}
					} catch (SocketTimeoutException e) {
						/*
						if (LocalStunInfo.getInstance().getStunInfo().isSymmetricCone() &&
								udpTester.getRemoteStunInfo().isSymmetricCone()){
							//
						}
						else if(
								(LocalStunInfo.getInstance().getStunInfo().isSymmetricCone() &&
								!udpTester.getRemoteStunInfo().isSymmetricCone()) 
							||
							 	(!LocalStunInfo.getInstance().getStunInfo().isSymmetricCone() && 
									udpTester.getRemoteStunInfo().isSymmetricCone())
						  ) {
							counter++;
							if (counter == AFTER_CONNECTION_ESTABLISHED_RESEND_AMOUNT){
                                holePunchTimeout = true;
							}
						} else {
							log.warn("Hole punching timeout, no result, stopping thread");
                            holePunchTimeout = true;
						}
						*/
					} catch (Exception e) {
						log.warn("Exception receiving PING packet",e);
					}
				}
				log.debug("HOLE Punching Receive Thread is STOPPING");
			}
		};
		udpListener.start();
		//first try with remote mapping rule
		synchronized (attackOnRemotePort){
			attackOnRemotePort = attackOnRemotePort + remotePortMappingRule;
		}
		long startTime = System.currentTimeMillis();
		for (int pingsAfterTraversal = 0, ping_counter = 0, port_increment_counter = 0; 
			   pingsAfterTraversal < AFTER_CONNECTION_ESTABLISHED_PING_AMOUNT &&
			   status != Status.CLOSING; 
			 ping_counter++)
        {
			if (System.currentTimeMillis() - startTime > 60000)
			{
				setStatus(Status.CLOSING);
				break;
			}
            UDPPacket sendUDPpacket = new UDPPacket(UDPPacket.PING);
			byte[] sendContent = sendUDPpacket.getBytes();
			DatagramPacket sendPacket = null;
			//next if there is cone -> symmetric case
			//try to ping a range of ports on symmetric side 
			int p = attackOnRemotePort + (	coneToSymPortRangePing *
											port_increment_counter *
											((int) Math.pow((-1), port_increment_counter))
										 );
			
			try
            {
				InetSocketAddress ias = new InetSocketAddress(remoteMappedAddress.getAddress(),p);
				sendPacket = new DatagramPacket(sendContent,sendContent.length,ias);
			}
            catch (IllegalArgumentException e)
            {
				if (p > 65535 || p < 1024){
					log.error("Remote Port number out of range [" + p + "]", e);
					setStatus(Status.CLOSING);
					break;
				}
			} catch (SocketException e) {
				log.error(getActivityName() +  " " + e.getMessage(),e);
			}
			if (sendPacket == null) continue;
			
			try
            {
				localSocket.send(sendPacket);
				log.debug("Sent PING packet to "
						   + sendPacket.getAddress().getHostAddress()
						   + ":"
						   + sendPacket.getPort());
			}
            catch (IOException e)
            {
				log.warn("I/O Exception sending PING packet to ["
						   + sendPacket.getAddress().getHostAddress()
						   + " "
						   + sendPacket.getPort()
						   +"]",e);
			}
			
			try
            {
				Thread.sleep(50);
			}
            catch (InterruptedException e) {}
			
			if(status == Status.CONNECTION_ESTABLISHED)
            {
				pingsAfterTraversal++;
				startTime = System.currentTimeMillis();
			}
			if(ping_counter > 3)
            {
				synchronized (attackOnRemotePort){
					attackOnRemotePort = p;
				}
				port_increment_counter++;
				ping_counter = 5;
			}
		}
        try
        {
            udpListener.join();
        } catch (InterruptedException e){}
        log.debug("Hole Punching Method is STOPPING");
	}
	
	private void exchangeMappedAddress(InetSocketAddress localMappedAddress, Integer localPortMappingRule) throws CommunicationFailedException{
		log.debug(getActivityName() + "Mapped Address Exchange");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Mapped Address Exchange"));
		//exchange mapped addresses
		for(int i = 0; i < DEFAULT_WAITING_TIMEOUT; i++){
			try{
                sendUDPTestMessage(new UDPTestMessage(localMappedAddress, localPortMappingRule));
				if (remoteMappedAddress != null) return;
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		if( remoteMappedAddress == null){
			log.error("Timeout while waiting mapped address");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FAILED,
					"Timeout while waiting mapped address"));
			return;
		}
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
		//CONNECTION_ID,
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
	
	/*UDPTestMessage(UUID id){
		this.id = id;
		this.type = Type.CONNECTION_ID;
	}*/
	
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

@SuppressWarnings("serial")
class PortMappingRuleDiscoveryException extends Exception
{
	PortMappingRuleDiscoveryException(){
		super();
	}
	
	PortMappingRuleDiscoveryException(String message){
		super(message);
	}
	
	PortMappingRuleDiscoveryException(String message, Throwable e){
		super(message,e);
	}
	
	PortMappingRuleDiscoveryException(Throwable e){
		super(e);
	}
}

@SuppressWarnings("serial")
class MappedAddressResolvingException extends Exception
{
	MappedAddressResolvingException() {
		super();
	}
	
	MappedAddressResolvingException(String message){
		super(message);
	}
	
	MappedAddressResolvingException(String message, Throwable e){
		super(message,e);
	}
	
	MappedAddressResolvingException(Throwable e){
	 	super(e);
	}
}
