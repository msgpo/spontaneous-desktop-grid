package net.ulno.jpunch.comm.udp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import net.ulno.jpunch.core.CommunicationFailedException;
import net.ulno.jpunch.util.Util;
import net.ulno.jpunch.util.logging.Logger;
import net.ulno.jpunch.util.stun.LocalStunInfo;
import net.ulno.jpunch.util.stun.StunInfo;
import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.attribute.MessageAttributeInterface.MessageAttributeType;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.header.MessageHeaderInterface.MessageHeaderType;
import de.javawi.jstun.util.UtilityException;

public class UDPTester extends Thread {
	final private static Logger log = Logger.getLogger(UDPTester.class);
	
	//Default values
	final private static int MAX_BINDING_ERRORS = 20;
	final private static int DEFAULT_WAITING_TIMEOUT = 600;
	private final static int DEFAULT_PORT_MAPPING_RULE = 1;
	private final static int DEFAULT_PING_INTERVAL = 1000;
	
	// SO Timeouts
	private final static int RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT = 1000;
	private final static int HOLE_PUNCHING_SO_TIMEOUT = 10000;
	
	// StunInfo
	private StunInfo remoteStunInfo = null;
	private StunInfo localStunInfo = null;
	
	// Mappings
	private Integer remotePortMappingRule = null;
	private InetSocketAddress remoteMappedAddress = null;
	
	// Receive Queue used for blocking receive
	private BlockingReceiveQueue blockingReceiveQueue = new BlockingReceiveQueue();
	
	//Receiving thread
	private MessageReceivingThread messageReceivingThread = null;
	
	//Established Connection
	private UDPConnection udpConnection = null;
	
	public UDPTester(){
		super(UDPTester.class.getName());
	}
	
	/**
	 * Returns tested UDPConnection
	 * returned UDPConnection is in running state
	 * method blocks until Connection is tested 
	 * @return tested UDPConnection
	 */
	public synchronized UDPConnection getUDPConnection(){
		if (this.udpConnection == null){
			try{
				wait();
			} catch (InterruptedException e) {}
			return udpConnection;
		}
		return udpConnection;
	}
	
	private synchronized void setUdpConnection(UDPConnection udpConnection){
		if (this.udpConnection == null && udpConnection != null){
			this.udpConnection = udpConnection;
			notify();
		}
	}
	
	private synchronized StunInfo getRemoteStunInfo(){
		if(this.remoteStunInfo == null){
			try {
				wait();
			} catch (InterruptedException e) {}
			return this.remoteStunInfo;
		}
		return this.remoteStunInfo;
	}
	
	private synchronized void setRemoteStunInfo(StunInfo stunInfo){
		if(this.remoteStunInfo == null && stunInfo != null && !"".equals(stunInfo)){
			this.remoteStunInfo = stunInfo;
			notify();
		}
	}
	
	private synchronized InetSocketAddress getRemoteMappedAddress(){
		if (this.remoteMappedAddress == null){
			try {
				wait();
			} catch (InterruptedException e) {}
			return this.remoteMappedAddress;
		}
		return this.remoteMappedAddress;
	}
	
	private synchronized void setRemoteMappedAddress(InetSocketAddress remoteMappedAddress){
		if (this.remoteMappedAddress == null && remoteMappedAddress != null){
			this.remoteMappedAddress = remoteMappedAddress;
			notify();
		}
	}
	
	private synchronized Integer getRemotePortMappingRule(){
		if (this.remotePortMappingRule == null){
			try {
				wait();
			} catch (InterruptedException e) {}
			return this.remotePortMappingRule;
		}
		return this.remotePortMappingRule;
	}
	
	private synchronized void setRemotePortMappingRule (Integer remotePortMappingRule){
		if (this.remotePortMappingRule == null && remotePortMappingRule != null){
			this.remotePortMappingRule = remotePortMappingRule;
			notify();
		}
	}

	
	/**
	 * Non-Blocking send.
	 * 
	 * @param udpTestMessage
	 * @throws CommunicationFailedException
	 */
	private void send(UDPTestMessage udpTestMessage) throws CommunicationFailedException{
			try {
				byte[] bytes = Util.serializeObject(udpTestMessage);
				byte[] compressed = Util.zip(bytes);
				String message = Util.encode(compressed);
				System.out.println(message);
			} catch (IOException e){
				throw new CommunicationFailedException("Failed Sending UDP Test Message ["
						+ udpTestMessage.toString() + "]",e);
			}
	}
	
