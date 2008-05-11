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
import java.util.Random;
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
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.stun.LocalStunInfo;

public class UDPConnection extends Thread implements Activity{
	
	private final static Logger log = Logger.getLogger(UDPConnection.class);
	
	//Default waiting timeout
	private final static int DEFAULT_WAITING_TIMEOUT = 600;
	
	//SO Timeouts
	private final static int RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT = 1000;
	private final static int HOLE_PUNCHING_SO_TIMEOUT = 10000;
	
	//Connection failed after ...
	private final static int MAX_SEND_ERRORS = 10;
	private final static int MAX_SEND_TIMEOUTS = 10;
	
	//
	private final static int DEFAULT_PORT_MAPPING_RULE = 1;  
	
	//
	private final static String HASH_ALGORITHM = "MD5";
	
	
	//Member fields
	//
	private UDPTester udpTester = null;
	private DatagramSocket localSocket = null;
	private UUID connectionId = null;
	private Status status = Status.INIT;
	
	//
	private InetSocketAddress localMappedAddress = null;
	private InetSocketAddress remoteMappedAddress = null;
	
	//
	private Integer localPortMappingRule = null;
	private Integer remotePortMappingRule = null;
	
	//
	private MessageDigest md = null;
	
	private enum Status{
		INIT,
		GOT_MAPPED_ADDRESS,
		//HOLE_PUNCHING_TIMEOUT,
		CONNECTION_ESTABLISHED,
        READY_TO_LISTEN,
        IDLE,
		CLOSING,
		SENDING,
		RECEIVING	
	}

	//Constructors
	private UDPConnection(DatagramSocket localSocket,
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
	
	UDPConnection (DatagramSocket localSocket,
				   InetAddress remoteIp,
				   UDPTester parent) throws NoSuchAlgorithmException {
		this (localSocket, remoteIp, parent, HASH_ALGORITHM);
	}
	
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
                    if (this.connectionId != null){
                    	i = DEFAULT_WAITING_TIMEOUT - 3;
                    }
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
	
	private void testFailed(){
		status = Status.CLOSING;
		this.udpTester.resetRunningTest();
	}
	
	private void sendUDPTestMessage(UDPTestMessage udpTestMessage) throws CommunicationFailedException{
		this.udpTester.sendUDPTestMessage(udpTestMessage);
	}
	
	void close(){
		log.info("Received stop signal, closing connection");
		this.status = Status.CLOSING;
		this.udpTester.removeConnection(this);
	}

	void receivedUDPTestMessage(UDPTestMessage udpm){
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
		} else if (this.status == Status.GOT_MAPPED_ADDRESS) {
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
        } else if (this.status == Status.CONNECTION_ESTABLISHED) {
 		} else {
			log.warn(" " + getName() 
						 + " Illegal message type at this moment [" 
						 + udpm.type + "]"
						 + " status [" + this.status + "]");
		}
	}
	
	UUID getConnectionId(){
		return connectionId;
	}
	
	void setConnectionId(UUID id){
		this.connectionId = id;
	}
	
	Status getStatus(){
		return status;
	}
	
