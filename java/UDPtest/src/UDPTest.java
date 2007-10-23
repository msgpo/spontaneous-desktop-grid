import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class UDPTest
{
	/**
	 * @param args
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException 
	{
		//new UDPTestThread();
		new UDPTestMsgs();
	}
}

class UDPTestMsgs
{
	final static byte ACK = 6;
	final static byte NAK = 21;
	final static int HASH_LENGTH = 16;
	final static int MAX_PACKET_SIZE = 65507;
	final static int MAX_MESSAGE_SIZE = MAX_PACKET_SIZE - HASH_LENGTH - 1;//one for MORE byte  
	final static String HASH_ALGORITHM = "MD5";
	
	private DatagramSocket socket = new DatagramSocket();
	byte[] msg = null, hash = new byte[HASH_LENGTH], data = new byte[MAX_PACKET_SIZE];// this is the maximum size
	DatagramPacket packetOut = null, packetIn = new DatagramPacket(data, MAX_PACKET_SIZE);
	InetSocketAddress addr = UDPTestThread.getAddrFromString("hades.at.mt.ut.ee:4445");
	private static MessageDigest md = null;
	byte[][] msgs = 
		new byte[][]
	    {
			new byte[1],
			new byte[2],
			new byte[4],
			new byte[8],
			new byte[16],
			new byte[32],
			new byte[64],
			new byte[128],
			new byte[256],
			new byte[512],
			new byte[1024],
			new byte[2048],
			new byte[4096],
			new byte[8192],
			new byte[16384],
			new byte[32768],
			new byte[MAX_PACKET_SIZE-HASH_LENGTH-1],
			new byte[MAX_PACKET_SIZE+HASH_LENGTH]
	    };
	String str = null;
	
	private static void sendMessage(byte[] msg, SocketAddress addr, DatagramSocket socket) throws IOException
	{
		sendMessage(msg, addr, socket, false);
	}
	private static void sendMessage(byte[] msg, SocketAddress addr, DatagramSocket socket, boolean more) throws IOException
	{
		if (msg.length > MAX_MESSAGE_SIZE)
		{
			int half_size1 = (int)Math.floor((double)msg.length / (double)2);
			byte[] half_msg = new byte[half_size1];
			for (int i = 0; i < half_size1; i++) half_msg[i] = msg[i];
			sendMessage(half_msg, addr, socket, true);
			int half_size2 = (int)Math.ceil((double) msg.length / (double)2);
			half_msg = new byte[half_size2];
			for (int i = 0; i < half_size2; i++) half_msg[i] = msg[i + half_size1];
			sendMessage(half_msg, addr, socket, more);
			return;
		}
		byte [] hash = md.digest(msg);
		byte [] data = new byte[1 + HASH_LENGTH + msg.length];
		data[0] = more ? ACK : NAK;
		for (int i = 1; i <= HASH_LENGTH; i++) data[i] = hash[i-1];
		for (int i = 1; i <= msg.length; i++) data[i+HASH_LENGTH] = msg[i-1];
		DatagramPacket packetOut = new DatagramPacket(data, data.length, addr);
		DatagramPacket packetIn = new DatagramPacket(new byte[1], 1); 
		while (true)
		{
			System.out.println("send " + msg.length + "...");
			socket.send(packetOut);
			// check if message was received correctly
			socket.setSoTimeout(5000);
			try {
				socket.receive(packetIn);
			}
			catch (SocketTimeoutException ex)
			{
				System.out.println("timeout");
				// if acknoledgment is not received in 5 seconds
				// send this packet in 2 pieces
				int half_size1 = (int)Math.floor((double)msg.length / (double)2);
				byte[] half_msg = new byte[half_size1];
				for (int i = 0; i < half_size1; i++) half_msg[i] = msg[i];
				sendMessage(half_msg, addr, socket, true);
				int half_size2 = (int)Math.ceil((double) msg.length / (double)2);
				half_msg = new byte[half_size2];
				for (int i = 0; i < half_size2; i++) half_msg[i] = msg[i + half_size1];
				sendMessage(half_msg, addr, socket, more);
				return;
			}
			if (packetIn.getData()[0] == ACK)
			{
				System.out.println("received ACK");
				// all done
				return;
			}
			else if (packetIn.getData()[0] == NAK)
				System.out.println("received NACK");
			else
				System.out.println("received unknown answer of size " + packetIn.getLength());
		}
	}
	
	private static SocketAddress inAddr = null;
	private static byte[] receiveMessage(DatagramSocket socket) throws IOException
	{
		byte[] hash = new byte[HASH_LENGTH], data = new byte[MAX_PACKET_SIZE];// this is the maximum size of incoming package
		DatagramPacket packetIn = new DatagramPacket(data, MAX_PACKET_SIZE);
		byte[] ret = new byte[0];
		while (true)
		{
			System.out.print("receive... ");
			socket.setSoTimeout(0);
			socket.receive(packetIn);
			data = packetIn.getData();
			boolean more = data[0] == ACK;
			for (int i = 1; i <= HASH_LENGTH; i++) hash[i-1] = data[i];
			String str = new String(data, HASH_LENGTH+1, packetIn.getLength()-HASH_LENGTH-1);
			byte[] msg = str.getBytes();
			System.out.println(str.length());
			if (MessageDigest.isEqual(hash, md.digest(msg)))
			//if (rnd.nextInt() > 0)
			{
				System.out.println("Hash OK");
				data = new byte[]{ACK};
				inAddr = packetIn.getSocketAddress();
				byte[] new_ret = new byte[ret.length+msg.length];
				for (int i = 0; i < ret.length; i++) new_ret[i] = ret[i];
				for (int i = 0; i < msg.length; i++) new_ret[i+ret.length] = msg[i];
				ret = new_ret;
			}
			else
			{
				System.out.println("Hash FAIL");
				data = new byte[]{NAK};
			}
			// responce
			DatagramPacket packetOut = new DatagramPacket(data, data.length, packetIn.getSocketAddress());
			socket.send(packetOut);
			if (data[0] == ACK && !more) return ret;
		}
	}
	
	UDPTestMsgs() throws IOException, NoSuchAlgorithmException 
	{
		md = MessageDigest.getInstance(HASH_ALGORITHM);
		System.out.print("Enter 'C' (client) or 'S' (server): ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		str = br.readLine();
		//Random rnd = new Random(System.currentTimeMillis()+str.hashCode());
		if (str.equals("C") || str.equals("c"))
		{
			initializeMsgs(msgs);
			System.out.println("client");
			for (byte[] msg: msgs)
			{
				System.out.println("\n");
				// send a message
				sendMessage(msg, addr, socket);
				// receive the same message
				receiveMessage(socket);
			}
		}
		else
		{
			System.out.println("server");
			socket = new DatagramSocket(addr);
			while (true)
			{
				System.out.println("\n");
				// receive a message
				msg = receiveMessage(socket);
				// send the message back
				sendMessage(msg, inAddr, socket);
			}
		}
	}
	
	private static void initializeMsgs(byte[][] msgs)
	{
		for (byte[] msg: msgs)
			for (byte b: msg) 
				b = 'A';
	}
}

class UDPTestThread
{
	private DatagramSocket socket = new DatagramSocket();
	//byte[] buf = new byte[socket.getReceiveBufferSize()-28];
	byte[] bufstr, bufhash, buf = new byte[65507];// this is the maximum size
	DatagramPacket packetOut = null, packetIn = new DatagramPacket(buf, buf.length);
	final byte ACK = 6, NAK = 21;
	
	InetSocketAddress addr = getAddrFromString("localhost:4445");
	MessageDigest md = null;
	
	UDPTestThread() throws IOException, NoSuchAlgorithmException 
	{
		md = MessageDigest.getInstance("MD5");
		System.out.print("Enter 'C' (client) or 'S' (server): ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String str = br.readLine();
		if (str.equals("C") || str.equals("c"))
		{
			System.out.println("client");
			System.out.print("Enter message to server: ");
			str = br.readLine();
			// data
			bufstr = str.getBytes();
			//System.out.println("String: "+toHex(bufstr));
			// hash
			bufhash = md.digest(bufstr);
			//System.out.println("Hash: "+toHex(bufhash));
			// data+hash
			buf = new byte[bufstr.length + 16];
			for (int i = 0; i < 16; i++) buf[i] = bufhash[i];
			for (int i = 0; i < bufstr.length; i++) buf[i+16] = bufstr[i];
			packetOut = new DatagramPacket(buf, buf.length, addr);
			// send
			while (true)
			{
				System.out.println("send");
				socket.send(packetOut);
				// check if message was received correctly
				socket.setSoTimeout(5000);
				try {
					socket.receive(packetIn);
				}
				catch (SocketTimeoutException ex)
				{
					System.out.println("timeout");
					// send again if acknoledgment is not received in 5 seconds
					continue;
				}
				if (packetIn.getData()[0] == ACK)
				{
					System.out.println("received ACK");
					break;
				}
				else if (packetIn.getData()[0] == NAK)
					System.out.println("received NACK");
				else
					System.out.println("received unknown answer of size " + packetIn.getLength());
			}
			
			System.out.print("Server received the message!");
		}
		else
		{
			System.out.println("server");
			System.out.println("Waiting for a message from client...");
			socket = new DatagramSocket(addr);
			socket.receive(packetIn);
			buf = packetIn.getData();
			bufhash = new byte[16];
			for (int i = 0; i < 16; i++) bufhash[i] = buf[i];
			//System.out.println("Hash: "+toHex(bufhash));
			str = new String(buf, 16, packetIn.getLength()-16);
			bufstr = str.getBytes();
			//System.out.println("String: "+toHex(bufstr));
			System.out.println(str);
			System.out.println(str.length());
			if (MessageDigest.isEqual(bufhash, md.digest(bufstr)))
			{
				System.out.println("OK");
				buf = new byte[]{ACK};
			}
			else
			{
				System.out.println("FAIL");
				buf = new byte[]{NAK};
			}
			// respond with ACK
			packetOut = new DatagramPacket(buf, buf.length, packetIn.getSocketAddress());
			socket.send(packetOut);
		}
	}
	
	private static String toHex(byte[] bytes)
	{
		String result = "";
		for (byte b: bytes) result += Integer.toHexString(b);
		return result;
	}
	
	static InetSocketAddress getAddrFromString(String str)
	{
		// Parameter checking.
		if (str==null || (str=str.trim()).equals("") || str.indexOf(":")<=0)
			return null;
		String[] strSplit = str.split(":");
		if (strSplit.length!=2) return null;
		InetSocketAddress interAddrRet = null;
		try
		{
			interAddrRet = new InetSocketAddress(strSplit[0], Integer.parseInt(strSplit[1]));
		}
		catch (Exception e)
		{
			interAddrRet = null;
		}
		return interAddrRet;
	}
}