	/**
	 * Handles incoming messages for UDPTester
	 * 
	 * @param message
	 *            incoming message, only UDPTestMessage instances allowed
	 */
	private void messageReceived(Object message) {
		if (message instanceof UDPTestMessage)
			messageReceived((UDPTestMessage) message);
		else log.warn("UDPTester.messageRecieved() handles only UDPTestMessage");
	}

	/*
	 * If UDPTestMessage received try to unblock queue if queue was not blocked
	 * then discard message
	 */
	private void messageReceived(UDPTestMessage msg) {
		if (msg.type == UDPTestMessage.Type.INIT){
			
		} else if (msg.type == UDPTestMessage.Type.STUN_INFO) {
			this.setRemoteStunInfo(msg.stunInfo);
		} else if (msg.type == UDPTestMessage.Type.MAPPED_ADDRESS) {
			this.setRemoteMappedAddress(msg.mappedAddress);
			this.setRemotePortMappingRule(msg.portMappingRule);
		} else if (msg.type == UDPTestMessage.Type.RECEIVED_PING) {
			// stop hole punching sending loop
			remoteSideReceivedPing = true;
			log.debug("Remote Sode Received Ping");
		}
		
	}

	public void run() {
		// just for information catch any exceptions that may occur
		try {
			testProcess();
		} catch (Exception e) {
			e.printStackTrace();
			log.warn(e.getMessage());
		}
	}

	private void testProcess() throws CommunicationFailedException {

		log.debug(getName() + " started");
		
		//start receiving thread
		messageReceivingThread = new MessageReceivingThread();
		messageReceivingThread.start();
		
		//get local stun info
		localStunInfo = LocalStunInfo.getInstance().getStunInfo();
				
		// exchange the STUN info
		 this.send( new UDPTestMessage(localStunInfo));
		 remoteStunInfo = this.getRemoteStunInfo();

		// now remote and local STUN info is known -> try to establish UDP
		// connection!

		log.debug("Local :\n" 
				+ "\tLocalIP [" + localStunInfo.getLocalIp() + "]\n"
				+ "\tPublicIP [" + localStunInfo.getPublicIp() + "]\n"
				+ "\tisOpenAccess : "
				+ localStunInfo.isOpenAccess() + "\n" + "\tisBlockedUDP : "
				+ localStunInfo.isBlockedUDP() + "\n"
				+ "\tisSymmetricUDPFirewall : "
				+ localStunInfo.isSymmetricUDPFirewall() + "\n"
				+ "\tisFullCone() : " + localStunInfo.isFullCone() + "\n"
				+ "\tisRestrictedCone : "
				+ localStunInfo.isRestrictedCone() + "\n"
				+ "\tisPortRestrictedCone : "
				+ localStunInfo.isPortRestrictedCone() + "\n"
				+ "\tisSymmetricCone : " + localStunInfo.isSymmetricCone()
				+ "\n");

		log.debug("Remote :\n" 
				+ "\tLocalIP [" + remoteStunInfo.getLocalIp() + "]\n"
				+ "\tPublicIP [" + remoteStunInfo.getPublicIp() + "]\n"
				+ "\tisOpenAccess : "
				+ remoteStunInfo.isOpenAccess() + "\n"
				+ "\tisBlockedUDP : " + remoteStunInfo.isBlockedUDP()
				+ "\n" + "\tisSymmetricUDPFirewall : "
				+ remoteStunInfo.isSymmetricUDPFirewall() + "\n"
				+ "\tisFullCone() : " + remoteStunInfo.isFullCone() + "\n"
				+ "\tisRestrictedCone : "
				+ remoteStunInfo.isRestrictedCone() + "\n"
				+ "\tisPortRestrictedCone : "
				+ remoteStunInfo.isPortRestrictedCone() + "\n"
				+ "\tisSymmetricCone : " + remoteStunInfo.isSymmetricCone()
				+ "\n");
					
			// decide which test should be run
			// if one of the sides has blocked UDP
			if (remoteStunInfo.isBlockedUDP() || localStunInfo.isBlockedUDP()) {
				// UDP communication is impossible -> UDP blocked on one of the
				// sides
				log.warn("UDP communication impossible, stopping test thread");
				return;
			}
			
			//Try to establish UDP connection
			UDPConnection udpConnection = UDPTest();
			// run established connection
			udpConnection.run();
			setUdpConnection(udpConnection);
	}

