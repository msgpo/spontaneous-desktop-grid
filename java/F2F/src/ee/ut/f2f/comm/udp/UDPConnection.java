package ee.ut.f2f.comm.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.stun.LocalStunInfo;

public class UDPConnection extends Thread implements Activity{
	
	private final static Logger log = Logger.getLogger(UDPConnection.class);
	
	//Default waiting timeout
	private final static int DEFAULT_WAITING_TIMEOUT = 600;
	
	//SO Timeouts
	private final static int RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT = 1000;
	private final static int HOLE_PUNCHING_SO_TIMEOUT = 10000;
	private final static int SEND_SO_TIMEOUT = 500;
	private final static int RECEIVE_SO_TIMEOUT = 0;
	
	//Connection failed after ...
	private final static int MAX_SEND_ERRORS = 10;
	private final static int MAX_SEND_TIMEOUTS = 10;
	
	//
	private final static int DEFAULT_PORT_MAPPING_RULE = 1;  
	
	//
	final static String HASH_ALGORITHM = "MD5";
	
	
	//Member fields
	//
	private UDPTester udpTester = null;
	private DatagramSocket localSocket = null;
	private UUID connectionId = null;
	private Status status = Status.INIT;
	
	//
	private InetSocketAddress mappedAddress = null;
	private InetSocketAddress remoteMappedAddress = null;
	
	//
	private Integer portMappingRule = null;
	private Integer remotePortMappingRule = null;
	
	//
	private MessageDigest md = null;
	
	private enum Status{
		INIT,
		GOT_MAPPED_ADDRESS,
		HOLE_PUNCHING,
		HOLE_PUNCHING_TIMEOUT,
		CONNECTION_ESTABLISHED,
        IDLE,
		CLOSING,
		SENDING,
		RECEIVING	
	}

	//Constructors
	public UDPConnection(DatagramSocket localSocket,
				   InetAddress remoteIp,
				   UDPTester parent,
				   String hashAlgorithm) throws NoSuchAlgorithmException {
		if (localSocket == null) throw new NullPointerException("localSocket == null");
		if (parent == null) throw new NullPointerException("parent UDPTester == null");
		
		this.localSocket = localSocket;
		this.udpTester = parent;
		this.setName("UDP TEST ");
		this.md = MessageDigest.getInstance(hashAlgorithm);
	}
	
	public UDPConnection (DatagramSocket localSocket,
				   InetAddress remoteIp,
				   UDPTester parent) throws NoSuchAlgorithmException {
		this (localSocket, remoteIp, parent, HASH_ALGORITHM);
	}
	
	
	//
	private void exchangeID(){
		if (this.udpTester.getRunningTest() == this){
			//connection ready
			//generate id
			UUID id = UUID.randomUUID();
			//send ID and wait for response
			for (int i = 0; i < DEFAULT_WAITING_TIMEOUT; i++)
            {
                try {
                    sendUDPTestMessage(new UDPTestMessage(id));
                    if (this.connectionId != null) break;
                    Thread.sleep(1000);
                } catch (Exception e) {}
            }
			if (this.connectionId == null){
				log.error(" " + getActivityName() + " Timeout waiting for remote ID");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
						ActivityEvent.Type.FAILED,
						"Timeout waiting for remote ID"));
				testFailed();
				return;
			}
			
