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
import java.util.Collection;
import java.util.UUID;

import net.ulno.jpunch.exceptions.CommunicationFailedException;
import net.ulno.jpunch.exceptions.UdpTestException;
import net.ulno.jpunch.util.JPunchProperties;
import net.ulno.jpunch.util.Util;
import net.ulno.jpunch.util.stun.LocalStunInfo;
import net.ulno.jpunch.util.stun.StunInfo;

import org.apache.log4j.Logger;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.attribute.MessageAttributeInterface.MessageAttributeType;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.header.MessageHeaderInterface.MessageHeaderType;
import de.javawi.jstun.util.UtilityException;

public class UDPTester{
	final private static Logger log = Logger.getLogger(UDPTester.class);
	
	// StunInfo
	private StunInfo remoteStunInfo = null;
	private StunInfo localStunInfo = null;
	
	// Bindingsf
	private InetSocketAddress remoteBoundAddress = null;
	private InetSocketAddress localBoundAddress = null;
	
	// Receive Queue used for blocking receive
	//private BlockingReceiveQueue blockingReceiveQueue = new BlockingReceiveQueue();
	
	//Receiving thread
	private UDPMessageReceiverThread udpTestMessageReceivingThread = null;
	
	//Established Connection
	//private UDPConnection udpConnection = null;

	public UDPTester(){}
	
	/*
	 * TODO: Find better way to close back-end channel without braking pipes
	 * 
	 * Workaround for closing UDPMessageReceiverThread
	 * this should be called after all transmissions are done,
	 * this method closes back-end channel (this breaks redirection pipes
	 * and causes the application to exit)
	 */
	public void stopUDPTestMessageReceiverThread(){
		if (this.udpTestMessageReceivingThread != null){
			
			try{
				this.udpTestMessageReceivingThread.stopReceiverThread();
			} catch (IOException e){
				log.warn("Error stopping UDPMessageReceiverThread",e);
			}
			
			try {
				this.udpTestMessageReceivingThread.join();
			} catch (InterruptedException e){
				log.warn("Error stopping UDPMessageReceiverThread",e);
			}
		}
	}
	
	/*
	 * Returns Stun info of the remote host
	 * Blocks until remote host sends it's Stun info
	 */
	private synchronized StunInfo getRemoteStunInfo(){
		if(this.remoteStunInfo == null){
			try {
				wait();
			} catch (InterruptedException e) {}
			return this.remoteStunInfo;
		}
		return this.remoteStunInfo;
	}
	
	/*
	 * Sets the Stun info of the remote host
	 * Unblocks waiting methods
	 */
	private synchronized void setRemoteStunInfo(StunInfo stunInfo){
		if(this.remoteStunInfo == null && stunInfo != null && !"".equals(stunInfo)){
			this.remoteStunInfo = stunInfo;
			notify();
		}
	}
	
	/*
	 * Returns bound address of the remote host
	 * Blocks until remote host sends it's remote address
	 */
	private synchronized InetSocketAddress getRemoteBoundAddress(){
		if (this.remoteBoundAddress == null){
			try {
				wait();
			} catch (InterruptedException e) {}
			return this.remoteBoundAddress;
		}
		return this.remoteBoundAddress;
	}
	
	/*
	 * Sets the remote bound address
	 * Unblocks waiting methods
	 */
	private synchronized void setRemoteBoundAddress(
			InetSocketAddress remoteBoundAddress
			){
		if (this.remoteBoundAddress == null && remoteBoundAddress != null){
			this.remoteBoundAddress = remoteBoundAddress;
			notify();
		}
	}
	
	/**
	 * Non-Blocking send.
	 * For sending UDP test messages to the remote host
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
				System.out.flush();
			} catch (IOException e){
				throw new CommunicationFailedException("Failed Sending UDP Test Message ["
						+ udpTestMessage.toString() + "]",e);
			}
	}

	/*
	 * Handles incoming UDP test messages
	 */
	private void messageReceived(UDPTestMessage msg) {
		if (msg.type == UDPTestMessage.Type.INIT){
			
		} else if (msg.type == UDPTestMessage.Type.STUN_INFO) {
			this.setRemoteStunInfo(msg.stunInfo);
		} else if (msg.type == UDPTestMessage.Type.MAPPED_ADDRESS) {
			this.setRemoteBoundAddress(msg.mappedAddress);
		} else if (msg.type == UDPTestMessage.Type.RECEIVED_PING) {
			remoteSideReceivedPing = true;
			log.debug("Remote Side Received Ping");
		}
		
	}