	private UDPConnection UDPTest() throws CommunicationFailedException {

		log.debug("Initiating UDP Tests");

		InetAddress localIas = null;
		InetAddress remoteIas = null;
		
		try {
			localIas = InetAddress.getByName(localStunInfo.getLocalIp());
			remoteIas = InetAddress.getByName(remoteStunInfo.getPublicIp());
		} catch (UnknownHostException e1) {
			log.error("Unable to resolve InetAddress", e1);
		}
		if (localIas == null) {
			log.error("Local InetAddress is not resolved");
			return null;
		}
		if (remoteIas == null) {
			log.error("Remote InetAddress is not resolved");
			return null;
		}
		
		// test local socket
		DatagramSocket localSocket = null;
		
		// local MappedAddress and MappinRule
		Integer localPortMappingRule = null;
		InetSocketAddress localMappedAddress = null;
		
		// remote MappedAddress and MappinRule
		remotePortMappingRule = null;
		remoteMappedAddress = null;
		
		// try to bind the socket
		for (int j = 0; j < MAX_BINDING_ERRORS; j++) {
			try {
				// choose port range
				int p = 49152 + (int) Math.round(Math.random() * 16383);
				// bind socket
				localSocket = new DatagramSocket(new InetSocketAddress(
						localIas, p));
				
				if (localSocket != null && localSocket.isBound()) {
					log.debug("DatagrammSocket is bound on localAddress ["
							+ localSocket.getLocalAddress()
									.getHostAddress() + ":"
							+ localSocket.getLocalPort() + "]");
					break;
				}
			} catch (SocketException e) {
				log.error("Unable to bind DatagramSocket on localAddress ["
						+ localSocket.getLocalAddress().getHostAddress()
						+ ":" + localSocket.getLocalPort() + "]", e);
				// destruct DatagramSocket
				localSocket = null;
			}// catch
		}// for -bindings
		
		// if timeout binding
		if (localSocket == null || !localSocket.isBound()) {
			log.error("Timeout binding DatagramSocket on localIp ["
					+ localIas.getHostAddress() + "]");
			return  null;
		}
		
		// try to set reuse=true address on socket
		try {
			localSocket.setReuseAddress(true);
			if (!localSocket.getReuseAddress()) {
				throw new SocketException("reuseAddress==false");
			}
		} catch (SocketException e) {
			log.error("Unable to set reuse address on bound socket ["
					+ localSocket.getLocalAddress().getHostAddress() + ":"
					+ localSocket.getLocalPort() + "]", e);
			return null;
		}

		// if behind Symmetric firewall try to guess the allocated port
		if (LocalStunInfo.getInstance().getStunInfo().isSymmetricCone()) {
			// get the needed amount of stun servers
			while (LocalStunInfo.getInstance().getStunServers(
					localSocket.getLocalAddress()).size() < 4) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			// get the possible port allocation rules
			List<Integer> rules = new ArrayList<Integer>();
			for (int i = 0; i < 5; i++) {
				try {
					int rule = portMappingRuleDiscovery(localSocket);
					rules.add(new Integer(rule));
				} catch (PortMappingRuleDiscoveryException e) {
					log.warn("Unable to Discover Port Mapping rule", e);
				}
			}
			log.debug("Discovered Port Allocation Rules :[" + rules + "]");
			
			// choose most frequent rule
			localPortMappingRule = getMostFrequentElem(rules);
			if (localPortMappingRule == null) {
				localPortMappingRule = DEFAULT_PORT_MAPPING_RULE;
			}
			
			log.debug("Using discovered Rule [" + localPortMappingRule
					+ "]");
		} else {
			// if not symmetric type, do not use MappingRule
			localPortMappingRule = 0;
		}
		
		// discover mapped IP address and port
		InetSocketAddress ias = null;
		try {
			ias = resolveMappedAddress(localSocket);
		} catch (MappedAddressResolvingException e) {
			log.error("Unable to resolve Mapped Address", e);
		}
		//TODO: add allocation rule here or on remote side
		localMappedAddress = new InetSocketAddress(ias.getAddress(), ias
				.getPort()
				+ localPortMappingRule);

		if (localMappedAddress == null) {
			log.error("Mapped address is not resolved, mappedAddress == null");
			return null;
		}

		log.info("Mapped address\t["
				+ localMappedAddress.getAddress().getHostAddress() + ":"
				+ localMappedAddress.getPort() + "]");
		// exchange MappedAddress

		log.debug("Mapped Address Exchange");
		// send localMappedAddress
		send(new UDPTestMessage(localMappedAddress,
				localPortMappingRule));
		// receive remote mapped address
		getRemoteMappedAddress();
		// receive remote port mapping rule
		getRemotePortMappingRule();
		
		if (remoteMappedAddress == null) {
			log.error("Remote mapped address is not resolved, RemoteMappedAddress == null");
			return null;
		}
		log.debug("RemoteMappedAddress ["
				+ remoteMappedAddress.getAddress().getHostAddress() + ":"
				+ remoteMappedAddress.getPort() + "]");

		// start Hole Punching
		// if hole punching succeeded, create UDP connection
		if(punchHole(localSocket)){
			return new UDPConnection(localSocket, remoteMappedAddress);
		} else {
			log.warn("Hole punching unsucceed, return null");
			return null;
		}
	}

