package ee.ut.f2f.comm.udp;

import java.io.IOException;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
	UDPConnection(DatagramSocket localSocket,
				   InetAddress remoteIp,
				   UDPTester parent)  {
		if (localSocket == null) throw new NullPointerException("localSocket == null");
		if (parent == null) throw new NullPointerException("parent UDPTester == null");
		
		this.localSocket = localSocket;
		this.udpTester = parent;
		this.setName("UDP TEST ");
		this.localConnectionId = UUID.randomUUID();
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
    private Integer localSynTag = null;
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
            localSynTag = F2FComputing.getLocalPeerID().hashCode();
            bytesToSend = bytes;
    		
            byte[] integer = intToBytes(localSynTag);
            while (true)
            {
                try
                {
                    //log.debug("Send SYN..."+synGen);
                    sendFromLocalSocket(new UDPPacket(UDPPacket.SYN, integer));
        			//log.debug("Sent SYN: " + content);
        		} catch (IOException e){
        			log.debug("Unable to send SYN packet", e);
        			status = Status.CLOSING;
                    throw new CommunicationFailedException(e);
                }
                
                // wait until the data has been transfered
                // the Listener thread sends the data after SYN-ACK has been received
                // send SYN again if the thread is interrupted
        		try
                {
        		    log.debug("synLock.wait...");
                    synLock.wait();
                    log.debug("synLock.wait ended");
                    break;
                } catch (InterruptedException e){continue;}
            }
            localSynTag = null;
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

    private LinkedBlockingQueue<byte[]> packetQueue = new LinkedBlockingQueue<byte[]>(Integer.MAX_VALUE);
    private void listen()
    {
    	setName("UDP Connection [" + localConnectionId.toString() + "]");
		log.info("UDP connection established, ID [" + localConnectionId.toString() + "]");
		log.debug("Listening for incoming packets");
		udpTester.addConnection(this);
		// try to set socket timeout to 0
    	setLocalSocketTimeout(0);
        while (status != Status.CLOSING)
        {
            byte[] buffer = new byte[UDPPacket.MAX_PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            //try to receive
            try
            {
                log.debug("UDP SOCKET receive ...");
                localSocket.receive(receivePacket);
                log.debug("UDP SOCKET received");
                packetQueue.put(receivePacket.getData());
            }
            catch (Exception e)
            {
                log.warn("UDP listening", e);
            }
        }
         
	}
    
    private UDPPacket receivePacket()
        throws InterruptedException, UDPPacketParseException, UDPPacketHashException
    {
        return receivePacket(0);
    }
    
    private UDPPacket receivePacket(int timeout)
        throws InterruptedException, UDPPacketParseException, UDPPacketHashException
    {
        byte[] data = null;
        if (timeout <= 0)
            data = packetQueue.take();
        else data = packetQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (data == null) return null;
        UDPPacket packet = new UDPPacket(data);
        log.debug("read "+packet);
        if (packet.getType() == UDPPacket.PING)
            return receivePacket(timeout);
        return packet;
    }
	
    private Thread collisionSendThread = null;
	private void startMessageHandlerThread()
    {
		new Thread ()
        {
            public void run()
            {
            	while (UDPConnection.this.status != Status.CLOSING)
            	{
            		UDPPacket content = null;
					try
                    {
						content = receivePacket();
					}
                    catch (Exception e)
                    {
						UDPConnection.this.status = Status.CLOSING;
                        log.error("Error while handling UDP message: ", e);
					}
            		// at this point we have received something!!!
                    if (content == null) continue;
                    //log.debug("UDP listener received " + content);
                    if (content.getType() == UDPPacket.SYN)
                    {
                        log.debug("aquire synLock SYN");
                        synchronized (synLock)
                        {
                        	if (localSynTag == null)
                            {
                                if (!startReceiving())
                                {
                                	status = Status.CLOSING;
                                	return;
                                }
                            	status = Status.IDLE;
                            }
                            else
                            {
                                log.debug("SYN collision. local gen " + localSynTag);
                                byte[] integer = content.getData();
                                int remoteSynTag = bytesToInt(integer);
                                log.debug("SYN collision. remote gen " + remoteSynTag);
                                if (localSynTag.intValue() < remoteSynTag)
                                {
                                	// first receive the remote data
                                    if (!startReceiving())
                                    {
                                    	status = Status.CLOSING;
                                    	return;
                                    }
                                    // then continue to send the local data
                                    if (collisionSendThread != null)
                                        collisionSendThread.interrupt();
                                    else startCollisionSendThread();
                                }
                            }
                        }
                        log.debug("release synLock SYN");
        			}
                    else if (content.getType() == UDPPacket.SYN_ACK)
                    {
                        log.debug("aquire synLock SYN-ACK");
                        synchronized (synLock)
                        {
                            //log.debug("Received SYN-ACK");
                    		log.debug("Send DATA [" + bytesToSend.length + "] ...");
                        	if (send(bytesToSend,0,bytesToSend.length,false))
                    			status = Status.IDLE;
                    		else
                    			status = Status.CLOSING;
                            log.debug("DATA sent " + status);
                            localSynTag = null;
                            bytesToSend = null;
                            // release the the original sender thread 
                            synLock.notifyAll();
                        }
                        log.debug("release synLock SYN-ACK");
                    }
            	}
            }
        }.start();
    }
	
	private void startCollisionSendThread()
    {
        collisionSendThread = new Thread()
        {
            public void run()
            {
                try {
                    send(bytesToSend);
                    collisionSendThread = null;
                } catch (CommunicationFailedException e) {
                    // notify the original sender thread about the exception
                    UDPConnection.this.status = Status.CLOSING;
                    UDPConnection.this.sendException = e;
                    UDPConnection.this.localSynTag.notifyAll();
                }
            }
        };
        collisionSendThread.start();
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
                            sendFromLocalSocket(new UDPPacket(UDPPacket.PING));
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
	private void sendFromLocalSocket(UDPPacket packet)
        throws IOException
    {
        DatagramPacket datagram = createDatagramPacketOut(packet);
		log.debug("aquire  sendLock ...");
		synchronized (sendLock)
		{
			log.debug("send "+packet);
	        localSocket.send(datagram);
		}
    }

    private boolean startReceiving()
    {
        this.status = Status.RECEIVING;
        log.debug("RECEIVING");
        //send confirmation
        try {
            //log.debug("Send SYN-ACK...");
            sendFromLocalSocket(new UDPPacket(UDPPacket.SYN_ACK));
            //log.debug("Sent SYN-ACK");
        } catch (IOException e) {
            log.error("Unable to send SYN_ACK", e);
            return false;
        }
        
        final byte[] receivedBytes = receiveData();
        if (receivedBytes == null) return false;
        
        log.debug("Received DATA [" + receivedBytes.length + "]");
        
        // deserialize the data and 
        // forward received object to the Core
    	// run in separate thread, because messageReceived() might want to 
        // send something out (BlockingReply), but then no thread is handling
        // incoming UDP messages
        new Thread()
        {
            public void run()
            {
                try
                {
                    byte[] raw_msg = Util.unzip(receivedBytes);
                    Object message = Util.deserializeObject(raw_msg);
                    
                    messageReceived(message, UDPConnection.this.udpTester.getRemotePeer().getID());
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
		
        return true;
    }

    private DatagramPacket createDatagramPacketOut(UDPPacket content)
        throws SocketException
    {
        return new DatagramPacket(content.getBytes(), content.getBytes().length, 
                this.remoteMappedAddress);
    }

    private int dataOutID = 1;
    private int dataInID = 0;
    //recursive method
	private boolean send(final byte[] bytes, final int offset, final int length, final boolean hasMore)
	{
		//If message is larger then MAX size split in two
		if (length > UDPPacket.MAX_MESSAGE_SIZE) {
			log.debug("Message too large ("+length+"), split in two");
			int half_size = (int)(length/2d);
			return 
				send(bytes, offset, half_size, true) &&
				send(bytes, (offset + half_size), (length - half_size), hasMore);
		}
		log.debug("Sending [" + length + "] bytes");
		while (this.status != Status.CLOSING)
		{
			// Try to send packet
            try
            {
				UDPPacket content = new UDPPacket(dataOutID, bytes, offset, length, hasMore);
				//log.debug("Sending data: "+ content.toString());
				sendFromLocalSocket(content);
			}
            catch (Exception e)
            {
				log.error("Unable to send [" + length + "] bytes", e);
			    return false;
			}
			
			// wait for answer
			//byte[] buffer = new byte[UDPPacket.HASH_LENGTH + 1 + 4];
			//DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			UDPPacket content = null;
			try
            {
                log.debug("wait for ACK/NAK...");
				content = receivePacket(500);
                //log.debug("got " + content);
				//content = new UDPPacket(receivePacket.getData());
			}
            catch (Exception e)
            {
				log.error("Unable to receive ACK/NAK", e);
				return false;
			}
			
            if(content == null)
            {
                log.warn("Timeout waiting for ACK/NAK...resend");
                return send(bytes,offset,length,hasMore);
            }
            else
            {
                if (content.getType() == UDPPacket.ACK)
                {
                    if (content.getDataID() == dataOutID)
                    {
                        dataOutID++;
                        return true;
                    }
                    else
                    {
                        log.warn("ACK for data chunk "+content.getDataID()+", but sent "+dataOutID+"...resend");
                        return send(bytes, offset, length, hasMore);
                    }
			    }
                if (content.getType() == UDPPacket.NAK)
                {
                    log.warn("NAK...resend");
                    return send(bytes, offset, length, hasMore);
			    }
                else
                {
                    log.error("waited for ACK/NAK but got "+content.getType());
                    return false;
                }
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
			UDPPacket received = null;
            UDPPacket response = null;
			//try to receive
			try{
				received = receivePacket();
				//log.debug("Received [" + Arrays.toString(udpp.getBytes()) + "]");

                if (received != null && (received.getType() == UDPPacket.ACK || received.getType() == UDPPacket.NAK))
                {
                    // check if this is the next chunk of data
                    if (received.getDataID() == dataInID + 1)
                    {
                        //append received data
                        dataInID++;
                        returnData = mergeByteArrays(returnData, received.getData());
                    }
                    response = new UDPPacket(dataInID, UDPPacket.ACK);
                    log.debug("respond ACK ...");
                }
                else
                {
                    response = new UDPPacket(dataInID, UDPPacket.NAK);
                    log.debug("respond NAK ...");
                }
            }
            catch (Exception e)
            {
            	log.warn(e);
                response = new UDPPacket(dataInID, UDPPacket.NAK);
                log.debug("respond NAK ...");
            }
			
			//try send the response
			try {
				sendFromLocalSocket(response);
				log.debug("responded");
			} catch (IOException e){
				log.error("Unable to send response",e);
				//errors++;
                return null;
			}
			if (response.getType() == UDPPacket.ACK && !received.hasMore())
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
				byte[] receiveContent = new byte[UDPPacket.HASH_LENGTH + 1 + 4 + 4];
				while(  UDPConnection.this.status != Status.CLOSING
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
		long startTime = System.currentTimeMillis();
		for (int pingsAfterTraversal = 0, ping_counter = 0, port_increment_counter = 0; 
			   pingsAfterTraversal < AFTER_CONNECTION_ESTABLISHED_PING_AMOUNT &&
			   status != Status.CLOSING; 
			 ping_counter++)
        {
			if (System.currentTimeMillis() - startTime > 60000)
			{
				status = Status.CLOSING;
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
					status = Status.CLOSING;
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

    public synchronized void sendMessage(Object message)throws CommunicationFailedException
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