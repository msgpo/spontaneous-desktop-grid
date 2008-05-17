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
import java.util.concurrent.ConcurrentLinkedQueue;

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
import ee.ut.f2f.comm.BlockingMessageSender;
import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.Util;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.stun.LocalStunInfo;

public class UDPConnection extends BlockingMessageSender implements Activity, Runnable
{
	
	private final static Logger log = Logger.getLogger(UDPConnection.class);
	
	//Default waiting timeout
	private final static int DEFAULT_WAITING_TIMEOUT = 600;
	
	//SO Timeouts
	private final static int RESOLVING_MAPPED_ADDRESS_SO_TIMEOUT = 1000;
	private final static int HOLE_PUNCHING_SO_TIMEOUT = 10000;
		
	//
	private final static int DEFAULT_PORT_MAPPING_RULE = 1;  
	
	//
	private final static String HASH_ALGORITHM = "MD5";
	
	
	//Member fields
	//
	private UDPTester udpTester = null;
	private DatagramSocket localSocket = null;
	
	private Status status = Status.INIT;
	
	//
	private UUID localConnectionId = null;
	
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
		CONNECTION_ESTABLISHED,
        IDLE,
		SENDING,
		RECEIVING,
        CLOSING
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
        this.localConnectionId = UUID.randomUUID();
	}
	
	UDPConnection (DatagramSocket localSocket,
				   InetAddress remoteIp,
				   UDPTester parent) throws NoSuchAlgorithmException {
		this (localSocket, remoteIp, parent, HASH_ALGORITHM);
	}
	
	/*private void exchangeID(){
		log.debug("Start Excahnging ID");
		if (this.udpTester.getRunningTest() == this){
			//connection ready
			//generate id
			this.localConnectionId = UUID.randomUUID();
			//send ID and wait for response
			for (int i = 0; i < DEFAULT_WAITING_TIMEOUT; i++)
            {
                try {
                    sendUDPTestMessage(new UDPTestMessage(localConnectionId));
                    if (this.remoteConnectionId != null) break;
                    Thread.sleep(1000);
                } catch (Exception e) {}
            }
			if (this.remoteConnectionId == null){
				log.error(" " + getActivityName() + " Timeout waiting for remote ID");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
						ActivityEvent.Type.FAILED,
						"Timeout waiting for remote ID"));
				testFailed();
				return;
			} else {
				log.debug("Local connection ID [" + localConnectionId + "]");
				log.debug("Remote connection ID [" + remoteConnectionId + "]");
			}
			
			//add new UDP connection and start next test 
			this.udpTester.addConnection(this);
			this.udpTester.resetRunningTest();
		} else {
			log.warn("running test != this");
		}
	}*/
	
	private void testFailed()
    {
		this.status = Status.CLOSING;
		this.udpTester.resetRunningTest();
	}
	
	private void sendUDPTestMessage(UDPTestMessage udpTestMessage) throws CommunicationFailedException
    {
		this.udpTester.sendUDPTestMessage(udpTestMessage);
	}
	
	void close()
    {
		log.info("Received stop signal, closing connection");
		this.status = Status.CLOSING;
		this.udpTester.removeConnection(this);
	}

	void receivedUDPTestMessage(UDPTestMessage udpm)
    {
		if(this.status == Status.INIT)
        {
			if(udpm.type == UDPTestMessage.Type.MAPPED_ADDRESS)
            {
                if (udpm.mappedAddress == null) return;
				this.remoteMappedAddress = udpm.mappedAddress;
				this.remotePortMappingRule = udpm.portMappingRule;
				this.status = Status.GOT_MAPPED_ADDRESS;
			}
            else
            {
				log.warn("Illegal message type at this moment [" + udpm.type + "]"
							+ " status [" + this.status + "]");
			}
		}
        else if (this.status == Status.GOT_MAPPED_ADDRESS)
        {
			if (udpm.type == UDPTestMessage.Type.RECEIVED_PING)
            {
				this.status = Status.CONNECTION_ESTABLISHED;
			}
            else if (udpm.type == UDPTestMessage.Type.MAPPED_ADDRESS)
            {
				this.remoteMappedAddress = udpm.mappedAddress;
			}
            else
            {
				log.warn(" " + getName() 
							 + " Illegal message type at this moment [" 
							 + udpm.type + "]"
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
						 + udpm.type + "]"
						 + " status [" + this.status + "]");
		}
	}
	
	UUID getConnectionId() { return this.localConnectionId; }
	
	Status getStatus() { return this.status;	}
	
	public InetSocketAddress getMappedAddress() { return this.localMappedAddress; }

    private String name = null;
    private void setName(String n) { name = n; }
    private String getName() { return name; }
	public String getActivityName() { return getName(); }

	public Activity getParentActivity() { return this.udpTester; }
	
	public void run()
    {
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
	
    private Boolean synLock = new Boolean(true);
    private Integer synGen = null;
    private byte[] bytesToSend = null;
    private CommunicationFailedException sendException = null;
	//Main Send Bytes method
    private void send(final byte[] bytes) throws CommunicationFailedException
    {
        log.debug("aquire synLock send()");
        synchronized (synLock)
        {
    		//first send the SYN packet to initialize data transfer
    		this.status = Status.SENDING;
            synGen = new Random(F2FComputing.getLocalPeer().getID().getLeastSignificantBits()+System.currentTimeMillis()).nextInt();
            bytesToSend = bytes;
    		
            byte[] integer = intToBytes(synGen);
            UDPPacket content = new UDPPacket(UDPPacket.SYN, integer);
            try
            {
                DatagramPacket sendPacket = createDatagramPacketOut(content);
                log.debug("Send SYN..."+synGen);
                sendFromLocalSocket(sendPacket);
    			log.debug("Sent SYN: " + content);
    		} catch (IOException e){
    			log.debug("Unable to send SYN packet", e);
    			this.status = Status.CLOSING;
    			throw new CommunicationFailedException(e);
    		}
            
            // wait until the data has been transfered
            // the Listener thread sends the data after SYN-ACK has been received
    		try
            {
    		    log.debug("synLock.wait()...");
                synLock.wait();
                log.debug("synLock.wait() ended");
            } catch (InterruptedException e){}
            synGen = null;
            bytesToSend = null;
            if (sendException != null) throw sendException;
        }
        log.debug("release synLock send()");
	}
    
    public static int bytesToInt(byte[] bytes)
    {
    	if (bytes.length != 4) return 0;
    	int intValue = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            intValue += (bytes[i] & 0x000000FF) << shift;
        }
        return intValue;
    }
    public static byte[] intToBytes(int intValue)
    {
    	byte[] byteValue = new byte[4]; 
    	byteValue[0]=(byte)((intValue & 0xff000000)>>>24);
    	byteValue[1]=(byte)((intValue & 0x00ff0000)>>>16);
    	byteValue[2]=(byte)((intValue & 0x0000ff00)>>>8);
    	byteValue[3]=(byte)((intValue & 0x000000ff));
    	return byteValue;
    }

    ConcurrentLinkedQueue<UDPPacket> packetQueue = new ConcurrentLinkedQueue<UDPPacket>();
    private void listen()
    {
    	setName("UDP Connection [" + localConnectionId.toString() + "]");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
				ActivityEvent.Type.CHANGED,
				"ID [" + localConnectionId.toString() + "] listening for incoming packets"));
		log.info("UDP Connection established, ID [" + localConnectionId.toString() + "]");
		log.debug("Listening for incoming packets");
		udpTester.addConnection(this);
		//try to set socket timeout to 0
    	setLocalSocketTimeout(0);
		while (this.status != Status.CLOSING)
		{					
			byte[] buffer = new byte[UDPPacket.MAX_PACKET_SIZE];
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			UDPPacket content = null;
			//try to receive
			try {
                log.debug("UDP SOCKET receive ...");
            	localSocket.receive(receivePacket);
    			content = new UDPPacket(receivePacket.getData());
    			if (content == null) continue;
    			log.debug("UDP SOCKET received: " + content);
    			if (content.getType() == UDPPacket.PING) continue;
        		synchronized (packetQueue)
				{
					packetQueue.add(content);
					packetQueue.notifyAll();
				}
			} catch (SocketTimeoutException e) {
				log.warn("UDP listening stopped for SocketTimeoutException");
				continue;
			} catch (IOException e) {
				log.error("Unable to receive packet", e);
				continue;
			} catch (UDPPacketParseException e) {
				log.warn("Received not a UDPPacket",e);
				continue;
			} catch (UDPPacketHashException e) {
                log.warn("Received a UDPPacket with wrong hash",e);
                continue;
            }
		}
	}
    
    private UDPPacket receivePacket() throws IOException, InterruptedException
    {
		synchronized (packetQueue)
		{
			if (packetQueue.isEmpty())
				packetQueue.wait();
			return packetQueue.poll();
		}
    }
	
	private void startMessageHandlerThread()
    {
		new Thread ()
        {
            public void run()
            {
            	while (UDPConnection.this.status != Status.CLOSING)
            	{
            		UDPPacket content = null;
					try {
						content = receivePacket();
					} catch (IOException e) {
						e.printStackTrace();
						UDPConnection.this.status = Status.CLOSING;
					} catch (InterruptedException e) {
						e.printStackTrace();
						UDPConnection.this.status = Status.CLOSING;
					}
            		// at this point we have received something!!!
                    if (content == null) continue;
                    //log.debug("UDP listener received " + content);
                    if (content.getType() == UDPPacket.PING)
                    {
                        continue;
                    }
        			if (content.getType() == UDPPacket.SYN)
                    {
                        synchronized (synLock)
                        {
                        	byte[] integer = content.getData();
                            int remoteGenSyn = bytesToInt(integer);
                            log.debug("received SYN..."+remoteGenSyn);
                            
                            if (synGen == null)
                            {
                                if (!sendSynAck())
                                {
                                	status = Status.CLOSING;
                                	return;
                                }
                            	//try to set socket timeout back to 0
                            	setLocalSocketTimeout(0);
                            	status = Status.IDLE;
                            }
                            else
                            {
                                log.debug("SYN collision. local gen " + synGen);
                                //byte[] integer = content.getData();
                                //log.debug("Collision Generated Int size [" + integer.length + "] content [" + Arrays.toString(integer) + "]");
                                //int remoteGenSyn = bytesToInt(integer);
                                log.debug("SYN collision. remote gen " + remoteGenSyn);
                                if (synGen.intValue() < remoteGenSyn)
                                {
                                	// first receive the remote data
                                    if (!sendSynAck())
                                    {
                                    	status = Status.CLOSING;
                                    	return;
                                    }
                                    //try to set socket timeout back to 0
                                	setLocalSocketTimeout(0);
                                    // then continue to send the local data
                                    new Thread()
                                    {
                                        public void run()
                                        {
                                            try {
                								send(bytesToSend);
                							} catch (CommunicationFailedException e) {
                                                // notify the original sender thread about the exception
                                                UDPConnection.this.status = Status.CLOSING;
                                                UDPConnection.this.sendException = e;
                                                UDPConnection.this.synGen.notifyAll();
                							}
                                        }
                                    }.start();
                                }
                            }
                        }
        			}
                    else if (content.getType() == UDPPacket.SYN_ACK)
                    {
                		log.debug("Received SYN-ACK");
                		log.debug("Sending data [" + Arrays.toString(bytesToSend) + "]");
                    	if (send(bytesToSend,0,bytesToSend.length,false,false))
                			status = Status.IDLE;
                		else 
                			status = Status.CLOSING;
                		log.debug("aquire synGen");
                        synchronized (synLock)
                        {
                            // release the the original sender thread 
                            synLock.notifyAll();
                        }
                        //try to set socket timeout back to 0
                    	setLocalSocketTimeout(0);
                    }
            	}
            }
        }.start();
    }
	
	private void startPingThread()
	{
		new Thread ()
        {
            public void run()
            {
            	while (UDPConnection.this.status != Status.CLOSING)
            	{
			        try
	                {
			        	Thread.sleep(10000);
			        	//log.debug("Status Before sending ID-PING [" + status + "]");
			        	if (UDPConnection.this.status == Status.IDLE)
			        	{
			        		//log.debug("Send PING...");
                            sendFromLocalSocket(createDatagramPacketOut(new UDPPacket(UDPPacket.PING)));
			        		//log.debug("Sent PING");
			        	}
	                } catch (Exception e)
	                {
	                    //log.warn("error Sending ID-PING ...", e);
	                }
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

	private Boolean sendLock = new Boolean(true);
	private void sendFromLocalSocket(DatagramPacket packet) throws IOException
    {
		log.debug("aquire  sendLock ...");
		synchronized (sendLock)
		{
			log.debug("UDP SOCKET send ...");
	        localSocket.send(packet);
			log.debug("UDP SOCKET sent");
		}
    }

    private boolean sendSynAck()
    {
        this.status = Status.RECEIVING;
        log.debug("Received SYN");
        //send confirmation
        try {
            DatagramPacket sendPacket = createDatagramPacketOut(new UDPPacket(UDPPacket.SYN_ACK));
            log.debug("Send SYN-ACK...");
            sendFromLocalSocket(sendPacket);
            log.debug("Sent SYN-ACK");
        } catch (IOException e) {
            log.error("Unable to send SYN_ACK", e);
            return false;
        }
        
        byte[] receivedBytes = receiveData();
        if (receivedBytes == null) return false;
        
        log.debug("Received bytes [" + Arrays.toString(receivedBytes) + "]");
        
        // deserialize the data and 
        // forward received object to the Core
        try {
			byte[] raw_msg = Util.unzip(receivedBytes);
			final Object message = Util.deserializeObject(raw_msg);
            // run in separate thread, because messageReceived() might want to 
            // send something out (BlockingReply), but then no thread is listening
            // on the UDP port
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        messageReceived(message, UDPConnection.this.udpTester.getRemotePeer().getID());
                    } catch (CommunicationFailedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }.start();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
        
        return true;
    }

    private DatagramPacket createDatagramPacketOut(UDPPacket content) throws SocketException
    {
        return new DatagramPacket(content.getBytes(), content.getBytes().length, 
                this.remoteMappedAddress);
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
		while (this.status != Status.CLOSING)
		{
			// Try to send packet
			try {
				UDPPacket content = new UDPPacket(bytes, offset, length, hasMore);
				DatagramPacket sendPacket = createDatagramPacketOut(content);
				log.debug("Sending data: "+ content.toString());
				sendFromLocalSocket(sendPacket);
			} catch (UDPPacketParseException e) {
				log.error("Unable to send [" + length + "] bytes ["
						+ e.getMessage() + "]");
				return false;
			} catch (SocketException e) {
				log.error("Unable to send [" + length + "] bytes", e);
			    return false;
			} catch (IOException e) {
				log.error("Unable to send [" + length + "] bytes", e);
			    return false;
			}			
			//set socket timeout
			// we wait 1 second for ACK/NAK
			setLocalSocketTimeout(1000);
			
			// wait for answer
			//byte[] buffer = new byte[UDPPacket.HASH_LENGTH + 1 + 4];
			//DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			UDPPacket content = null;
			try{
				content = receivePacket();
				//content = new UDPPacket(receivePacket.getData());
			} catch (SocketTimeoutException e) {
				log.warn("Timeout waiting for ACK");
				if (length == 1)
                {
                    return false;
                }
				return send(bytes,offset,length,hasMore,true);
			} catch (Exception e){
				log.error("Unable to receive ACK/NAK", e);
				return false;
			}
			
			if(content != null)
            {
                if (content.getType() == UDPPacket.ACK)
                {
                    return true;
			    }
                if (content.getType() == UDPPacket.NAK)
                {
				    return send(bytes, offset, length, hasMore, true);
			    }
            }
			else
			{
				log.error("Received NULL instead of ACK/NAK");
				return false;
			}
		}
		return false;
	}
	
	private byte[] receiveData()
	{
		//Final data array
		byte[] returnData = new byte[0];
		//byte[] pData = new byte[UDPPacket.MAX_PACKET_SIZE];
		//DatagramPacket receivePacket = new DatagramPacket(pData, pData.length);
		while (this.status != Status.CLOSING)
		{
			//try to set socket timeout
			setLocalSocketTimeout(0);

			UDPPacket udpp = null;
            UDPPacket response = null;
			//try to receive
			try{
				udpp = receivePacket();
				//log.debug("Received [" + Arrays.toString(udpp.getBytes()) + "]");

                if (udpp != null)
                {
                    //append received data
                    returnData = mergeByteArrays(returnData, udpp.getData());
                    response = new UDPPacket(UDPPacket.ACK);
                    log.debug("respond ACK ...");
                }
                else
                {
                    response = new UDPPacket(UDPPacket.NAK);
                    log.debug("respond NAK ...");
                }
            }
            catch (Exception e)
            {
            	log.warn(e);
                response = new UDPPacket(UDPPacket.NAK);
                log.debug("respond NAK ...");
            }
			
			//try send the response
			try {
				sendFromLocalSocket(createDatagramPacketOut(response));
				log.debug("responded");
			} catch (IOException e){
				log.error("Unable to send response",e);
				//errors++;
                return null;
			}
			if (response.getType() == UDPPacket.ACK && !udpp.hasMore())
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
		if (this.status != Status.CONNECTION_ESTABLISHED)
        {
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
					ActivityEvent.Type.FINISHED,
					"UDP test failed"));
			log.warn(" Test Failed");
			testFailed();
		}
        else
        {
			//connection established, start listening
			//exchangeID();
            this.status = Status.IDLE;
            ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
                    ActivityEvent.Type.CHANGED,
                    "started to listen"));
            startPingThread();
            startMessageHandlerThread();
            listen();
            close();
            ActivityManager.getDefault().emitEvent(new ActivityEvent(this,
                    ActivityEvent.Type.CHANGED,
                    "stopped to listen"));
		}
	}
	
	private Integer coneToSymPortRangePing = 0;
	private Integer attackOnRemotePort = 0;
	
    private boolean holePunchTimeout = false;
	private void punchHole()
    {
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
		
		final int AFTER_CONNECTION_ESTABLISHED_PING_AMOUNT = 3;
		
		Thread udpListener = new Thread()
        {
			public void run()
            {
				byte[] receiveContent = new byte[UDPPacket.HASH_LENGTH + 1 + 4];
				while(  !UDPConnection.this.holePunchTimeout && 
                        UDPConnection.this.status != Status.CLOSING
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
							
							if (!(UDPConnection.this.remoteMappedAddress.getAddress().equals(receivePacket.getAddress()) &&
                                  UDPConnection.this.remoteMappedAddress.getPort() == receivePacket.getPort())){	
								//multiple subnetworks case
								//ping received from different remote address
								//change the target IP and port for ping
                                UDPConnection.this.remoteMappedAddress = new InetSocketAddress(receivePacket.getAddress(),
																		receivePacket.getPort());
								//change the currently used target port
								//stop the range attack
								synchronized (UDPConnection.this.attackOnRemotePort ) {
                                    UDPConnection.this.attackOnRemotePort = receivePacket.getPort();
								}
								synchronized (UDPConnection.this.coneToSymPortRangePing) {
                                    UDPConnection.this.coneToSymPortRangePing = 0;
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
			attackOnRemotePort = attackOnRemotePort + this.remotePortMappingRule;
		}
		for (int pingsAfterTraversal = 0, ping_counter = 0, port_increment_counter = 0; 
			   pingsAfterTraversal < AFTER_CONNECTION_ESTABLISHED_PING_AMOUNT &&
			   !holePunchTimeout && 
			   status != Status.CLOSING; 
			 ping_counter++)
        {
			byte[] sendContent = (new UDPPacket(UDPPacket.PING)).getBytes();
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
                    holePunchTimeout = true;
				}
			} catch (SocketException e) {
				log.error(getActivityName() +  " " + e.getMessage(),e);
			}
			if (sendPacket == null) continue;
			
			try
            {
				localSocket.send(sendPacket);
				log.debug("Sent PING packet to ["
						   + sendPacket.getAddress().getHostAddress()
						   + " "
						   + sendPacket.getPort()
						   +"]");
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
			log.debug("STUN SERVER MAPPED ADDRESS REQUEST >>>>>");
			soc.receive(receivePacket);
			log.debug("STUN SERVER MAPPED ADDRESS REQUEST <<<<<");
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
	
	@SuppressWarnings("unused")
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
    
    @SuppressWarnings("serial")
    private class UDPPacketHashException extends Exception
    {
        UDPPacketHashException(UDPPacket packet){
            super("Hash failure: " + packet.toString());
        }
    }
	
	private class UDPPacket
	{
		/* Packet Structure
		 * |-------------|------------------------------------------------|
		 * |    HASH     |  TYPE / MORE  |DATA LENGTH |  DATA             |
		 * |  16 bytes   |  1 byte       | 4 bytes    |  MAX_MESSAGE_SIZE |
		 */
		
        private final static byte ACK = 6;		//ACK if successfully received
        private final static byte NAK = 7;		//NAK if failed
        private final static byte SYN = 8;		//SYN initializes transfer
        private final static byte SYN_ACK = 9;	//SYN-ACK confirms initialization
        private final static byte PING = 10;    //hole punching ping
		
        private final static int HASH_LENGTH = 16;
		
        private final static int MAX_PACKET_SIZE = 65507;
        private final static int MAX_MESSAGE_SIZE = MAX_PACKET_SIZE - HASH_LENGTH - 1 - 4;//1 for TYPE/MORE byte, 4 for length
		
        private byte[] bytes = null;
		
        // constructors for outgoing packet
        private UDPPacket(byte type) {
			bytes = new byte[HASH_LENGTH +1+4];
			setType(type);
            setDataLenght(0);
			setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
            if (!checkHash())
            {
                log.error("HASH is wrong!");
            }
		}
		private UDPPacket(byte[] data, int offset, int length, boolean more) 
							throws UDPPacketParseException 
        {
			if (length == 0 || data == null || data.length == 0) 
				throw new UDPPacketParseException("No data");
			bytes = new byte[HASH_LENGTH + 1 + 4 + length];
			if (more) setType(ACK);
            else setType(NAK);
            setDataLenght(length);
            setData(data, offset, length);
			setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
            if (!checkHash())
            {
                log.error("HASH is wrong!");
            }
		}
        private UDPPacket(byte type, byte[] data) 
        {
            bytes = new byte[HASH_LENGTH + 1 + 4 + data.length];
            setType(type);
            setData(data, 0, data.length);
            setDataLenght(data.length);
            setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
            if (!checkHash())
            {
                log.error("HASH is wrong!");
            }
        }
		
        // constructor of incoming packet
        private UDPPacket(byte[] bytes) throws UDPPacketParseException, UDPPacketHashException
        {
        	//log.debug("forming UDPPacket: "+ Arrays.toString(bytes));
            // check the message size
			if (bytes.length < (MAX_PACKET_SIZE - MAX_MESSAGE_SIZE)) 
				throw new UDPPacketParseException("Message too Short");
            // check the TYPE field
			if (bytes[HASH_LENGTH] < ACK || bytes[HASH_LENGTH] > PING) 
            {
                //log.error("received packet with wrong TYPE field: "+bytes[HASH_LENGTH]);
                //log.debug(Arrays.toString(bytes));
				throw new UDPPacketParseException("Invalid TYPE Field");
            }
			int size = bytesToInt(getSubSequence(bytes, HASH_LENGTH+1, 4));
			if (size > MAX_MESSAGE_SIZE)
				throw new UDPPacketParseException("Data too long, " + size);
			this.bytes = getSubSequence(bytes, 0, HASH_LENGTH+1+4+size);

            // check the hash
            if (!checkHash())
            {
                throw new UDPPacketHashException(this);
            }
        }
		
        private void setDataLenght(int i)
        {
			byte[] lenght = intToBytes(i);
			setBytes(lenght, 0, 4, HASH_LENGTH + 1);
		}
        private int getDataLenght()
        {
        	return bytesToInt(getSubSequence(bytes, HASH_LENGTH+1, 4));
        }
		
        private boolean hasMore()
        {
			return bytes[HASH_LENGTH] == ACK;
		}
		
        private byte getType(){
			return bytes[HASH_LENGTH];
		}
 		
        private byte[] getBytes(){
			return bytes;	
		}
		
        private byte[] getHash(){
			return getBytes(0,HASH_LENGTH);
		}
		
        private byte[] getData(){
			return getBytes(HASH_LENGTH + 1 + 4, getDataLenght());
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
			sb.append("]\nData: [" + getDataLenght() + "] bytes\n");
			sb.append("MD5 hash [" + byteArrayToHexString(bytes, 0, HASH_LENGTH) + "]\n");
			sb.append("Total [" + bytes.length + "] bytes\n");
			
			return sb.toString();
		}
		
        private boolean checkHash(){
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
			setBytes(data, offset, length, HASH_LENGTH + 1 + 4);
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

    public void sendMessage(Object message)throws CommunicationFailedException
    {
        try
        {
            // serialize message
            byte[] raw_msg = Util.serializeObject(message);
            // compress message
            raw_msg = Util.zip(raw_msg);
            this.send(raw_msg);
        }
        catch (IOException e)
        {
            throw new CommunicationFailedException(e);
        }
    }
}