	public InetSocketAddress getMappedAddress(){
		return localMappedAddress;
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
	
    private Integer synGen = null;
	//Main Send Bytes method
    synchronized void send(final byte[] bytes) throws CommunicationFailedException
    {
		//try to send SYN packet
		this.status = Status.SENDING;
        synGen = new Random(F2FComputing.getLocalPeer().getID().getLeastSignificantBits()+System.currentTimeMillis()).nextInt();
        synchronized(synGen)
        {
    		UDPPacket content = null;
    		DatagramPacket packet = null;
    		try {
                byte[] integer = new byte[4]; 
                integer[0]=(byte)((synGen & 0xff000000)>>>24);
                integer[1]=(byte)((synGen & 0x00ff0000)>>>16);
                integer[2]=(byte)((synGen & 0x0000ff00)>>>8);
                integer[3]=(byte)((synGen & 0x000000ff));
                content = new UDPPacket(UDPPacket.SYN, integer, 0, integer.length, false);
                packet = new DatagramPacket(content.getBytes(), content.getBytes().length, remoteMappedAddress);
    			localSocket.send(packet);
    			log.debug("Sent SYN: " + content);
    		} catch (IOException e){
    			log.debug("Unable to send SYN packet", e);
    			this.status = Status.CLOSING;
    			return;
    		}
            
    		try
            {
                log.debug("Starting waiting for SYN-ACK");
                synGen.wait();
                log.debug("Stopping waiting for SYN-ACK");
            } catch (InterruptedException e1){}
            synGen = null;
        }
		
		log.debug("Received SYN-ACK");
		log.debug("Sending [" + Arrays.toString(bytes) + "]");
		if (send(bytes,0,bytes.length,false,false))
			this.status = Status.IDLE;
		else 
			this.status = Status.CLOSING;
	}

    private void listen()
    {
        ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"ID [" + connectionId.toString() + "] listening for incoming packets"));
		setName("UDP Connection ID [" + connectionId.toString() + "]");
		log.info("UDP Connection established, ID [" + connectionId.toString() + "]");
		log.debug("Listening for incoming packets");
		runPingThread();
		while (this.status != Status.CLOSING)
		{
			//try to set socket timeout
			setLocalSocketTimeout(0);
						
			byte[] buffer = new byte[UDPPacket.MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			UDPPacket content = null;
			//try to receive
			try {
                log.debug("Receive Starting >>>>>>");
                receive(packet);
                log.debug("Receive Stopping <<<<<<");
				content = new UDPPacket(packet.getData());
			} catch (SocketTimeoutException e) {
				log.debug("Receive Stopping <<<<<< SocketTimeoutException");
				continue;
			} catch (IOException e) {
				log.error("Unable to receive packet", e);
				continue;
			} catch (UDPPacketParseException e) {
				log.debug("Unable to parse UDP Packet",e);
				continue;
			}
            
            // at this point we have received something!!!
            if (content == null) continue;
            log.debug("UDP listener received " + content);
            
			if (content.getType() == UDPPacket.SYN)
            {
                if (synGen == null)
                {
                    receivedSYN();
                    this.status = Status.IDLE;
                    continue;
                }
                else
                {
                    synchronized (synGen)
                    {
                        log.debug("SYN collision. local gen " + synGen);
                        byte[] integer = content.getData();
                        log.debug("Collision Generated Int size [" + integer.length + "] content [" + Arrays.toString(integer) + "]");
                        int remoteGenSyn = 0;
                        for (int i = 0; i < 4; i++) {
                            int shift = (4 - 1 - i) * 8;
                            remoteGenSyn += (integer[i] & 0x000000FF) << shift;
                        }
                        log.debug("SYN collision. remote gen " + remoteGenSyn);
                        if (synGen.intValue() < remoteGenSyn)
                        {
                            receivedSYN();
                            synGen.notifyAll();
                        }
                    }
                }
			}
            if (content.getType() == UDPPacket.SYN_ACK)
            {
                synchronized (synGen)
                {
                    synGen.notifyAll();
                }
            }
		}
	}
	
	private void runPingThread()
	{
		new Thread ()
        {
            public void run()
            {
		        try
                {
		        	Thread.sleep(10000);
		        	log.debug("Status Before sending ID-PING [" + status + "]");
		        	if (status == Status.IDLE)
		        	{
		        		log.debug("Send ID-PING...");
		        		send(connectionId.toString().getBytes());
		        		log.debug("Sent ID-PING");
		        	}
                } catch (Exception e)
                {
                    log.warn("error Sending ID-PING ...", e);
                }
            }
        }.start();
	}

	private void setLocalSocketTimeout(int sendSoTimeout)
	{
		try {
			if (localSocket.getSoTimeout() != sendSoTimeout) {
				localSocket.setSoTimeout(sendSoTimeout);
			}
		} catch (SocketException e){
			log.error("Unable to set socket timeout to "+sendSoTimeout, e);
		}
	}

