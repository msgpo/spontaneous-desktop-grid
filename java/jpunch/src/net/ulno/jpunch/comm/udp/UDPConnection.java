package net.ulno.jpunch.comm.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.ulno.jpunch.comm.BlockingMessageSender;
import net.ulno.jpunch.core.CommunicationFailedException;
import net.ulno.jpunch.util.Util;
import net.ulno.jpunch.util.logging.Logger;

public class UDPConnection extends BlockingMessageSender implements Runnable
{
	private final static Logger log = Logger.getLogger(UDPConnection.class);
			
	private DatagramSocket localSocket = null;
	private InetSocketAddress remoteMappedAddress = null;
	
	//F2FPeer remotePeer = null;
	
	private Status status = Status.IDLE;
			
	private enum Status{
        IDLE,
		SENDING,
		RECEIVING,
        CLOSING
	}

	UDPConnection(DatagramSocket localSocket,
				   InetSocketAddress remoteMappedAddress
				   //F2FPeer remotePeer
				  )
	{
		this.localSocket = localSocket;
		this.remoteMappedAddress = remoteMappedAddress;
		//this.remotePeer = remotePeer;
	}
	
    private String name = null;
    private void setName(String n) { name = n; }
    private String getName() { return name; }
	public String getActivityName() { return getName(); }
	
	public void run()
    {
		// just for information catch any exceptions that may occur
		try
		{
            setName("UDP Connection");
    		startPingThread();
            startMessageHandlerThread();
    		listen();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.warn(getActivityName() + e.getMessage());
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
//            localSynTag = F2FComputing.getLocalPeerID().hashCode();
    		localSynTag = this.localSocket.getLocalAddress().getHostName().hashCode();
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
                    
//                    messageReceived(message, remotePeer.getID());
                    
                    log.info("Received Object [" + ((String)message) + "]");
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
	
	private byte[] mergeByteArrays(byte[] bytes1, byte[] bytes2){
		byte[] returnBytes = new byte[bytes1.length + bytes2.length];
		for (int i = 0; i < bytes1.length || i < bytes2.length ;i++){
			if( i < bytes1.length ) returnBytes[i] = bytes1[i];
			if( i < bytes2.length ) returnBytes[bytes1.length + i] = bytes2[i];
		}
		return returnBytes;
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
    
//    private Object receivedObject = null;
/*    
    public synchronized Object receiveMessage(){
    	if (receivedObject == null){
    		try{
    			wait();
    		} catch (InterruptedException e) {}
    		return receivedObject;
    	}
    	return receivedObject;
    }
*/ 
}