	private int portMappingRuleDiscovery(DatagramSocket localSocket)
			throws PortMappingRuleDiscoveryException {
		log.debug("Port Mapping Rule Discovery");

		Collection<InetSocketAddress> stunServers = Collections
				.synchronizedCollection(LocalStunInfo.getInstance()
						.getStunServers(localSocket.getLocalAddress()));

		Integer rule = null;
		int previousMappedPort = -1;

		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(new InetSocketAddress(localSocket
					.getLocalAddress(), 0));
			if (socket != null && socket.isBound()) {
				socket.setReuseAddress(true);
			}
		} catch (SocketException e) {
			log.error("Unable to bind DatagramSocket on localIp ["
					+ socket.getLocalAddress().getHostAddress() + "]");
		}
		if (socket == null) {
			throw new PortMappingRuleDiscoveryException(
					"Unable to bind DatagramSocket on localIp ["
							+ socket.getLocalAddress().getHostAddress() + "]");
		}

		for (InetSocketAddress stunServer : stunServers) {
			try {
				//log.debug("RESOLVE MAPPED ADDRESS >>>>>>>");
				InetSocketAddress mappedAddress = resolveMappedAddress(socket,
						stunServer);
				//log.debug("RESOLVE MAPPED ADDRESS <<<<<<<");
				if (rule == null && previousMappedPort == -1) {
					previousMappedPort = mappedAddress.getPort();
				} else if (rule == null && previousMappedPort > -1) {
					rule = new Integer(mappedAddress.getPort()
							- previousMappedPort);
					previousMappedPort = mappedAddress.getPort();
				} else if (rule != null && previousMappedPort > -1) {
					/*
					 * log.debug("Port allocation rule: [MappedPort:" +
					 * mappedAddress.getPort() + "] [previous Port:" +
					 * previousMappedPort + "] [Rule:" + rule.intValue() + "]");
					 */
					if (rule.intValue() == (mappedAddress.getPort() - previousMappedPort)) {
						socket.disconnect();
						socket.close();
						return rule.intValue();
					} else {
						rule = new Integer(mappedAddress.getPort()
								- previousMappedPort);
						previousMappedPort = mappedAddress.getPort();
					}
				}
				log.debug("Port allocation rule: ["
						+ stunServer.getAddress().getHostAddress() + ":"
						+ stunServer.getPort() + "] <- ["
						+ mappedAddress.getAddress().getHostAddress() + ":"
						+ mappedAddress.getPort() + "] <- ["
						+ socket.getLocalAddress().getHostAddress() + ":"
						+ socket.getLocalPort() + "] rule:" + rule + "]");
			} catch (MappedAddressResolvingException e) {
				continue;
			}
		}
		throw new PortMappingRuleDiscoveryException(
				"Unable to discover Port Mapping rule ["
						+ socket.getLocalAddress().getHostAddress() + "]");
	}

	private InetSocketAddress resolveMappedAddress(DatagramSocket soc,
			InetSocketAddress stunServer)
			throws MappedAddressResolvingException {
		byte[] bytes = new byte[0];
		MessageHeader sendMh = null;
		try {
			// create DP content
			sendMh = new MessageHeader(MessageHeaderType.BindingRequest);
			sendMh.generateTransactionID();
			ChangeRequest cr = new ChangeRequest();
			sendMh.addMessageAttribute(cr);
			bytes = sendMh.getBytes();
		} catch (UtilityException e) {
			log.error("JSTUN utility exception", e);
			bytes = new byte[0];
			sendMh = null;
		}
		if (bytes.length == 0 || sendMh == null) {
			log.error("Unable to create content for DatagramPacket");
			throw new MappedAddressResolvingException(
					"Unable to create content for DatagramPacket");
		}

		// create DatagramPacket
		DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length);

		try {
			soc.connect(stunServer);
			soc.send(sendPacket);
		} catch (SocketException e) {
			log.warn("Unable to connect to stun Server ["
					+ stunServer.getAddress().getHostAddress() + ":"
					+ stunServer.getPort() + "] from local Socket ["
					+ soc.getLocalAddress().getHostAddress() + ":"
					+ soc.getLocalPort() + "]", e);
			throw new MappedAddressResolvingException(e);
		} catch (IOException e) {
			log.warn("Unable to send Packet to StunServer ["
					+ soc.getInetAddress().getHostAddress() + ":"
					+ soc.getPort() + "] from local Socket ["
					+ soc.getLocalAddress().getHostAddress() + ":"
					+ soc.getLocalPort() + "]", e);
			// skip this server
			throw new MappedAddressResolvingException(e);
		}

		//
		MessageHeader receiveMh = new MessageHeader();
		// listen for incoming packets
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[200],
					200);
			// log.debug("STUN SERVER MAPPED ADDRESS REQUEST >>>>>");
			soc.receive(receivePacket);
			// log.debug("STUN SERVER MAPPED ADDRESS REQUEST <<<<<");
			receiveMh = MessageHeader.parseHeader(receivePacket.getData());
		} catch (SocketTimeoutException e) {
			log.warn("Socket Timeout waiting answer from ["
					+ soc.getInetAddress().getHostAddress() + ":"
					+ soc.getPort() + "]");
			throw new MappedAddressResolvingException(e);
		} catch (IOException e) {
			log.warn("IOException while listening on ["
					+ soc.getInetAddress().getHostAddress() + ":"
					+ soc.getPort() + "]", e);
			throw new MappedAddressResolvingException(e);
		} catch (MessageAttributeParsingException e1) {
			log.warn("Unable to create content for DatagramPacket");
			throw new MappedAddressResolvingException(e1);
		} catch (MessageHeaderParsingException e2) {
			log.warn("Unable to create content for DatagramPacket");
			throw new MappedAddressResolvingException(e2);
		}
		if (!receiveMh.equalTransactionID(sendMh)) {
			log.warn("Discard wrong packet");
			throw new MappedAddressResolvingException("Discard wrong packet");
		}
		MappedAddress ma = (MappedAddress) receiveMh
				.getMessageAttribute(MessageAttributeType.MappedAddress);
		if (!(ma == null)) {
			InetAddress mappedIas = null;
			int mappedPort = -1;
			try {
				mappedIas = ma.getAddress().getInetAddress();
				mappedPort = ma.getPort();
			} catch (UtilityException e) {
				log.error("JSTUN utility exception",
						e);
			} catch (UnknownHostException e) {
				log.error(e.getMessage());
			}
			if (mappedIas == null || mappedPort <= 0) {
				log.error("Unable to get mapped address from received packet");
				throw new MappedAddressResolvingException(
						"Unable to get mapped address from received packet");
			}
			InetSocketAddress mappedAddress = new InetSocketAddress(mappedIas,
					mappedPort);
			soc.disconnect();
			return mappedAddress;
		} else {
			log.error("Unable to get mapped address using stunServer ["
					+ stunServer.getAddress().getHostAddress() + ":"
					+ stunServer.getPort() + "]");
			throw new MappedAddressResolvingException(
					"Unable to get mapped address using stunServer ["
							+ stunServer.getAddress().getHostAddress() + ":"
							+ stunServer.getPort() + "]");
		}
	}

	private InetSocketAddress resolveMappedAddress(DatagramSocket soc)
			throws MappedAddressResolvingException {
		InetSocketAddress mappedAddress = null;
		log.debug("Resolving Mapped Address");

		Collection<InetSocketAddress> stunServers = LocalStunInfo.getInstance()
				.getStunServers(soc.getLocalAddress());
		Collections.shuffle((List<InetSocketAddress>) stunServers);
		// log.info("Got total [" + stunServers.size() +
		// "] STUN server addresses for localIp");

		// set socket timeout
		try {
			if (soc.getSoTimeout() != RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT) {
				soc.setSoTimeout(RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT);
			}
		} catch (SocketException e) {
			log
					.error(
							"Unable to set socket timeout to RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT",
							e);
		}

		// loop over stunServers, ask for mapped address
		for (InetSocketAddress stunServer : stunServers) {
			mappedAddress = resolveMappedAddress(soc, stunServer);
			if (mappedAddress == null)
				throw new NullPointerException(
						"mappedAddress == null after resolving");
			break;
		}
		return mappedAddress;
	}

	private Integer getMostFrequentElem(List<Integer> list) {
		Integer mostFrequent = null;
		int count = 0;
		for (Integer i : list) {
			int c = Collections.frequency(list, i);
			if (c > count) {
				count = c;
				mostFrequent = i;
				if (count > (list.size() / 2)) {
					break;
				}
			}
		}
		return mostFrequent;
	}

	//common variables for send/receive loops 
	private Integer coneToSymPortRangePing = 0;
	private Integer attackOnRemotePort = 0;
	private boolean holePunchingRunning = true;
	
	private boolean localSideReceivedPing = false;
	private boolean remoteSideReceivedPing = false;
	
	private boolean punchHole(final DatagramSocket localSocket) {
		log.debug("Hole Punching");
		
		// set SO_TIMEOUT
		try {
			if (localSocket.getSoTimeout() != HOLE_PUNCHING_SO_TIMEOUT) {
				localSocket.setSoTimeout(HOLE_PUNCHING_SO_TIMEOUT);
			}
		} catch (SocketException e) {
			log.error( "Unable to set socket timeout to HOLE_PUNCHING_SO_TIMEOUT",e);
		}
		
		// set target port for attacking
		synchronized (attackOnRemotePort) {
			attackOnRemotePort = remoteMappedAddress.getPort();
		}
		
		// set range ping
		synchronized (coneToSymPortRangePing) {
			if (remoteStunInfo.isSymmetricCone()
					&& !LocalStunInfo.getInstance().getStunInfo()
							.isSymmetricCone()) {
				coneToSymPortRangePing = 1;
			}
		}
		
		// separate thread for listening UDP packets
		Thread udpListener = new Thread() {
			public void run() {
				byte[] receiveContent = new byte[UDPPacket.HASH_LENGTH + 1 + 4 + 4];
				while (holePunchingRunning) {
					try {
						// listen  for UDP packets
						DatagramPacket receivePacket = new DatagramPacket(
								receiveContent, receiveContent.length);
						localSocket.receive(receivePacket);
						UDPPacket udpp = new UDPPacket(receivePacket.getData());
						//if packet is PING
						if (UDPPacket.PING == udpp.getType()) {
							log.debug("Received PING packet from ["
									+ receivePacket.getAddress()
											.getHostAddress() + ":"
									+ receivePacket.getPort() + "]");
							
							// if destination address changed 
							if (!(remoteMappedAddress.getAddress().equals(
									receivePacket.getAddress()) && remoteMappedAddress
									.getPort() == receivePacket.getPort())) {
								// multiple subnetworks case
								// ping received from different remote address
								// change the target IP and port for ping
								remoteMappedAddress = new InetSocketAddress(
										receivePacket.getAddress(),
										receivePacket.getPort());
								// change the currently used target port
								// stop the range attack
								synchronized (attackOnRemotePort) {
									attackOnRemotePort = receivePacket.getPort();
								}
								synchronized (coneToSymPortRangePing) {
									coneToSymPortRangePing = 0;
								}
							}

							// inform the other side that we have received a
							// PING packet
							try {
								send(new UDPTestMessage(UDPTestMessage.Type.RECEIVED_PING));
								localSideReceivedPing = true;
								return;
							} catch (CommunicationFailedException e) {
								log.error("Unable to send messsage",e);
							}
						} else {
							log.warn("Illegal packet received " + receivePacket);
						}
					} catch (SocketTimeoutException e) {
						log.error("Socket timeout exception receiving PING packet", e);
					} catch (Exception e) {
						log.error("Exception receiving PING packet", e);
					}
				}
				log.debug("HOLE Punching Receive Thread is STOPPING");
			}
		};
		udpListener.start();
		
		// first try with remote mapping rule
		synchronized (attackOnRemotePort) {
			//TODO: add mapping rule here or on local side
			attackOnRemotePort = attackOnRemotePort + remotePortMappingRule;
		}
		long startTime = System.currentTimeMillis();
		for (int ping_counter = 0, port_increment_counter = 0; 
				holePunchingRunning;  
				ping_counter++) {
			if (System.currentTimeMillis() - startTime > 60000) {
				// stop send/receive loops
				holePunchingRunning = false;
				break;
			}
			// prepare PING UDPPacket
			UDPPacket sendUDPpacket = new UDPPacket(UDPPacket.PING);
			byte[] sendContent = sendUDPpacket.getBytes();
			DatagramPacket sendPacket = null;
			// next if there is cone -> symmetric case
			// try to ping a range of ports on symmetric side
			int p = attackOnRemotePort
					+ (coneToSymPortRangePing * port_increment_counter * ((int) Math
							.pow((-1), port_increment_counter)));

			//prepare PING DatagramPacket
			try {
				InetSocketAddress ias = new InetSocketAddress(
						remoteMappedAddress.getAddress(), p);
				sendPacket = new DatagramPacket(sendContent,
						sendContent.length, ias);
			} catch (IllegalArgumentException e) {
				if (p > 65535 || p < 1024) {
					log.error("Remote Port number out of range [" + p + "]", e);
					// stop send/receive loops
					holePunchingRunning = false;
					break;
				}
			} catch (SocketException e) {
				log.error(e.getMessage(), e);
			}
			if (sendPacket == null)
				continue;

			//send packet
			try {
				localSocket.send(sendPacket);
				log.debug("Sent PING packet to "
						+ sendPacket.getAddress().getHostAddress() + ":"
						+ sendPacket.getPort());
			} catch (IOException e) {
				log.warn("I/O Exception sending PING packet to ["
						+ sendPacket.getAddress().getHostAddress() + " "
						+ sendPacket.getPort() + "]", e);
			}
			// if remote and local sides received pings 
			// stop pinging
			if ( localSideReceivedPing && remoteSideReceivedPing){
				log.debug("Both sides received PING, hole punching succeed");
				holePunchingRunning = false;
			}
			
			// sleep
			try {
				Thread.sleep(DEFAULT_PING_INTERVAL);
			} catch (InterruptedException e) {
			}

			if (ping_counter > 3) {
				synchronized (attackOnRemotePort) {
					attackOnRemotePort = p;
				}
				port_increment_counter++;
				ping_counter = 5;
			}
		}
		try {
			udpListener.join();
		} catch (InterruptedException e) {
		}
		log.debug("Hole Punching Method is STOPPING");
		
		// hole punching succeed if local and remote sides received pings
		return localSideReceivedPing && remoteSideReceivedPing;
	}

	private class MessageReceivingThread extends Thread{
		public MessageReceivingThread() {
			this.setName(this.getClass().getName());
		}
		
		public void run(){
			while(true){
				try{
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String message = null;
					while (message == null || "".equals(message)){
						message = br.readLine();
					}
					log.debug("Received message [" + message + "]");
					byte[] compressed = Util.decode(message);
					byte[] bytes = Util.unzip(compressed);
					Object obj = Util.deserializeObject(bytes);
					if (obj instanceof UDPTestMessage)  messageReceived((UDPTestMessage)obj);
				} catch (IOException e) {
					log.debug("Unable to receive message, IO Exception");
				} catch (ClassNotFoundException e){
					log.debug("Unable to deserialize message");
				}
			}
		}
	}
	
	private class BlockingReceiveQueue {

		private boolean isBlocking = false;
		private UDPTestMessage udpTestMessage = null;

		public synchronized UDPTestMessage getUDPTestMessage()
				throws ReceiveQueueException {
			if (!isBlocking) {
				isBlocking = true;
				try {
					wait();
				} catch (InterruptedException e) {
				}
				UDPTestMessage udpTestMessage = this.udpTestMessage;
				isBlocking = false;
				return udpTestMessage;
			} else
				throw new ReceiveQueueException(
						"Queue blocked, awaiting messages ...");
		}

		public synchronized void setUDPTestMessage(UDPTestMessage udpTestMessage)
				throws ReceiveQueueException {
			if (isBlocking) {
				this.udpTestMessage = udpTestMessage;
				notify();
			} else
				throw new ReceiveQueueException(
						"Queue unblocked, awaiting no messages ...");
		}
		
		public boolean isBlocking(){
			return isBlocking;
		}
	}

	private class ReceiveQueueException extends Exception {

		private static final long serialVersionUID = -6666347154011719243L;

		public ReceiveQueueException() {
			super();
		}

		public ReceiveQueueException(String message, Throwable cause) {
			super(message, cause);
		}

		public ReceiveQueueException(String message) {
			super(message);
		}

		public ReceiveQueueException(Throwable cause) {
			super(cause);
		}
	}
	
	private class NotConfirmedException extends Exception {

		private static final long serialVersionUID = -1144051931616044274L;

		public NotConfirmedException() {
			super();
		}

		public NotConfirmedException(String message, Throwable cause) {
			super(message, cause);
		}

		public NotConfirmedException(String message) {
			super(message);
		}

		public NotConfirmedException(Throwable cause) {
			super(cause);
		}
	}
	
	private class UndesiredTypeException extends Exception {

		private static final long serialVersionUID = 8981068069914209610L;

		public UndesiredTypeException() {
			super();
		}

		public UndesiredTypeException(String message, Throwable cause) {
			super(message, cause);
		}

		public UndesiredTypeException(String message) {
			super(message);
		}

		public UndesiredTypeException(Throwable cause) {
			super(cause);
		}
	}
}