	/**
	 * Makes proper tests for UDP connectivity tries to establish
	 * UDP Connection
	 * 
	 * @return established UDPConnection
	 * @return null if test failed
	 * @throws CommunicationFailedException if back-end connectivity failures occur
	 * @throws UdpTestException if some test preconditions unresolved
	 */
	public UDPConnection getUDPConnection() throws CommunicationFailedException,
													UdpTestException{
		
		//start receiving thread
		log.debug("Starting UDP test message receiver thread");
		udpTestMessageReceivingThread = new UDPMessageReceiverThread();
		udpTestMessageReceivingThread.start();
		
		//get local stun info
		log.debug("Discovering Local STUN Info");
		localStunInfo = LocalStunInfo.getInstance().getStunInfo();
				
		// exchange the STUN info
		log.debug("STUN info exchange");
		 this.send(new UDPTestMessage(localStunInfo));
		 
		log.info("Local STUN info:\n" 
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

		remoteStunInfo = this.getRemoteStunInfo();
		
		log.info("Remote STUN info:\n" 
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
		
		// now remote and local STUN info is known -> try to establish UDP
		// connection!
		
		try{
			if (!remoteStunInfo.isBlockedUDP() && !localStunInfo.isBlockedUDP()) {
				//Try to establish UDP connection
				UDPConnection udpConnection = UDPTest();
				return udpConnection;
			} else {
				// if one of the sides has blocked UDP
				throw new UdpTestException(
					"UDP communication impossible, one of the sides has blocked UDP");
			}
		} finally {
			// don't forget to stop message receiver thread
			/*
			 * TODO: actually should be uncommented, but using pipes like
			 * 		input | jPunch | output 
			 * in Linux, the pipe will be broken if we stop receiver thread
			 * here (close the InputStream)
			 * 
			try{
				log.trace("Stopping UDP test message receiver thread");
				udpTestMessageReceivingThread.stopReceiverThread();
				udpTestMessageReceivingThread.join();
				log.info("UDP test message receiver thread stopped");
			} catch (InterruptedException e) {
				log.warn("Wait for UDP Test Message receiver thread to stop",e);
			} catch (IOException e1){
				log.warn("IO Error closing UDP Test Message receiver thread", e1);
			}
			*/
		}
	}
	
	/*
	 * Makes proper tests for UDP connectivity tries to establish
	 * UDP Connection
	 * 
	 * @return established UDPConnection
	 * @return null if test failed
	 * @throws CommunicationFailedException if back-end connectivity failures occur
	 * @throws UdpTestException if some test preconditions unresolved
	 */
	private UDPConnection UDPTest() throws CommunicationFailedException,
											UdpTestException{

		log.debug("Initiating UDP Tests");
		InetAddress localIas;
		try{
			localIas = InetAddress.getByName(localStunInfo.getLocalIp());
		} catch (UnknownHostException e){
			throw new UdpTestException(String.format(
					"Uknown host [%s]", localStunInfo.getLocalIp()));
		}
		
		
		/*
		 * LOCAL
		 * ------------------------------------------------
		 * HOST			localUdpTestSocket
		 * 						|
		 * 						V
		 * NAT			localBoundAddress:(port+localPortBindingRule)
		 * ------------------------------------------------
		 * INET					| ^
		 * 						V |
		 * ------------------------------------------------
		 * NAT			remoteBoundAddress:(port+remotePortBindingRule)
		 * 						|
		 * 						V
		 * HOST					X (remote socket)
		 * ------------------------------------------------
		 * REMOTE
		 */
		
		// local test socket
		DatagramSocket localUdpTestSocket = null;
		
		// local Binding Rule
		Integer localPortBindingRule = null;
		
		// remote Bound Address
		remoteBoundAddress = null;
		
		int maxBindingErrors = JPunchProperties.getIntegerProperty(
								JPunchProperties.HP_MAX_BINDING_ERRORS);
		
		// try to bind the socket
		for (int j = 0; j < maxBindingErrors; j++) {
			try {
				// choose port range
				//TODO: put these numbers into properties file
				int p = 49152 + (int) Math.round(Math.random() * 16383);
				// bind socket
				localUdpTestSocket = new DatagramSocket(new InetSocketAddress(
						localIas, p));
				
				// TODO: Try test using DatagramChannel
				//SocketAddress socketAddress = new InetSocketAddress(localIas, p);
				//DatagramChannel datagramChannel = DatagramChannel.open();
				//localSocket = datagramChannel.socket();
				//localSocket.bind(socketAddress);
				
				if (localUdpTestSocket != null && localUdpTestSocket.isBound()) {
					log.debug("UDPTest DatagrammSocket is bound on localAddress ["
							+ localUdpTestSocket.getLocalAddress()
									.getHostAddress() + ":"
							+ localUdpTestSocket.getLocalPort() + "]");
					break;
				}
			} catch (SocketException e) {
				throw new UdpTestException(String.format(
						"Timeout binding UDPTest DatagramSocket on localIp [%s]",
						localIas.getHostAddress()),e);
			}// catch
		}// for -bindings

		
		// try to set reuse=true address on socket
		try{
			localUdpTestSocket.setReuseAddress(true);
		} catch (SocketException e){
			throw new UdpTestException(String.format(
				"Unable to set reuse address on local UDPTest DatagramSocket [%s:%d]",
				localUdpTestSocket.getLocalAddress().getHostAddress(),
				localUdpTestSocket.getLocalPort()));
		}

		// if host is behind the symmetric fire wall
		// try to guess the allocated port
		if (LocalStunInfo.getInstance().getStunInfo().isSymmetricCone()) {
			
			// wait for the needed amount of StunServers
			// TODO: Put needed amount of stun servers into properties files
			/*
			 * 
			while (LocalStunInfo.getInstance().getActiveStunServers(
					localUdpTestSocket.getLocalAddress()).size() < 2) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.warn("Wait for the needed amount of StunServers",e);
				}
			}
			 *
			 */
			
			// get the possible port binding rules
			// TODO: Put possible count into properties files
			/*
			 * Temporary do no not use binding rule discovery
			 * 
			List<Integer> rules = new ArrayList<Integer>();
			for (int i = 0; i < 5; i++) {
				try {
					int rule = portBindingRuleDiscovery(localUdpTestSocket);
					rules.add(new Integer(rule));
				} catch (PortBindingRuleDiscoveryException e) {
					log.warn("Unable to Discover Port Binding rule", e);
				}
			}
			log.debug("Discovered Port Binding rules :[" + rules + "]");	
			
			// choose most frequent port binding rule
			localPortBindingRule = getMostFrequentElem(rules);
			*/
			if (localPortBindingRule == null) {	
				localPortBindingRule = JPunchProperties.getIntegerProperty(
						JPunchProperties.HP_BINDING_RULE);
			}
		} else {
			// if not symmetric type, do not use Port Binding Rule
			localPortBindingRule = 0;
		}
		log.debug("Using Port Binding rule [" + localPortBindingRule
				+ "]");
		
		// discover bound IP address and port
		// in case of symmetric NAT the port is unreliable and 
		// and discovered port binding rule is used
		try {
			InetSocketAddress ias = resolveBoundAddress(localUdpTestSocket);
			//TODO: what is better ?
			// add binding rule here or on remote side
			localBoundAddress = new InetSocketAddress(ias.getAddress(), 
					ias.getPort() + localPortBindingRule);
		} catch (BoundAddressResolvingException e) {
			throw new UdpTestException("Unable to resolve Local Bound Address",e);
		}

		log.info("Local Bound address\t["
				+ localBoundAddress.getAddress().getHostAddress() + ":"
				+ localBoundAddress.getPort() + "]");
		
		// exchange the Bound Addresses
		log.debug("Bound Address Exchange");
		
		// TODO: re-factor, throw exception if remoteBoundAddress could not
		// be resolve instead of not-null check
		
		// send localBoundAddress
		send(new UDPTestMessage(localBoundAddress,localPortBindingRule));
		// receive remoteBoundAddress
		getRemoteBoundAddress();
		
		if (remoteBoundAddress == null) {
			throw new UdpTestException("Remote Bound Address is not resolved");
		}
		
		log.debug("Remote Bound Address ["
				+ remoteBoundAddress.getAddress().getHostAddress() + ":"
				+ remoteBoundAddress.getPort() + "]");

		// start Hole Punching
		// if UDP hole punching succeeded, create UDP connection
		try{
			if(udpHolePunching(localUdpTestSocket)){
				log.warn("UDP Hole Punching succeed, return UDPConnection");
				return new UDPConnection(localUdpTestSocket, remoteBoundAddress);
			} else {
				log.warn("UDP Hole Punching unsucceed, return null");
				return null;
			}
		} catch (UdpHolePunchingException e){
			throw new UdpTestException("Failure during UDP hole punching" ,e);
		}
	}
	
	// TODO: find the better way for discovering port binding rule
	/*
	private int portBindingRuleDiscovery(DatagramSocket localSocket)
			throws PortBindingRuleDiscoveryException{
		
		log.debug("Port Binding Rule Discovery");
		
		// Get active STUN servers
		Collection<InetSocketAddress> stunServers = Collections
				.synchronizedCollection(LocalStunInfo.getInstance()
						.getActiveStunServers(localSocket.getLocalAddress()));

		Integer bindingRule = null;		// Port Binding rule		
		int previousBoundPort = -1;		// Store previous discovered bound port
										//  to approximate the port binding rule 

		DatagramSocket pbrTestSocket = null;	// Random socket for PBR discovery
		
		try {
			pbrTestSocket = new DatagramSocket(new InetSocketAddress(
					localSocket.getLocalAddress(), 0));
			if (pbrTestSocket != null && pbrTestSocket.isBound()) {
				pbrTestSocket.setReuseAddress(true);
			}
		} catch (SocketException e) {
			log.error("Port Binding Rule Discovery - " +
					"Unable to bind DatagramSocket on localIp ["
					+ pbrTestSocket.getLocalAddress().getHostAddress() + "]");
		}

		//For each stun server try to resolve bound address and port for
		//previously chosen pbrTestSocket
		//use discovered bound port for
		//approximation of port binding rule
		for (InetSocketAddress stunServer : stunServers) {
			try {
				InetSocketAddress boundAddress = resolveBoundAddress(
						pbrTestSocket, stunServer);
				if (bindingRule == null && previousBoundPort == -1) {
					// first set-up initial bound port
					previousBoundPort = boundAddress.getPort();
				} else if (bindingRule == null && previousBoundPort > -1) {
					// next approximate the port binding rule
					bindingRule = new Integer(
							boundAddress.getPort() - previousBoundPort);
					previousBoundPort = boundAddress.getPort();
				} else if (bindingRule != null && previousBoundPort > -1) {
					// if next bound port differs of previous in the
					// value of discovered binding rule then the rule
					// is not changing and can be used
					if (bindingRule.intValue() == (boundAddress.getPort() - previousBoundPort)) {
						pbrTestSocket.disconnect();
						pbrTestSocket.close();
						return bindingRule.intValue();
					} else {
						bindingRule = new Integer(
								boundAddress.getPort()- previousBoundPort);
						previousBoundPort = boundAddress.getPort();
					}
				}
				log.debug("Discovering Port Binding Rule: ["
						+ stunServer.getAddress().getHostAddress() + ":"
						+ stunServer.getPort() + "] <- ["
						+ boundAddress.getAddress().getHostAddress() + ":"
						+ boundAddress.getPort() + "] <- ["
						+ pbrTestSocket.getLocalAddress().getHostAddress() + ":"
						+ pbrTestSocket.getLocalPort() + "] rule:" + bindingRule + "]");
			} catch (BoundAddressResolvingException e) {
				continue;
			}
		}
		throw new PortBindingRuleDiscoveryException(
				"Unable to discover Port Binding Rule ["
						+ pbrTestSocket.getLocalAddress().getHostAddress() + "]");
	}
	*/
	
	/**
	 * Returns the bound address using local test DatagramSocket
	 * and STUN server 
	 * 
	 * @param localTestSocket
	 * @param stunServer
	 * @return bound address for localTestSocket
	 * @throws BoundAddressResolvingException
	 */
	private InetSocketAddress resolveBoundAddress(DatagramSocket localTestSocket,
			InetSocketAddress stunServer)
			throws BoundAddressResolvingException {
		
		//Prepare test packet content
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
		if (bytes == null || sendMh == null || bytes.length == 0) { 
			log.error("Unable to create content for DatagramPacket");
			throw new BoundAddressResolvingException(
					"Unable to create content for DatagramPacket");
		}

		// create DatagramPacket
		DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length);

		try {
			// Connect to STUN server using localTestSocket
			localTestSocket.connect(stunServer);
			localTestSocket.send(sendPacket);
		} catch (IOException e) {
			log.warn("Unable to send Packet to StunServer ["
					+ localTestSocket.getInetAddress().getHostAddress() + ":"
					+ localTestSocket.getPort() + "] from local Socket ["
					+ localTestSocket.getLocalAddress().getHostAddress() + ":"
					+ localTestSocket.getLocalPort() + "]", e);
			// TODO: instead of dropping, skip this server and use next one
			throw new BoundAddressResolvingException(e);
		}

		// Receive response
		MessageHeader receiveMh = new MessageHeader();
		// listen for incoming packets
		try {
			// Create receive packet
			DatagramPacket receivePacket = new DatagramPacket(new byte[200],200);
			localTestSocket.receive(receivePacket);
			// Parse received packet
			receiveMh = MessageHeader.parseHeader(receivePacket.getData());
		} catch (SocketTimeoutException e) {
			log.warn("Socket Timeout waiting answer from ["
					+ localTestSocket.getInetAddress().getHostAddress() + ":"
					+ localTestSocket.getPort() + "]");
			throw new BoundAddressResolvingException(e);
		} catch (IOException e) {
			log.warn("IOException while listening on ["
					+ localTestSocket.getInetAddress().getHostAddress() + ":"
					+ localTestSocket.getPort() + "]", e);
			throw new BoundAddressResolvingException(e);
		} catch (MessageAttributeParsingException e1) {
			log.warn("Unable to parse attributes, using content of received" +
					" DatagramPacket");
			throw new BoundAddressResolvingException(e1);
		} catch (MessageHeaderParsingException e2) {
			log.warn("Unable to parse header, using content of received " +
					"DatagramPacket");
			throw new BoundAddressResolvingException(e2);
		}
		// Discarding wrong packets
		if (!receiveMh.equalTransactionID(sendMh)) {
			log.warn("Discard wrong packet");
			throw new BoundAddressResolvingException("Discard wrong packet");
		}
		
		// Extract Bound Address using packet contents
		MappedAddress ma = (MappedAddress) receiveMh
				.getMessageAttribute(MessageAttributeType.MappedAddress);
		
		InetAddress resolvedBoundAddress = null;
		int resolvedBoundPort = -1;
		try {
			resolvedBoundAddress = ma.getAddress().getInetAddress();
			resolvedBoundPort = ma.getPort();
		} catch (UtilityException e) {
			log.error("JSTUN utility exception", e);
			throw new BoundAddressResolvingException(
					"Unable to resolveBoundAddress",e);
		} catch (UnknownHostException e) {
			log.error(e.getMessage());
			throw new BoundAddressResolvingException(
					"Unable to resolveBoundAddress",e);
		}
		
		InetSocketAddress boundAddress = 
			new InetSocketAddress(resolvedBoundAddress,resolvedBoundPort);
		localTestSocket.disconnect();
		return boundAddress;
	}

	/**
	 * Iterates over STUN servers from properties file
	 * tries to resolve the bound address for localTestSovket
	 * using these STUN servers
	 * 
	 * @param localTestSocket
	 * @return boundAddress
	 * @throws BoundAddressResolvingException
	 */
	private InetSocketAddress resolveBoundAddress(DatagramSocket localTestSocket)
			throws BoundAddressResolvingException{
		
		InetSocketAddress boundAddress = null;
		log.debug("Resolving Bound Address");

		// Get the list of active STUN servers for local address
		// and shuffle the list
		Collection<InetSocketAddress> stunServers = 
			LocalStunInfo.getInstance().getActiveStunServers(localTestSocket.getLocalAddress());
		//Collections.shuffle((List<InetSocketAddress>) stunServers);
		
		// Prepare local test socket for resolving the bound address
		// set SO_TIMEOUT	
		int boundAddressSoTimeout = JPunchProperties.getIntegerProperty(
					JPunchProperties.HP_BOUND_ADDRESS_RESOLVE_SO_TIMEOUT);
		
		try {
			if (localTestSocket.getSoTimeout() != boundAddressSoTimeout) {
				localTestSocket.setSoTimeout(boundAddressSoTimeout);
			}
		} catch (SocketException e) {
			throw new BoundAddressResolvingException(
					"Unable to change SO_TIMEOUT of the localTestSocket to ->" +
					" RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT [" + 
					boundAddressSoTimeout + "]" ,e);
		}

		// loop over stunServers, ask and try to resolve Bound address
		for (InetSocketAddress stunServer : stunServers) {
			boundAddress = resolveBoundAddress(localTestSocket, stunServer);
			// if bound address was resolved return, else iterate further
			if (boundAddress != null){
				return boundAddress;
			}
		}
		throw new BoundAddressResolvingException("Unable to resolve Bound Address," +
				"tried [" + stunServers.size() + "] STUN servers");
	}

	/*
	 * Returns most frequent element in list
	 */
	/*
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
	*/
	//UDP Hole Punchings
	//common variables for send/receive threads
	// TODO: threads can be implemented in separate class
	private Integer coneToSymPortRangePing = 0;
	private Integer attackOnRemotePort = 0;
	private boolean holePunchingRunning = true;
	
	private boolean localSideReceivedPing = false;
	private boolean remoteSideReceivedPing = false;
	
	private boolean udpHolePunching(final DatagramSocket localTestSocket) 
			throws UdpHolePunchingException{
		log.debug("UDP Hole Punching");
		
		// Prepare local test socket for UDP hole punching
		// set SO_TIMEOUT
		int hpSoTimeout = JPunchProperties.getIntegerProperty(
							JPunchProperties.HP_TEST_SO_TIMEOUT);
		try {
			if (localTestSocket.getSoTimeout() != hpSoTimeout) {
				localTestSocket.setSoTimeout(hpSoTimeout);
			}
		} catch (SocketException e) {
			throw new UdpHolePunchingException("Unable to change SO_TIMEOUT of the localTestSocket to ->" +
					" HP_TEST_SO_TIMEOUT [" + 
					hpSoTimeout + "]" ,e);
		}
		
		//Setup common variables for send/receive threads
		
		// set target remote port for attacking
		synchronized (attackOnRemotePort) {
			attackOnRemotePort = remoteBoundAddress.getPort();
		}
		
		// set range ping
		synchronized (coneToSymPortRangePing) {
			if (remoteStunInfo.isSymmetricCone()
					&& !LocalStunInfo.getInstance().getStunInfo()
							.isSymmetricCone()) {
				coneToSymPortRangePing = 1;
			}
		}
		
		// separate thread for listening UDP packets (receive thread)
		log.info(String.format("Listening for packets on bound address [%s:%d]",
				localBoundAddress.getAddress().getHostAddress(),
				localBoundAddress.getPort()));
		Thread udpListener = new Thread() {
			public void run() {
				// Prepare content buffer for receiving
				// TODO: re-factor 1 + 1 + 4 
				byte[] receiveContent = new byte[UDPPacket.HASH_LENGTH + 1 + 4 + 4];
				while (holePunchingRunning) {
					try {
						// listen  for UDP packets
						DatagramPacket receivePacket = new DatagramPacket(
								receiveContent, receiveContent.length);
						localTestSocket.receive(receivePacket);
						// extract UDPPacket
						UDPPacket udpp = new UDPPacket(receivePacket.getData());
						
						//if packet is PING
						if (UDPPacket.PING == udpp.getType()) {
							log.debug("Received PING packet from ["
									+ receivePacket.getAddress()
											.getHostAddress() + ":"
									+ receivePacket.getPort() + "]");
							
							// if destination address changed 
							if (!(remoteBoundAddress.getAddress().equals(
									receivePacket.getAddress()) && remoteBoundAddress
									.getPort() == receivePacket.getPort())) {
								// multiple subnetworks case
								// ping received from different remote address
								// change the target IP and port for ping
								remoteBoundAddress = new InetSocketAddress(
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
							// PING packet, and stop thread
							try {
								send(new UDPTestMessage(UDPTestMessage.Type.RECEIVED_PING));
								localSideReceivedPing = true;
								break;
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
				log.debug("UDP Hole Punching Ping Receive Thread is Stopping");
				System.gc();
			}
		};
		udpListener.setName("UDP Hole Punching Ping Receive Thread");
		udpListener.start();
		
		// Timeout for sending thread
		int udpHpTimout = JPunchProperties.getIntegerProperty(
							JPunchProperties.HP_TEST_RUN_TIMEOUT);
		
		long startTime = System.currentTimeMillis();
		for (int /*ping_counter = 0,*/ port_increment_counter = 0; 
				holePunchingRunning;  
				/*ping_counter++*/) {
			if (System.currentTimeMillis() - startTime > udpHpTimout) {
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
						remoteBoundAddress.getAddress(), p);
				sendPacket = new DatagramPacket(sendContent, sendContent.length, ias);
			} catch (IllegalArgumentException e) {
				// TODO: put the numbers into the properties file
				if (p > 65535 || p < 1024) {
					log.error("Remote Port number out of range [" + p + "]", e);
					// stop send/receive loops
					holePunchingRunning = false;
					break;
				}
			} catch (SocketException e) {
				log.error(e.getMessage(), e);
			}
			if (sendPacket == null){
				log.error("Unable to create Datagramm Packet");
				continue;
			}
				

			//send packet
			try {
				localTestSocket.send(sendPacket);
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
			
			// Set Default delay between sending ping packets
			int defaultPingInterval = JPunchProperties.getIntegerProperty(
										JPunchProperties.HP_PING_INTERVAL);
			try {
				Thread.sleep(defaultPingInterval);
			} catch (InterruptedException e) {
				log.warn("UDP Hole Punching Ping Interval Sleep iterrupted", e);
			}
			
			// TODO: put the numbers into the properties file
			// Send 3 packets to each port
			// then increment port
			//if (ping_counter > 6) {
				synchronized (attackOnRemotePort) {
					attackOnRemotePort = p;
				}
				port_increment_counter++;
			//	ping_counter = 0;
			//}
		}
		log.info("UDP Hole Punching Ping Sender Stopped");
		// if ready, wait for receiver thread to stop
		try {
			udpListener.join();
			log.info("UDP Hole Punching Ping Receive Thread Stopped");
		} catch (InterruptedException e) {
			log.warn("Wait for UDP Hole Punching Ping Receive " +
					"thread to die interrupted",e);
		}
		log.debug("UDP Hole Punching Method is Stopped");
		
		// hole punching succeed if local and remote sides received pings
		return localSideReceivedPing && remoteSideReceivedPing;
	}
	
	/*
	 * UDP Message Receiver Thread
	 * Listening for console-input
	 * Filtering UDPMessages
	 */
	private class UDPMessageReceiverThread extends Thread{
		
		private BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		private boolean run = true;
		
		UDPMessageReceiverThread() {
			this.setName(this.getClass().getName());
		}
		
		void stopReceiverThread() throws IOException{
			log.trace("Closing InputReader ...");
			setRun(false);
			br.notify();
			br.close();
			log.trace("InputReader Closed");
		}
		
		private synchronized boolean isRunning(){
			return this.run;
		}
		
		private synchronized void setRun(boolean run){
			if (this.run != run) this.run = run;
		}
		
		public void run(){
			while(isRunning()){
				try{
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
			log.trace("Exit UDP Test Message Receiver main loop");
			br = null;
			System.gc();
		}
	}
	
	/*
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
	*/
}

class UDPTestMessage implements Serializable {
	private static final long serialVersionUID = 8503336434324780827L;

	enum Type {
		INIT, 
		STUN_INFO,
		MAPPED_ADDRESS,
		RECEIVED_PING,
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
class PortBindingRuleDiscoveryException extends Exception {
	PortBindingRuleDiscoveryException() {
		super();
	}

	PortBindingRuleDiscoveryException(String message) {
		super(message);
	}

	PortBindingRuleDiscoveryException(String message, Throwable e) {
		super(message, e);
	}

	PortBindingRuleDiscoveryException(Throwable e) {
		super(e);
	}
}

@SuppressWarnings("serial")
class BoundAddressResolvingException extends Exception {
	BoundAddressResolvingException() {
		super();
	}

	BoundAddressResolvingException(String message) {
		super(message);
	}

	BoundAddressResolvingException(String message, Throwable e) {
		super(message, e);
	}

	BoundAddressResolvingException(Throwable e) {
		super(e);
	}
}

class UdpHolePunchingException extends Exception {
	private static final long serialVersionUID = -2345597477755740582L;

	public UdpHolePunchingException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UdpHolePunchingException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public UdpHolePunchingException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public UdpHolePunchingException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}
}