	synchronized private void receive(DatagramPacket packet) throws IOException
    {
        localSocket.receive(packet);
    }

    private void receivedSYN()
    {
        log.debug("Received SYN");
        //send confirmation
        this.status = Status.RECEIVING;
        try {
            UDPPacket content = new UDPPacket (UDPPacket.SYN_ACK);
            DatagramPacket packet = new DatagramPacket(content.getBytes(), 
                                        content.getBytes().length, remoteMappedAddress);
            localSocket.send(packet);
            log.debug("Sent SYN-ACK");
        } catch (IOException e) {
            log.error("Unable to send SYN_ACK", e);
            return;
        }
        
        byte[] receivedBytes = receiveData();
        
        //TODO:
        // deserialize the data and 
        // forward received object to the Core
        
        log.debug("Received bytes [" + Arrays.toString(receivedBytes) + "]");
        String rs = new String(receivedBytes);
        log.debug("Received string [" + rs + "]");
        if (this.connectionId.toString().equals(rs)){
            log.debug("Received ID-PING from paired connection");
        }
    }

    //recursive method
	private boolean send(byte[] bytes, int offset, int length, boolean hasMore, boolean split)
	{	
		//If message is larger then MAX size split in two
		if (length > UDPPacket.MAX_MESSAGE_SIZE) {
			log.debug("Message to large, split in two");
			return send(bytes, offset, length, false, true);
		}
		if (split) {
			int half_size = (int)(length/2d);
			return 
				send(bytes, offset, half_size, true, false) &&
				send(bytes, (offset + half_size), (length - half_size), hasMore, false);
		}
		log.debug("Sending [" + length + "] bytes");
		int errors = 0, timeouts = 0;
		while (this.status != Status.CLOSING){
			//Check counters
			if (errors > MAX_SEND_ERRORS){
				log.info("Max send errors reached, closing thread");
				return false;
			}
			if (timeouts > MAX_SEND_TIMEOUTS){
				log.info("Max send timeouts reached, closing thread");
				return false;
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
                return false;
			} catch (IOException e) {
				log.error("Unable to send [" + length + "] bytes", e);
				errors++;
                return false;
			}			
			//set socket timeout
			// we wait 1 second for ACK/NAK
			setLocalSocketTimeout(1000);
			
			// wait for answer
			byte[] buffer = new byte[UDPPacket.HASH_LENGTH + 1];
			DatagramPacket rDp = new DatagramPacket(buffer, buffer.length);
			UDPPacket content = null;
			try{
				receive(rDp);
				content = new UDPPacket(rDp.getData());
			} catch (SocketTimeoutException e) {
				log.warn("Timeout waiting for ACK");
				timeouts++;
				if (length == 1)
                {
                    return false;
                }
				return send(bytes,offset,length,hasMore,true);
			} catch (IOException e){
				log.error("Unable to receive ACK", e);
				errors++;
                return false;
			} catch (UDPPacketParseException e) {
				log.debug("Unable to parse UDP Packet", e);
                return false;
			} 
			
			if(content != null && content.getType() == UDPPacket.ACK){
				return true;
			}
			if(content != null && content.getType() == UDPPacket.NAK){
				return send(bytes, offset, length, hasMore, true);
			}
		}
		return false;
	}
	