class UDPTestMessage implements Serializable {
	private static final long serialVersionUID = 8503336434324780827L;

	enum Type {
		INIT, 
		STUN_INFO,
		// STUN_INFO_RECEIVED,
		MAPPED_ADDRESS,
		// MAPPED_ADDRESS_RECEIVED,
		// CONNECTION_ID,
		RECEIVED_PING,
		//CONFIRM
	}

	Type type = null;

	UDPTestMessage() {
		type = Type.INIT;
	}

	UDPTestMessage(Type type) {
		this.type = type;
	}

	StunInfo stunInfo = null;
	UUID id = null;
	InetSocketAddress mappedAddress = null;
	Integer portMappingRule = null;

	/*
	 * UDPTestMessage(UUID id){ this.id = id; this.type = Type.CONNECTION_ID; }
	 */

	UDPTestMessage(InetSocketAddress mAddress, Integer mRule) {
		this.mappedAddress = mAddress;
		this.portMappingRule = mRule;
		this.type = Type.MAPPED_ADDRESS;
	}

	UDPTestMessage(InetSocketAddress mAddress) {
		this.mappedAddress = mAddress;
		this.type = Type.MAPPED_ADDRESS;
	}

	UDPTestMessage(StunInfo stunInfo) {
		type = Type.STUN_INFO;
		this.stunInfo = stunInfo;
	}

	public String toString() {
		String s = "UDPTestMessage [" + type.toString() + "]";
		return s;
	}
}

@SuppressWarnings("serial")
class PortMappingRuleDiscoveryException extends Exception {
	PortMappingRuleDiscoveryException() {
		super();
	}

	PortMappingRuleDiscoveryException(String message) {
		super(message);
	}

	PortMappingRuleDiscoveryException(String message, Throwable e) {
		super(message, e);
	}

	PortMappingRuleDiscoveryException(Throwable e) {
		super(e);
	}
}

@SuppressWarnings("serial")
class MappedAddressResolvingException extends Exception {
	MappedAddressResolvingException() {
		super();
	}

	MappedAddressResolvingException(String message) {
		super(message);
	}

	MappedAddressResolvingException(String message, Throwable e) {
		super(message, e);
	}

	MappedAddressResolvingException(Throwable e) {
		super(e);
	}
}
