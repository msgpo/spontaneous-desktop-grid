package net.ulno.jpunch.comm.udp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.ulno.jpunch.util.JPunchProperties;

import org.apache.log4j.Logger;

class UDPPacket
{
	/* Packet Structure
	 * |-------------|-----------------------------------------------------------|
	 * |    HASH     |  TYPE / MORE  |DATA ID   |DATA LENGTH |  DATA             |
	 * |  16 bytes   |  1 byte       | 4 bytes  | 4 bytes    |  MAX_MESSAGE_SIZE |
	 */
	
    final static byte ACK = 6;		//ACK if successfully received
    final static byte NAK = 7;		//NAK if failed
    final static byte SYN = 8;		//SYN initializes transfer
    final static byte SYN_ACK = 9;	//SYN-ACK confirms initialization
    final static byte PING = 10;    //hole punching ping
	
    final static int HASH_LENGTH = 16;
	
    private byte[] bytes = null;

    private int maxPacketSize;
    private int maxMessageSize;
    
	private MessageDigest md = null;
	private final static Logger log = Logger.getLogger(UDPPacket.class);
	
	private UDPPacket(int maxPacketSize)
	{
		this.maxPacketSize = maxPacketSize;
		this.maxMessageSize = this.maxPacketSize - HASH_LENGTH-1-4-4;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {}
	}
	
	public UDPPacket() {
		this(UDPPacket.getDefaultPacketSize());
	}
	
	public static int getDefaultPacketSize(){
		return JPunchProperties.getIntegerProperty(
				JPunchProperties.UDP_MAX_PACKET_SIZE);
	}
	
    // constructors for outgoing packet
    UDPPacket(byte type)
    {
    	this();
		bytes = new byte[HASH_LENGTH + 1 + 4 + 4];
		setType(type);
        setDataID(0);
        setDataLenght(0);
		setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
        if (!checkHash())
        {
            log.error("HASH is wrong!");
        }
	}
    UDPPacket(int dataID, byte type)
    {
    	this();
        bytes = new byte[HASH_LENGTH + 1 + 4 + 4];
        setType(type);
        setDataID(dataID);
        setDataLenght(0);
        setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
        if (!checkHash())
        {
            log.error("HASH is wrong!");
        }
    }
	UDPPacket(int dataID, byte[] data, int offset, int length, boolean more) 
	{
    	this();
		bytes = new byte[HASH_LENGTH + 1 + 4 + 4 + length];
		if (more) setType(ACK);
        else setType(NAK);
        setDataID(dataID);
        setDataLenght(length);
        setData(data, offset, length);
		setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
        if (!checkHash())
        {
            log.error("HASH is wrong!");
        }
	}
    UDPPacket(byte type, byte[] data) 
    {
    	this();
        bytes = new byte[HASH_LENGTH + 1 + 4 + 4 + data.length];
        setType(type);
        setDataID(0);
        setDataLenght(data.length);
        setData(data, 0, data.length);
        setHash(hashByteArray(bytes, HASH_LENGTH, (bytes.length - HASH_LENGTH)));
        if (!checkHash())
        {
            log.error("HASH is wrong!");
        }
    }
	
    // constructor of incoming packet
    UDPPacket(byte[] bytes) throws UDPPacketParseException, UDPPacketHashException
    {
    	this();
    	//log.debug("forming UDPPacket: "+ Arrays.toString(bytes));
        // check the message size
    	
    	//
		if (bytes.length < (maxPacketSize - maxMessageSize)) 
			throw new UDPPacketParseException("Message too Short");
        // check the TYPE field
		if (bytes[HASH_LENGTH] < ACK || bytes[HASH_LENGTH] > PING) 
        {
            //log.error("received packet with wrong TYPE field: "+bytes[HASH_LENGTH]);
            //log.debug(Arrays.toString(bytes));
			throw new UDPPacketParseException("Invalid TYPE Field");
        }
		int size = UDPConnection.bytesToInt(getSubSequence(bytes, HASH_LENGTH+1+4, 4));
		if (size > maxMessageSize)
			throw new UDPPacketParseException("Data too long, " + size);
		this.bytes = getSubSequence(bytes, 0, HASH_LENGTH+1+4+4+size);

        // check the hash
        if (!checkHash())
        {
            throw new UDPPacketHashException(this);
        }
    }
    
    void setDataID(int i)
    {
        byte[] lenght = UDPConnection.intToBytes(i);
        setBytes(lenght, 0, 4, HASH_LENGTH + 1);
    }
    int getDataID()
    {
        return UDPConnection.bytesToInt(getSubSequence(bytes, HASH_LENGTH+1, 4));
    }
	
    private void setDataLenght(int i)
    {
		byte[] lenght = UDPConnection.intToBytes(i);
		setBytes(lenght, 0, 4, HASH_LENGTH + 1 + 4);
	}
    private int getDataLenght()
    {
    	return UDPConnection.bytesToInt(getSubSequence(bytes, HASH_LENGTH+1+4, 4));
    }
	
    boolean hasMore()
    {
		return bytes[HASH_LENGTH] == ACK;
	}
	
    byte getType(){
		return bytes[HASH_LENGTH];
	}
		
    byte[] getBytes(){
		return bytes;	
	}
	
    private byte[] getHash(){
		return getBytes(0,HASH_LENGTH);
	}
	
    byte[] getData(){
		return getBytes(HASH_LENGTH + 1 + 4 + 4, getDataLenght());
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("UDPPacket :\nType: [");
		if (bytes[HASH_LENGTH] == ACK) {
			sb.append("ACK("+getDataID()+")");
        } else if (bytes[HASH_LENGTH] == NAK){
            sb.append("NAK("+getDataID()+")");
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
		setBytes(data, offset, length, HASH_LENGTH + 1 + 4 + 4);
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
}

@SuppressWarnings("serial")
class UDPPacketParseException extends Exception
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
class UDPPacketHashException extends Exception
{
    UDPPacketHashException(UDPPacket packet){
        super("Hash failure: " + packet.toString());
    }
}