	private byte[] receiveData()
	{
		//Final data array
		byte[] returnData = new byte[0];
		byte[] pData = new byte[UDPPacket.MAX_PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(pData, pData.length);
		int errors = 0;
		while (this.status != Status.CLOSING)
		{
			//Check counters
			if (errors > MAX_SEND_ERRORS){
				log.info("Max send errors reached, closing thread");
				this.status = Status.CLOSING;
                //TODO: do not close the connection
                // order the remote peer to start the sending again
				break;
			}
			//try to set socket timeout
			setLocalSocketTimeout(0);
			
			//try to receive
			try{
				receive(packet);
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
		
		setLocalSocketTimeout(5000);
		
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
			ias = resolveMappedAddress();
		} catch (MappedAddressResolvingException e) {
			log.error("Unable to resolve Mapped Address",e);
		}
		this.localMappedAddress = new InetSocketAddress(ias.getAddress(),ias.getPort() 
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
				   + this.localMappedAddress.getAddress().getHostAddress()
				   + ":"
				   + this.localMappedAddress.getPort()
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
		if (this.status != Status.CONNECTION_ESTABLISHED){
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
		} else {
			//connection established, start listening
			exchangeID();
            this.status = Status.IDLE;
			listen();
		}
	}
	
	private Integer coneToSymPortRangePing = 0;
	private Integer attackOnRemotePort = 0;
	
    private boolean holePunchTimeout = false;
	private void punchHole(){
		log.debug(getActivityName() + "Hole Punching");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Hole Punching"));
				
		setLocalSocketTimeout(HOLE_PUNCHING_SO_TIMEOUT);
		
		synchronized (attackOnRemotePort) {
			attackOnRemotePort = this.remoteMappedAddress.getPort();
		}
		synchronized (coneToSymPortRangePing) {
			if (this.udpTester.getRemoteStunInfo().isSymmetricCone() &&
				!LocalStunInfo.getInstance().getStunInfo().isSymmetricCone()) {
				coneToSymPortRangePing = 1;
			}
			//else if ()
		}
		
		final int AFTER_CONNECTION_ESTABLISHED_RESEND_AMOUNT = 1;
		
		Thread udpListener = new Thread(){
			public void run(){
				byte[] receiveContent = new byte[UDPPacket.HASH_LENGTH + 1];
				int counter = 1;
				for( int n = 0; n < AFTER_CONNECTION_ESTABLISHED_RESEND_AMOUNT && 
					  !holePunchTimeout && 
					  status != Status.CLOSING;
					  /* status != Status.CONNECTION_ESTABLISHED */ ){	
					try {
						DatagramPacket receivePacket = new DatagramPacket(receiveContent,receiveContent.length);
						receive(receivePacket);
                        UDPPacket udpp = new UDPPacket(receivePacket.getData());
						if (UDPPacket.PING == udpp.getType()){
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
                                holePunchTimeout = true;
							}
						} else {
							log.warn("Hole punching timeout, no result, stopping thread");
                            holePunchTimeout = true;
						}
					} catch (Exception e) {
						log.warn("Exception receiving PING packet",e);
					}
				}
			}
		};
		udpListener.start();
		//first try with remote mapping rule
		synchronized (attackOnRemotePort){
			attackOnRemotePort = attackOnRemotePort + this.remotePortMappingRule;
		}
		for (int afterConnnect = 0, ping_counter = 0, port_increment_counter = 0; 
			   afterConnnect < AFTER_CONNECTION_ESTABLISHED_RESEND_AMOUNT &&
			   !holePunchTimeout && 
			   status != Status.CLOSING; 
			 ping_counter++){
			   /* status != Status.CONNECTION_ESTABLISHED; */
			
			byte[] sendContent = (new UDPPacket(UDPPacket.PING)).getBytes();
			DatagramPacket sendPacket = null;
			//next if there is cone -> symmetric case
			//try to ping a range of ports on symmetric side 
			int p = attackOnRemotePort + (	coneToSymPortRangePing *
											port_increment_counter *
											((int) Math.pow((-1), port_increment_counter))
										 );
			
			try {
				InetSocketAddress ias = new InetSocketAddress(remoteMappedAddress.getAddress(),p);
				sendPacket = new DatagramPacket(sendContent,sendContent.length,ias);
			} catch (IllegalArgumentException e){
				if (p > 65535 || p < 1024){
					log.error("Remote Port number out of range [" + p + "]", e);
                    holePunchTimeout = true;
				}
			} catch (SocketException e1) {
				log.error(getActivityName() +  " " + e1.getMessage(),e1);
			}
			if (sendPacket == null){
				continue;
			}
			
			try{
					localSocket.send(sendPacket);
					log.debug("Sent PING packet to ["
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
				afterConnnect++;
			}
			if(ping_counter > 3){
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
	}
	
	private void exchangeMappedAddress() throws CommunicationFailedException{
		log.debug(getActivityName() + "Mapped Address Exchange");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"Mapped Address Exchange"));
		//exchange mapped addresses
		for(int i = 0; i < DEFAULT_WAITING_TIMEOUT; i++){
			try{
                sendUDPTestMessage(new UDPTestMessage(this.localMappedAddress,this.localPortMappingRule));
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
		DatagramPacket sendDp = new DatagramPacket(bytes,bytes.length);
		
		try{
			soc.connect(stunServer);
			soc.send(sendDp);
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
			DatagramPacket receiveDp = new DatagramPacket(new byte[200], 200);
			log.debug("STUN SERVER MAPPED ADDRESS REQUEST >>>>>");
			soc.receive(receiveDp);
			log.debug("STUN SERVER MAPPED ADDRESS REQUEST <<<<<");
			receiveMh = MessageHeader.parseHeader(receiveDp.getData());
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
		
		//set socket timeout
		setLocalSocketTimeout(RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT);
		
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
		
		DatagramSocket socket = null;
		try{
			socket = new DatagramSocket(new InetSocketAddress(this.localSocket.getLocalAddress(),0));
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
	
	@SuppressWarnings("serial")
	private class MappedAddressResolvingException extends Exception
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
	
	@SuppressWarnings("serial")
	private class PortMappingRuleDiscoveryException extends Exception
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
	private class UDPPacketParseException extends Exception
	{
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
	
	private class UDPPacket
	{
		/* Packet Structure
		 * |-------------|-------------DATA----------------------|
		 * |    HASH     |  TYPE   |   MESSAGE                   |
		 * |  16 bytes   |  1 byte |   MAX_MESSAGE_SIZE			 |
		 */
		
		final static byte ACK = 6;		//ACK if successfully received
		final static byte NAK = 11;		//NAK if failed
		final static byte SYN = 7;		//SYN initializes transfer
		final static byte SYN_ACK = 8;	//SYN-ACK confirms initialization
		final static byte PING = 12;    //hole punching ping
		
		final static int HASH_LENGTH = 16;
		
		final static int MAX_PACKET_SIZE = 65507;
		final static int MAX_MESSAGE_SIZE = MAX_PACKET_SIZE - HASH_LENGTH - 1;//one for MORE byte
		
		byte[] bytes = new byte[0];
		
		UDPPacket(byte type) {
			bytes = new byte[HASH_LENGTH +1];
			setType(type);
			setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
		}
		
		UDPPacket(byte[] data, int offset, int length, boolean more) 
							throws UDPPacketParseException {
			if (length == 0 || data.length == 0) 
				throw new UDPPacketParseException("No Data, array length == 0");
			bytes = new byte[1 + HASH_LENGTH + length];
			if (more) setType(ACK);
			else setType(NAK);
			setData(data, offset, length);
			setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
		}
        
        UDPPacket(byte type, byte[] data, int offset, int length, boolean more) 
                            {
            bytes = new byte[1 + HASH_LENGTH + length];
            setType(type);
            setData(data, offset, length);
            setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
        }
		
		UDPPacket(byte[] bytes) throws UDPPacketParseException{
			if (bytes.length < (MAX_PACKET_SIZE - MAX_MESSAGE_SIZE)) 
				throw new UDPPacketParseException("Message to Short");
			if (bytes[HASH_LENGTH] < ACK || bytes[HASH_LENGTH] > PING) 
            {
                log.error("received packet with wrong MORE field: "+bytes[HASH_LENGTH]);
                log.debug(Arrays.toString(bytes));
				throw new UDPPacketParseException("MORE Field Invalid");
            }
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
            } else if (bytes[HASH_LENGTH] == SYN){
                sb.append("SYN");
            } else if (bytes[HASH_LENGTH] == SYN_ACK){
                sb.append("SYN_ACK");
            } else if (bytes[HASH_LENGTH] == PING){
                sb.append("PING");
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
		
		private void setData(byte[] data, int offset, int length) {
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