			//choose ID
			if (this.connectionId.compareTo(id) != -1){
				this.connectionId = id;
			}
			//add new UDP connection and start next test 
			this.udpTester.addConnection(this);
			this.udpTester.resetRunningTest();
		} else {
			log.warn("running test != this");
		}
	}
	
	public void testFailed(){
		status = Status.CLOSING;
		this.udpTester.resetRunningTest();
	}
	
	public void sendUDPTestMessage(UDPTestMessage udpTestMessage) throws CommunicationFailedException{
		this.udpTester.sendUDPTestMessage(udpTestMessage);
	}
	
	public void close(){
		log.info("Received stop signal, closing connection");
		this.status = Status.CLOSING;
		this.udpTester.removeConnection(this);
	}

	public void receivedUDPTestMessage(UDPTestMessage udpm){
		if(this.status == Status.INIT){
			if(udpm.type == UDPTestMessage.Type.MAPPED_ADDRESS){
                if (udpm.mappedAddress == null) return;
				this.remoteMappedAddress = udpm.mappedAddress;
				this.remotePortMappingRule = udpm.portMappingRule;
				this.status = Status.GOT_MAPPED_ADDRESS;
			} else {
				log.warn("Illegal message type at this moment [" + udpm.type + "]"
							+ " status [" + this.status + "]");
			}
		} else if (this.status == Status.HOLE_PUNCHING) {
			if (udpm.type == UDPTestMessage.Type.RECEIVED_PING){
				this.status = Status.CONNECTION_ESTABLISHED;
			} else if (udpm.type == UDPTestMessage.Type.MAPPED_ADDRESS){
				this.remoteMappedAddress = udpm.mappedAddress;
			} else {
				log.warn(" " + getName() 
							 + " Illegal message type at this moment [" 
							 + udpm.type + "]"
							 + " status [" + this.status + "]");
			}			
 		} else {
			log.warn(" " + getName() 
						 + " Illegal message type at this moment [" 
						 + udpm.type + "]"
						 + " status [" + this.status + "]");
		}
	}
	
	public UUID getConnectionId(){
		return connectionId;
	}
	
	public void setConnectionId(UUID id){
		this.connectionId = id;
	}
	
	public Status getStatus(){
		return status;
	}
	
	public InetSocketAddress getMappedAddress(){
		return mappedAddress;
	}

	public String getActivityName() {
		return getName();
	}

	public Activity getParentActivity() {
		return this.udpTester;
	}

	
	public void run(){
		// just for information catch any exceptions that may occur
		try
		{
			testProcess();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.warn(getActivityName() + e.getMessage());
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FAILED, e.getMessage()));
		}
	}
	

	
	//Main Send Bytes method
	void send(final byte[] bytes) throws CommunicationFailedException {
		//check if Connection is established
		if (status != Status.IDLE){
			throw new CommunicationFailedException("UDP Connection is not established");
		}
		
		//try to send SYN packet
		this.status = Status.SENDING;
		UDPPacket content = null;
		DatagramPacket packet = null;
		try {
			if (localSocket.getSoTimeout() != RECEIVE_SO_TIMEOUT){
				localSocket.setSoTimeout(RECEIVE_SO_TIMEOUT);
			}
			content = new UDPPacket(UDPPacket.SYN);
			packet = new DatagramPacket(content.getBytes(), content.getBytes().length, remoteMappedAddress);
			localSocket.send(packet);
			log.debug("Sent SYN");
		} catch (IOException e){
			log.debug("Unable to send SYN packet", e);
			this.status = Status.CLOSING;
			return;
		} catch (UDPPacketParseException e) {
			log.debug("Unable to create SYN Packet", e);
            this.status = Status.CLOSING;
			return;
		}
		
		//wait for SYN-ACK packet
		try {
			localSocket.receive(packet);
			//log.debug("Received [" + Arrays.toString(packet.getData()) + "]");
			content = new UDPPacket(packet.getData());
		} catch (IOException e){
			log.debug("Unable to receive packet", e);
            this.status = Status.CLOSING;
			return;
		} catch (UDPPacketParseException e) {
			log.debug("Unable to parse UDP Packet", e);
            this.status = Status.CLOSING;
			return;
		}
		
		if (content.getType() == UDPPacket.SYN_ACK){
			log.debug("Received SYN-ACK");
			log.debug("Sending [" + Arrays.toString(bytes) + "]");
			send(bytes,0,bytes.length,false,false);
			//try to send FIN packet
			try {
				if (localSocket.getSoTimeout() != RECEIVE_SO_TIMEOUT){
					localSocket.setSoTimeout(RECEIVE_SO_TIMEOUT);
				}
				content = new UDPPacket(UDPPacket.FIN);
				packet = new DatagramPacket(content.getBytes(), content.getBytes().length, remoteMappedAddress);
				localSocket.send(packet);
				log.debug("Sent FIN");
			} catch (IOException e){
				log.debug("Unable to send FIN packet", e);
				this.status = Status.CLOSING;
				return;
			} catch (UDPPacketParseException e) {
				log.debug("Unable to create FIN Packet", e);
				this.status = Status.CLOSING;
				return;
			}
			
			//wait for FIN-ACK packet
			try {
				localSocket.receive(packet);
				content = new UDPPacket(packet.getData());
			} catch (IOException e){
				log.debug("Unable to receive packet", e);
				this.status = Status.CLOSING;
				return;
			} catch (UDPPacketParseException e) {
				log.debug("Unable to parse UDP Packet", e);
				this.status = Status.CLOSING;
				return;
			}
			
			if (content.getType() == UDPPacket.FIN_ACK){
				log.debug("Received FIN-ACK");
				this.status = Status.IDLE;
				return;
			}
            else
            {
                this.status = Status.CLOSING;
                return;
            }
		}
        else
        {
            this.status = Status.CLOSING;
            return;
        }
	}
	

	
	//Private Methods
	private void listen(){

        ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"ID [" + connectionId.toString() + "] listening for incoming packets"));
		setName("UDP Connection ID [" + connectionId.toString() + "]");
		log.info("UDP Connection established, ID [" + connectionId.toString() + "]");
		log.debug("Listening for incoming packets");
		int errors = 0, timeouts = 0, counter = 10;
		while (this.status != Status.CLOSING) {
			//Check counters
			if (errors > MAX_SEND_ERRORS){
				log.info("Max send errors reached, closing thread");
				this.status = Status.CLOSING;
				break;
			}
			if (timeouts > MAX_SEND_TIMEOUTS) {
				log.info("Max send timeouts reached, closing thread");
				this.status = Status.CLOSING;
				break;
			}
			
			//try to set send timeout
			try {
				if (localSocket.getSoTimeout() != SEND_SO_TIMEOUT) {
					localSocket.setSoTimeout(SEND_SO_TIMEOUT);
				}
			} catch (SocketException e){
				log.error("Unable to set SEND_SO_TIMEOUT", e);
				errors++;
				continue;
			}
			
			byte[] buffer = new byte[UDPPacket.HASH_LENGTH + 1];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			UDPPacket content = null;
			//try to receive
			try {
				localSocket.receive(packet);
				content = new UDPPacket(packet.getData());
			} catch (SocketTimeoutException e) {
				//if counter == 10 -> send ping
				if (counter == 10) {
					counter = 0;
					try {
						send(this.connectionId.toString().getBytes());
						log.debug("Sent ID-PING");
						continue;
					} catch (CommunicationFailedException e1) {
						log.error("Unable to send PING",e);
                        continue;
					}
				} else {
					counter++;
					log.debug("counter [" + counter + "]");
                    continue;
				}
			} catch (IOException e) {
				log.error("Unable to receive packet", e);
				errors++;
				continue;
			} catch (UDPPacketParseException e) {
				log.debug("Unable to parse UDP Packet",e);
				continue;
			}
            
            // at this point we have received something!!!
			
			//if received SYN packet
			if (content != null && content.getType() == UDPPacket.SYN) {
				log.debug("Received SYN");
				//send confirmation
				this.status = Status.RECEIVING;
				try {
					content = new UDPPacket (UDPPacket.SYN_ACK);
					packet = new DatagramPacket(content.getBytes(), 
												content.getBytes().length, remoteMappedAddress);
					localSocket.send(packet);
					log.debug("Sent SYN-ACK");
				} catch (IOException e) {
					log.error("Unable to send SYN_ACK", e);
					errors++;
					continue;
				} catch (UDPPacketParseException e) {
					log.debug("Unable to create SYN_ACK Packet", e);
					continue;
				}
				
				byte[] receivedBytes = receive();
                
                //TODO: 
                // deserialize the data and 
                // forward received object to the Core
				
				log.debug("Received bytes [" + Arrays.toString(receivedBytes) + "]");
				String rs = new String(receivedBytes);
				log.debug("Received string [" + rs + "]");
				if (this.connectionId.toString().equals(rs)){
					log.debug("Received ID-PING from paired connection");
				}
				
				errors = 0;
				timeouts = 0;
				this.status = Status.IDLE;
			}
			
			//if received FIN packet
			if (content != null && content.getType() == UDPPacket.FIN) {
				log.debug("Received FIN");
				//send confirmation
				this.status = Status.IDLE;
				try {
					content = new UDPPacket (UDPPacket.FIN_ACK);
					packet = new DatagramPacket(content.getBytes(), 
												content.getBytes().length, remoteMappedAddress);
					localSocket.send(packet);
					log.debug("Sent FIN-ACK");
				} catch (IOException e) {
					log.error("Unable to seng FIN_ACK", e);
					errors++;
					continue;
				} catch (UDPPacketParseException e) {
					log.debug("Unable to create FIN_ACK Packet", e);
					continue;
				}
			}
		}
	}
	
	
	//recursive method
	private void send(byte[] bytes, int offset, int length, boolean hasMore, boolean split){	
		//If message is larger then MAX size split in two
		if (length > UDPPacket.MAX_MESSAGE_SIZE) {
			log.debug("Message to large, split in two");
			send(bytes, offset, length, false, true);
			return;
		}
		if (split) {
			int half_size = (int)(length/2d);
			send(bytes, offset, half_size, true, false);
			send(bytes, (offset + half_size), (length - half_size), hasMore, false);
			return;
		}
		log.debug("Sending [" + length + "] bytes");
		int errors = 0, timeouts = 0;
		while (this.status != Status.CLOSING){
			//Check counters
			if (errors > MAX_SEND_ERRORS){
				log.info("Max send errors reached, closing thread");
				this.status = Status.CLOSING;
				break;
			}
			if (timeouts > MAX_SEND_TIMEOUTS){
				log.info("Max send timeouts reached, closing thread");
				this.status = Status.CLOSING;
				break;
			}
			
			// Try to send packet
			try {
				UDPPacket udpp = new UDPPacket(bytes, offset, length, hasMore);
				DatagramPacket sDp = new DatagramPacket(udpp.getBytes(), udpp
						.getBytes().length, remoteMappedAddress);
				localSocket.send(sDp);
			} catch (UDPPacketParseException e) {
				log.error("Unable to send [" + length + "] bytes ["
						+ e.getMessage() + "]");
				errors++;
                this.status = Status.CLOSING;
				continue;
			} catch (SocketException e) {
				log.error("Unable to send [" + length + "] bytes", e);
				errors++;
                this.status = Status.CLOSING;
				continue;
			} catch (IOException e) {
				log.error("Unable to send [" + length + "] bytes", e);
				errors++;
                this.status = Status.CLOSING;
				continue;
			}			
			//set SO_timeout
			try{
				if (localSocket.getSoTimeout() != SEND_SO_TIMEOUT) {
					localSocket.setSoTimeout(SEND_SO_TIMEOUT);
				}
			} catch (SocketException e) {
				log.debug("Unable to set SEND_SO_TIMEOUT [" + SEND_SO_TIMEOUT + "]",e);
			}
			// wait for answer
			byte[] buffer = new byte[UDPPacket.HASH_LENGTH + 1];
			DatagramPacket rDp = new DatagramPacket(buffer, buffer.length);
			UDPPacket content = null;
			try{
				localSocket.receive(rDp);
				content = new UDPPacket(rDp.getData());
			} catch (SocketTimeoutException e) {
				log.warn("Timeout waiting for ACK");
				timeouts++;
				if (length == 1)
                {
                    this.status = Status.CLOSING;
                    continue;
                }
				send(bytes,offset,length,hasMore,true);
				return;
			} catch (IOException e){
				log.error("Unable to receive ACK", e);
				errors++;
                this.status = Status.CLOSING;
				continue;
			} catch (UDPPacketParseException e) {
				log.debug("Unable to parse UDP Packet", e);
                this.status = Status.CLOSING;
				continue;
			} 
			
			if(content != null && content.getType() == UDPPacket.ACK){
				return;
			}
			if(content != null && content.getType() == UDPPacket.NAK){
				send(bytes, offset, length, hasMore, true);
				return;
			}
            this.status = Status.CLOSING;
			//log.debug("Errors [" + errors + "]");
			//errors++;
		}
	}
	
	private byte[] receive(){
		//Final data array
		byte[] returnData = new byte[0];
		byte[] pData = new byte[UDPPacket.MAX_PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(pData, pData.length);
		int errors = 0;
		while (this.status != Status.CLOSING){
			//Check counters
			if (errors > MAX_SEND_ERRORS){
				log.info("Max send errors reached, closing thread");
				this.status = Status.CLOSING;
                //TODO: do not close the connection
                // order the remote peer to start the sending again
				break;
			}
			//try to set SO_TIMEOUT
			try{
				if (localSocket.getSoTimeout() != RECEIVE_SO_TIMEOUT){
					localSocket.setSoTimeout(RECEIVE_SO_TIMEOUT);
				}
			} catch (SocketException e){
				log.error("Unable to set RECEIVE_SO_TIMEOUT [" + RECEIVE_SO_TIMEOUT + "]",e);
				errors++;
				continue;
			}
			//try to receive
			try{
				localSocket.receive(packet);
			} catch (IOException e){
				log.error("Unable to receive packet", e);
                this.status = Status.CLOSING;
				continue;
			}
			//try to get data
			UDPPacket udpp = null;
			try{
				udpp = new UDPPacket(packet.getData());
				//log.debug("Received [" + Arrays.toString(udpp.getBytes()) + "]");
			} catch (UDPPacketParseException e){
				log.error("Unable to parse received udp packet [" 
							+ packet.getData().length + "] bytes", e);
                this.status = Status.CLOSING;
                continue;
			}
			// check if hash OK 
			if (udpp != null && udpp.checkHash()){
                //append received data
                returnData = mergeByteArrays(returnData, udpp.getData());
				pData = new byte[] {UDPPacket.ACK};
			} else {
				log.warn("Hash check failed");
				pData = new byte[] {UDPPacket.NAK};
			}
			//check more field
			boolean hasMore = udpp.hasMore();
			
			//try send response
			try {
				UDPPacket content = new UDPPacket(pData[0]);
				packet = new DatagramPacket(content.getBytes(), 
											content.getBytes().length, remoteMappedAddress);
				localSocket.send(packet);
			} catch (IOException e){
				log.error("Unable to send response",e);
				errors++;
                this.status = Status.CLOSING;
				continue;
			} catch (UDPPacketParseException e) {
				log.debug("Unable to create response UDP Packet", e);
                this.status = Status.CLOSING;
				continue;
			} 
			if ( pData[0] == UDPPacket.ACK && !hasMore)
            {
                log.debug("Stop receiving, return [" + returnData.length + "] bytes");
                return returnData;
            }
		}
		return null;
	}
	
	private void testProcess() throws CommunicationFailedException{
		log.debug("Starting " + getActivityName());
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.STARTED,
				"Init"));
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
					int rule = portMappingRuleDiscovery();
					rules.add(new Integer(rule));
				} catch (PortMappingRuleDiscoveryException e) {
					log.warn("Unable to Discover Port Mapping rule", e);
				}
			}
			log.debug("Discovered Port Allocation Rules :[" + rules + "]");
			portMappingRule = getMostFrequentElem(rules);
			if (portMappingRule == null){
				portMappingRule = DEFAULT_PORT_MAPPING_RULE;
			}
			log.debug("Using discovered Rule [" + portMappingRule + "]");
		} else {
			portMappingRule = 0;
		}
		// discover mapped IP address and port
		InetSocketAddress ias = null;
		try{
			ias = resolveMappedAddress();
		} catch (MappedAddressResolvingException e) {
			log.error("Unable to resolve Mapped Address",e);
		}
		this.mappedAddress = new InetSocketAddress(ias.getAddress(),ias.getPort() 
									+ portMappingRule);
		
		if( mappedAddress == null ){
			log.error("Mapped address is not resolved");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FAILED,
					"Mapped address is not resolved"));
			return;
		}
		
		log.info(getActivityName() 
				   + "Mapped address\t["
				   + this.mappedAddress.getAddress().getHostAddress()
				   + ":"
				   + this.mappedAddress.getPort()
				   + "]");
		//exchange MappedAddress
		exchangeMappedAddress();
		
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
		punchHole();
		if (this.status == Status.HOLE_PUNCHING_TIMEOUT){
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FINISHED,
					"test failed"));
			log.warn(" Test Failed");
			testFailed();
		} else if (this.status == Status.CLOSING){
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FINISHED,
					"closed"));
			log.warn(((connectionId == null) ? " UDPTest " : ("UDP Connedtion ID ["
						+ connectionId.toString() + "]")) + " closed");
		} else if (this.status == Status.CONNECTION_ESTABLISHED){
			//connection established, start listening
			exchangeID();
            this.status = Status.IDLE;
			listen();
		}
	}

	/*
	private void passivePing(){
		while(true){
			if (connectionId != null) break;
			try{
				Thread.sleep(500);
			} catch (InterruptedException e) {}
		}
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"ID [" + connectionId.toString() + "] passive ping"));
		setName("UDP Connection ID [" + connectionId.toString() + "]");
		log.info("UDP Connection established, ID [" + connectionId.toString() + "]");
		log.debug("Entering passive ping mode");
		
		final String name = this.getName();
		Thread listener = new Thread(){
			public void run(){
				this.setName(name + " Passive Ping Thread");
				byte[] receiveContent = new byte[connectionId.toString().getBytes().length];
				while(status != Status.CLOSING){
					try{
						//Send passive ping
						byte[] sendContent = connectionId.toString().getBytes();
						DatagramPacket sendPacket = null;
						try {
							sendPacket = new DatagramPacket(sendContent,
															sendContent.length,remoteMappedAddress);
							
						} catch (SocketException e1) {
							log.error(getActivityName() +  " " + e1.getMessage(),e1);
						}
						if (sendPacket == null){
							continue;
						}
						
						try{
							try{
								localSocket.send(sendPacket);
								log.debug(" " + getName() 
										  + " -> ["
										  + remoteMappedAddress.getAddress().getHostAddress()
										  + ":"
										  + remoteMappedAddress.getPort()
										  + "]");
							} catch (IOException e){
								log.warn(" " + getName() + " Unable to send passive ping to", e);
							}
							Thread.sleep(5000);
						} catch (InterruptedException e) {}
						
						DatagramPacket receivePacket = new DatagramPacket(receiveContent,
																		  receiveContent.length);
						//Receive input
						localSocket.receive(receivePacket);
						String receivedString = new String(receivePacket.getData());
						UUID receivedId = null;
						
						try {
							receivedId = UUID.fromString(receivedString);
						} catch (IllegalArgumentException e){
							log.warn(" " + getName() + " Received [" + new String(receiveContent) + "]");
						}
						
						if (connectionId.equals(receivedId)){
								log.debug(" " 
										  + getName()
										  + " <- ["
										  + receivePacket.getAddress().getHostAddress()
										  + ":"
										  + receivePacket.getPort()
										  + "]");
						}
					} catch (SocketTimeoutException e) {
						//@TODO
						
					} catch (IOException e) {
						log.warn("I/O Exception receiving PING packet",e);
					}
				}
			}
		};
		
		listener.start();
	}
	*/
	
	private void punchHole(){
		log.debug(getActivityName() + "Hole Punching");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Hole Punching"));
		
		this.status = Status.HOLE_PUNCHING;
		
		try {
			localSocket.setSoTimeout(HOLE_PUNCHING_SO_TIMEOUT);
		} catch (SocketException e2) {
			log.error("Unable to set hole punching so_timeout", e2);
			return;
		}
		final int AFTER_CONNECTION_ESTABLISHED_RESEND_AMOUNT = 5;
		
		Thread udpListener = new Thread(){
			public void run(){
				byte[] receiveContent = new byte["PING".getBytes().length];
				int counter = 1;
				for( int n = 0; n < AFTER_CONNECTION_ESTABLISHED_RESEND_AMOUNT && 
					  status != Status.HOLE_PUNCHING_TIMEOUT && 
					  status != Status.CLOSING;
					  /* status != Status.CONNECTION_ESTABLISHED */ ){	
					try {
						DatagramPacket receivePacket = new DatagramPacket(receiveContent,receiveContent.length);
						localSocket.receive(receivePacket);
						if ("PING".equals(new String(receivePacket.getData()))){
							log.debug("Received PING packet from ["
										+ receivePacket.getAddress().getHostAddress()
										+ ":"
										+ receivePacket.getPort()
										+ "]");
							if (!(remoteMappedAddress.getAddress().equals(receivePacket.getAddress()) &&
								remoteMappedAddress.getPort() == receivePacket.getPort())){	
								remoteMappedAddress = new InetSocketAddress(receivePacket.getAddress(),
																		receivePacket.getPort());
							}
							try {
								sendUDPTestMessage(new UDPTestMessage(UDPTestMessage.Type.RECEIVED_PING));
								if (status == Status.CONNECTION_ESTABLISHED) n++;
							} catch (CommunicationFailedException e) {}
						} else {
							log.warn("Received " + new String(receivePacket.getData()));
						}
					} catch (SocketTimeoutException e) {
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
								status = Status.HOLE_PUNCHING_TIMEOUT;
							}
						} else {
							log.warn("Hole punching timeout, no result, stopping thread");
							status = Status.HOLE_PUNCHING_TIMEOUT;
						}
					} catch (IOException e) {
						log.warn("I/O Exception receiving PING packet",e);
					}
				}
			}
		};
		udpListener.start();
		int port = this.remoteMappedAddress.getPort();
		for (int afcr = 0, c = 0, counter = 0; 
			   afcr < AFTER_CONNECTION_ESTABLISHED_RESEND_AMOUNT &&
			   status != Status.HOLE_PUNCHING_TIMEOUT && 
			   status != Status.CLOSING; 
			 c++){
			   /* status != Status.CONNECTION_ESTABLISHED; */
			
			byte[] sendContent = "PING".getBytes();
			DatagramPacket sendPacket = null;			
			int p = port + (this.remotePortMappingRule *
										counter *
										((int) Math.pow((-1), counter))
						   );
			
			try {
				InetSocketAddress ias = new InetSocketAddress(remoteMappedAddress.getAddress(),p);
				sendPacket = new DatagramPacket(sendContent,sendContent.length,ias);
			} catch (IllegalArgumentException e){
				if (p > 65535 || p < 1024){
					log.error("Remote Port number out of range [" + p + "]", e);
					status = Status.HOLE_PUNCHING_TIMEOUT;
				}
			} catch (SocketException e1) {
				log.error(getActivityName() +  " " + e1.getMessage(),e1);
			}
			if (sendPacket == null){
				continue;
			}
			
			
			try{
					localSocket.send(sendPacket);
					log.debug("Sending PING packet to ["
							   + sendPacket.getAddress().getHostAddress()
							   + " "
							   + sendPacket.getPort()
							   +"]");
			} catch (IOException e){
					log.warn("I/O Exception sending PING packet to ["
							   + sendPacket.getAddress().getHostAddress()
							   + " "
							   + sendPacket.getPort()
							   +"]",e);
			}
			
			try{
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			
			if(status == Status.CONNECTION_ESTABLISHED){
				afcr++;
			}
			if(c > 3){
				port = p;
				counter++;
				c = 0;
			}
		}
	}
	
	private void exchangeMappedAddress() throws CommunicationFailedException{
		log.debug(getActivityName() + "Mapped Address Exchange");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Mapped Address Exchange"));
		//exchange mapped addresses
		for(int i = 0; i < DEFAULT_WAITING_TIMEOUT; i++){
			try{
                sendUDPTestMessage(new UDPTestMessage(this.mappedAddress,this.portMappingRule));
				if (this.remoteMappedAddress != null) return;
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		if( this.remoteMappedAddress == null){
			log.error("Timeout while waiting mapped address");
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FAILED,
					"Timeout while waiting mapped address"));
			return;
		}
	}
	
	private InetSocketAddress resolveMappedAddress(DatagramSocket localSocket, InetSocketAddress stunServer) throws MappedAddressResolvingException{
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
		DatagramPacket sendDp = new DatagramPacket(bytes,bytes.length);
		
		try{
			localSocket.connect(stunServer);
			localSocket.send(sendDp);
		} catch (SocketException e){
			log.warn(getActivityName() 
						+ " "
						+"Unable to connect to stun Server ["
						+ stunServer.getAddress().getHostAddress()
						+ ":"
						+ stunServer.getPort()
						+ "] from local Socket ["
						+ localSocket.getLocalAddress().getHostAddress()
						+ ":"
						+ localSocket.getLocalPort()
						+ "]",e);
			throw new MappedAddressResolvingException(e);
		} catch (IOException e){
			log.warn(getActivityName()
					+ " "
					+ "Unable to send Packet to StunServer ["
					+ localSocket.getInetAddress().getHostAddress()
					+ ":"
					+ localSocket.getPort()
					+ "] from local Socket ["
					+ localSocket.getLocalAddress().getHostAddress()
					+ ":"
					+ localSocket.getLocalPort()
					+ "]",e);
			//skip this server
			throw new MappedAddressResolvingException(e);
		}
		
		
		//
		MessageHeader receiveMh = new MessageHeader();
		//listen for incoming packets
		try{	
			DatagramPacket receiveDp = new DatagramPacket(new byte[200], 200);
			localSocket.receive(receiveDp);
			receiveMh = MessageHeader.parseHeader(receiveDp.getData());
		} catch (SocketTimeoutException e){
			log.warn(getActivityName() 
					+ " "
					+ "Socket Timeout waiting answer from ["
					+ localSocket.getInetAddress().getHostAddress()
					+ ":"
					+ localSocket.getPort()
					+ "]");
			throw new MappedAddressResolvingException(e);
		} catch (IOException e){
			log.warn(getActivityName() 
					+ " "
					+ "IOException while listening on ["
					+ localSocket.getInetAddress().getHostAddress()
					+ ":"
					+ localSocket.getPort()
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
			localSocket.disconnect();
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
	
	private InetSocketAddress resolveMappedAddress() throws MappedAddressResolvingException{
		InetSocketAddress mappedAddress = null;
		log.debug(getActivityName() 
				+ " "
				+ "Resolving Mapped Address");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Resolving Mapped Address"));
		
		Collection<InetSocketAddress> stunServers = LocalStunInfo.getInstance().getStunServers(this.localSocket.getLocalAddress());
		Collections.shuffle((List<InetSocketAddress>) stunServers);
		//log.info("Got total [" + stunServers.size() + "] STUN server addresses for localIp");
		
		//set socket Timeout 300
		try{
			this.localSocket.setSoTimeout(RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT);
		} catch (SocketException e){
			log.error(getActivityName() 
					+ " "
					+ "Unable to set Socket timeout",e);
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FAILED,
					"Unable to resolve Mapped Address"));
			throw new MappedAddressResolvingException("Unable to set Socket timeout",e);
		}
		
		//loop over stunServers, ask for mapped address
		for(InetSocketAddress stunServer : stunServers){
			mappedAddress = resolveMappedAddress(localSocket,stunServer);
			if (mappedAddress == null) throw new NullPointerException("mappedAddress == null after resolving");
			break;
		}
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Mapped Address Resolved"));
		return mappedAddress;
	}

	private int portMappingRuleDiscovery() throws PortMappingRuleDiscoveryException{
		log.debug(getActivityName() 
				+ " "
				+ "Port Mapping Rule Discovery");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Port Mapping Rule Discovery"));
		
		Collection<InetSocketAddress> stunServers = Collections.synchronizedCollection(LocalStunInfo.getInstance().getStunServers(this.localSocket.getLocalAddress()));
		//Collections.shuffle(stunServers);
		
		Integer rule = null;
		int previousMappedPort = -1;
		
		DatagramSocket localSocket = null;
		try{
			localSocket = new DatagramSocket(new InetSocketAddress(this.localSocket.getLocalAddress(),0));
			if (localSocket != null && localSocket.isBound()){
				localSocket.setReuseAddress(true);
			}
		} catch (SocketException e){
			log.error("Unable to bind DatagramSocket on localIp [" 
							+ localSocket.getLocalAddress().getHostAddress()
							+ "]");
		}
		if( localSocket == null ){
			throw new PortMappingRuleDiscoveryException("Unable to bind DatagramSocket on localIp [" 
					+ localSocket.getLocalAddress().getHostAddress()
					+ "]");
		}
		
		for(InetSocketAddress stunServer : stunServers){
			try {
				InetSocketAddress mappedAddress = resolveMappedAddress(localSocket, stunServer);
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
						localSocket.disconnect();
						localSocket.close();
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
						+ localSocket.getLocalAddress().getHostAddress()
						+ ":"
						+ localSocket.getLocalPort()
						+ "] rule:"
						+ rule
						+ "]");
			} catch (MappedAddressResolvingException e) {
				continue;
			}
		}
			throw new PortMappingRuleDiscoveryException("Unable to discover Port Mapping rule [" 
					+ localSocket.getLocalAddress().getHostAddress()
					+ "]");
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
		
	private int getDataLength(byte[] bytes, int offset, int length){
		int zero_start_at = 0, zero_end_at = 0;
		for (int i = offset; i < length; i++){
			if (bytes[offset + i] == 0 && zero_end_at >= zero_start_at){
				zero_start_at = i;
			} else if (bytes[offset + i] > 0 && zero_end_at < zero_start_at){
				zero_end_at = i;
			}
		}
		if (zero_start_at > zero_end_at){
			return zero_start_at;
		} else {
			return length;
		}
	}
	
	private byte[] trimByteArray(byte[] bytes, int offset, int length){
		return getSubSequence(bytes, offset, getDataLength(bytes, offset, length));
	}
	
	private byte[] getSubSequence(byte[] bytes, int offset, int length){
		if (offset == 0 && length == bytes.length) return bytes;
		byte[] returnBytes = new byte[length];
		for(int i = 0; i < returnBytes.length; i++) returnBytes[i] = bytes[offset + i];
		return returnBytes;
	}
	
	private byte[] mergeByteArrays(byte[] bytes1, byte[] bytes2){
		byte[] returnBytes = new byte[bytes1.length + bytes2.length];
		for (int i = 0; i < bytes1.length || i < bytes2.length ;i++){
			if( i < bytes1.length ) returnBytes[i] = bytes1[i];
			if( i < bytes2.length ) returnBytes[bytes1.length + i] = bytes2[i];
		}
		return returnBytes;
	}
	
	//Private Exceptions
	
	private class MappedAddressResolvingException extends Exception{
		
		private static final long serialVersionUID = -4728460267462164203L;

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
	
	private class PortMappingRuleDiscoveryException extends Exception{
		private static final long serialVersionUID = 131585482074418044L;
		
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
	
	private class UDPPacketParseException extends Exception{
		private static final long serialVersionUID = -6764517470407708849L;
		
		UDPPacketParseException(){
			super();
		}
		
		UDPPacketParseException(String message){
			super(message);
		}
		
		UDPPacketParseException(String message, Throwable e){
			super(message,e);
		}
		
		UDPPacketParseException(Throwable e){
			super(e);
		}
	}
	
	public class UDPPacket{
		/* Packet Structure
		 * |-------------|-------------DATA----------------------|
		 * |    HASH     |  TYPE   |   MESSAGE                   |
		 * |  16 bytes   |  1 byte |   MAX_MESSAGE_SIZE			 |
		 */
		
		
		final static byte ACK = 6;		//ACK if successfully received
		final static byte NAK = 11;		//NAK if failed
		final static byte SYN = 7;		//SYN initializes transfer
		final static byte SYN_ACK = 8;	//SYN-ACK confirms initialization
		final static byte FIN = 9;		//FIN finalizes transfer
		final static byte FIN_ACK = 10; //FIN-ACK confirms finalization
		
		final static int HASH_LENGTH = 16;
		
		final static int MAX_PACKET_SIZE = 65507;
		final static int MAX_MESSAGE_SIZE = MAX_PACKET_SIZE - HASH_LENGTH - 1;//one for MORE byte
		
		byte[] bytes = new byte[0];
		
		
		public UDPPacket(byte type) throws UDPPacketParseException {
			if (type < ACK || type > NAK) throw new UDPPacketParseException("Type not known [" + type + "]");
			bytes = new byte[HASH_LENGTH +1];
			setType(type);
			setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
		}
		
		public UDPPacket(byte[] data, int offset, int length, boolean more) 
							throws UDPPacketParseException {
			if (length == 0 || data.length == 0) 
				throw new UDPPacketParseException("No Data, array length == 0");
			bytes = new byte[1 + HASH_LENGTH + length];
			if (more) setType(ACK);
			else setType(NAK);
			setData(data, offset, length);
			setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
		}
		
		public UDPPacket(byte[] bytes) throws UDPPacketParseException{
			if (bytes.length < (MAX_PACKET_SIZE - MAX_MESSAGE_SIZE)) 
				throw new UDPPacketParseException("Message to Short");
			if (bytes[HASH_LENGTH] < ACK || bytes[HASH_LENGTH] > NAK) 
				throw new UDPPacketParseException("MORE Field Invalid");
			this.bytes = trimByteArray(bytes, 0, bytes.length); 
		}
		
		public boolean hasMore(){
			return bytes[0] == ACK;
		}
		
		public byte getType(){
			return bytes[HASH_LENGTH];
		}
 		
		public byte[] getBytes(){
			return bytes;	
		}
		
		public byte[] getHash(){
			return getBytes(0,HASH_LENGTH);
		}
		
		public byte[] getData(){
			return getBytes((HASH_LENGTH + 1), (bytes.length - HASH_LENGTH -1));
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("UDPPacket :\nType: [");
			if (bytes[HASH_LENGTH] == ACK) {
				sb.append("ACK");
			} else if (bytes[HASH_LENGTH] == NAK){
				sb.append("NAK");
			}
			sb.append("]\nData: [" + (bytes.length - HASH_LENGTH - 1) + "] bytes\n");
			sb.append("MD5 hash [" + byteArrayToHexString(bytes, 0, HASH_LENGTH) + "]\n");
			sb.append("Total [" + bytes.length + "] bytes\n");
			
			return sb.toString();
		}
		
		public boolean checkHash(){
			boolean b = false;
			byte[] hash = hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH));
			b = MessageDigest.isEqual(hash, getHash());
			return b;
		}
		
		private byte[] getBytes(int offset, int length){
			return getSubSequence(bytes, offset, length);
		}
		
		private void setHash(byte[] hash){
			setBytes(hash, 0, hash.length, 0);
		}
		
		private void setData(byte[] data, int offset, int length) throws UDPPacketParseException{
			setBytes(data, offset, length, (HASH_LENGTH + 1));
			/*
			byte[] hash = hashByteArray(data, offset, length);
			if (hash.length != HASH_LENGTH) throw new UDPPacketParseException("Unable to hash data");
			for (int i = 0; i < hash.length || i < length; i++){
				if( i < hash.length ) bytes[i] = hash[i];
				if( i < length ) bytes[hash.length + 1 + i] = data[offset + i];
			}
			*/
		}
		
		private void setType(byte type){
			bytes[HASH_LENGTH] = type;
		}
		
		private void setBytes(byte[] src_bytes, int src_offset, int src_length, int dest_offset) {
			for (int i = 0; i < src_length; i++){
				bytes[dest_offset + i] = src_bytes[src_offset + i];
			}
		}
		
		private byte[] hashByteArray(byte[] bytes, int offset, int length) {
			md.reset();
			md.update(bytes, offset, length);
			return md.digest();
		}
		
		private String byteArrayToHexString(byte[] bytes, int offset, int length){
			StringBuffer sBuf = new StringBuffer();
			for (int i = 0; i < length; i++) {
				String hex = Integer.toHexString(0xFF & bytes[offset + i]);
				if (hex.length() == 1) sBuf.append("0");
				sBuf.append(hex);
			}
			return sBuf.toString();
		}
		
		
	}
}