import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
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
	final static String HASH_ALGORITHM = "MD5";
	
	private DatagramSocket socket = new DatagramSocket();
	byte[] msg = null, hash = new byte[HASH_LENGTH], data = new byte[MAX_PACKET_SIZE];// this is the maximum size
	DatagramPacket packetOut = null, packetIn = new DatagramPacket(data, MAX_PACKET_SIZE);
	InetSocketAddress addr = UDPTestThread.getAddrFromString("localhost:4445");
	MessageDigest md = null;
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
			new byte[MAX_PACKET_SIZE-HASH_LENGTH],
			new byte[MAX_PACKET_SIZE+HASH_LENGTH]
	    };
	String str = null;
	
	UDPTestMsgs() throws IOException, NoSuchAlgorithmException 
	{
		md = MessageDigest.getInstance(HASH_ALGORITHM);
		System.out.print("Enter 'C' (client) or 'S' (server): ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		str = br.readLine();
		Random rnd = new Random(System.currentTimeMillis()+str.hashCode());
		if (str.equals("C") || str.equals("c"))
		{
			initializeMsgs(msgs);
			System.out.println("client");
			for (byte[] msg: msgs)
			{
				System.out.println("\n" + msg.length);
				System.out.println("send");
				// send a message
				hash = md.digest(msg);
				data = new byte[msg.length + HASH_LENGTH];
				for (int i = 0; i < HASH_LENGTH; i++) data[i] = hash[i];
				for (int i = 0; i < msg.length; i++) data[i+HASH_LENGTH] = msg[i];
				packetOut = new DatagramPacket(data, data.length, addr);
				while (true)
				{
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
				
				// receive the same message
				while (true)
				{
					System.out.println("receive");
					socket.setSoTimeout(0);
					socket.receive(packetIn);
					data = packetIn.getData();
					for (int i = 0; i < HASH_LENGTH; i++) hash[i] = data[i];
					str = new String(data, HASH_LENGTH, packetIn.getLength()-HASH_LENGTH);
					msg = str.getBytes();
					System.out.println(str.length());
					if (MessageDigest.isEqual(hash, md.digest(msg)))
					//if (rnd.nextInt() > 0)
					{
						System.out.println("Hash OK");
						data = new byte[]{ACK};
					}
					else
					{
						System.out.println("Hash FAIL");
						data = new byte[]{NAK};
					}
					// responce
					packetOut = new DatagramPacket(data, data.length, packetIn.getSocketAddress());
					socket.send(packetOut);
					if (data[0] == ACK) break;
				}
			}
		}
		else
		{
			System.out.println("server");
			socket = new DatagramSocket(addr);
			while (true)
			{
				// receive a message
				socket.setSoTimeout(0);
				socket.receive(packetIn);
				data = packetIn.getData();
				for (int i = 0; i < HASH_LENGTH; i++) hash[i] = data[i];
				str = new String(data, HASH_LENGTH, packetIn.getLength()-HASH_LENGTH);
				msg = str.getBytes();
				System.out.println(str.length());
				if (MessageDigest.isEqual(hash, md.digest(msg)))
				//if (rnd.nextInt() > 0)
				{
					System.out.println("Hash OK");
					data = new byte[]{ACK};
				}
				else
				{
					System.out.println("Hash FAIL");
					data = new byte[]{NAK};
				}
				// responce
				packetOut = new DatagramPacket(data, data.length, packetIn.getSocketAddress());
				socket.send(packetOut);
				if (data[0] == NAK) continue;
				
				// send the message back
				hash = md.digest(msg);
				data = new byte[msg.length + HASH_LENGTH];
				for (int i = 0; i < HASH_LENGTH; i++) data[i] = hash[i];
				for (int i = 0; i < msg.length; i++) data[i+HASH_LENGTH] = msg[i];
				packetOut = new DatagramPacket(data, data.length, packetIn.getSocketAddress());
				while (true)
				{
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
