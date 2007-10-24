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
		// initialize static test data 
		TestStuff.initTestStuff();
		
		// launch the needed test
		//new UDPTestMsg();
		//new UDPTestExchangeMsgs();
		//new UDPTestBandwidthWithACK();
		new UDPTestBandwidthWithoutACK();
	}
}

class TestStuff
{
	final static byte ACK = 6;
	final static byte NAK = 21;
	final static int HASH_LENGTH = 16;
	final static int MAX_PACKET_SIZE = 65507;
	final static int MAX_MESSAGE_SIZE = MAX_PACKET_SIZE - HASH_LENGTH - 1;//one for MORE byte  
	final static String HASH_ALGORITHM = "MD5";
	private static MessageDigest md = null;
	static byte[][] msgs = 
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
	
	static void initTestStuff() throws NoSuchAlgorithmException
	{
		md = MessageDigest.getInstance(HASH_ALGORITHM);
		for (byte[] msg: msgs)
			for (byte b: msg) 
				b = 'A';
	}
	
	static String askClientOrServer() throws IOException
	{
		System.out.print("Enter 'c' (client) or 's' (server): ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		return br.readLine();
	}
	
	static void sendMessage(byte[] msg, SocketAddress addr, DatagramSocket socket) throws IOException
	{
		sendMessage(msg, addr, socket, false);
	}

	private static long error_nak = 0;
	private static long error_timeout = 0;
	static void sendMessage(byte[] msg, SocketAddress addr, DatagramSocket socket, boolean more) throws IOException
	{
		if (msg.length > MAX_MESSAGE_SIZE)
		{
			//TODO split the message in a better way
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
	//		System.out.println("send " + msg.length + "...");
			socket.send(packetOut);
			// check if message was received correctly
			socket.setSoTimeout(500);
			try {
				socket.receive(packetIn);
			}
			catch (SocketTimeoutException ex)
			{
				error_timeout++;
	//			System.out.println("timeout");
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
	//			System.out.println("received ACK");
				// all done
				return;
			}
			error_nak++;
	//		else if (packetIn.getData()[0] == NAK)
	//			System.out.println("received NACK");
	//		else
	//			System.out.println("received unknown answer of size " + packetIn.getLength());
		}
	}
	
	static SocketAddress inAddr = null;
	static byte[] receiveMessage(DatagramSocket socket) throws IOException
	{
		byte[] hash = new byte[HASH_LENGTH], data = new byte[MAX_PACKET_SIZE];// this is the maximum size of incoming package
		DatagramPacket packetIn = new DatagramPacket(data, MAX_PACKET_SIZE);
		byte[] ret = new byte[0];
		//Random rnd = new Random(System.currentTimeMillis()+socket.hashCode());
		while (true)
		{
	//		System.out.print("receive... ");
			socket.setSoTimeout(0);
			socket.receive(packetIn);
			data = packetIn.getData();
			boolean more = data[0] == ACK;
			for (int i = 1; i <= HASH_LENGTH; i++) hash[i-1] = data[i];
			String str = new String(data, HASH_LENGTH+1, packetIn.getLength()-HASH_LENGTH-1);
			byte[] msg = str.getBytes();
	//		System.out.println(str.length());
			if (MessageDigest.isEqual(hash, md.digest(msg)))
			//if (rnd.nextInt() > 0)
			{
				//System.out.println("Hash OK");
				data = new byte[]{ACK};
				inAddr = packetIn.getSocketAddress();
				byte[] new_ret = new byte[ret.length+msg.length];
				for (int i = 0; i < ret.length; i++) new_ret[i] = ret[i];
				for (int i = 0; i < msg.length; i++) new_ret[i+ret.length] = msg[i];
				ret = new_ret;
			}
			else
			{
				System.out.println("\nHash FAIL\n");
				data = new byte[]{NAK};
			}
			// responce
			DatagramPacket packetOut = new DatagramPacket(data, data.length, packetIn.getSocketAddress());
			socket.send(packetOut);
			if (data[0] == ACK && !more) return ret;
		}
	}

	static long testBandwidthWithACK(byte[] msg, SocketAddress addr, DatagramSocket socket) throws IOException
	{
		// 10M
		System.out.print("Sending 10 MB in total by " +msg.length+ " B ... ");
		final long data_size = 10*1024*1024;
		long sent_data = 0;
		long start = System.currentTimeMillis();
		while (true)
		{
			sendMessage(msg, addr, socket);
			sent_data += msg.length;
			if (sent_data >= data_size) break;
		}
		long end = System.currentTimeMillis();
		System.out.println(error_nak+" NAK " +error_timeout+ " timeout");
		long duration = end - start;
		long bandwidth = data_size/duration;
		String sDuration = duration < 10000 ? ""+duration+" ms" : ""+duration/1000+" s";
		System.out.println("\ttook " + sDuration + ", " + bandwidth + " B/ms");
		return duration;
	}
	
	static void sendMessageWithoutACK(byte[] msg, SocketAddress addr, DatagramSocket socket, boolean more) throws IOException
	{
		if (msg.length > MAX_MESSAGE_SIZE)
		{
			int half_size1 = (int)Math.floor((double)msg.length / (double)2);
			byte[] half_msg = new byte[half_size1];
			for (int i = 0; i < half_size1; i++) half_msg[i] = msg[i];
			sendMessageWithoutACK(half_msg, addr, socket, true);
			int half_size2 = (int)Math.ceil((double) msg.length / (double)2);
			half_msg = new byte[half_size2];
			for (int i = 0; i < half_size2; i++) half_msg[i] = msg[i + half_size1];
			sendMessageWithoutACK(half_msg, addr, socket, more);
			return;
		}
		byte [] hash = md.digest(msg);
		byte [] data = new byte[1 + HASH_LENGTH + msg.length];
		data[0] = more ? ACK : NAK;
		for (int i = 1; i <= HASH_LENGTH; i++) data[i] = hash[i-1];
		for (int i = 1; i <= msg.length; i++) data[i+HASH_LENGTH] = msg[i-1];
		DatagramPacket packetOut = new DatagramPacket(data, data.length, addr);
		socket.send(packetOut);
	}
	
	static long testBandwidthWithoutACK(byte[] msg, SocketAddress addr, DatagramSocket socket) throws IOException
	{
		// 10M
		System.out.println("Sending 10 MB in total by " +msg.length+ " B ... ");
		final long data_size = 10*1024*1024;
		long sent_data = 0;
		long start = System.currentTimeMillis();
		while (true)
		{
			sendMessageWithoutACK(msg, addr, socket, false);
			sent_data += msg.length;
			if (sent_data >= data_size) break;
		}
		long end = System.currentTimeMillis();
		long duration = end - start;
		long bandwidth = data_size/duration;
		String sDuration = duration < 10000 ? ""+duration+" ms" : ""+duration/1000+" s";
		System.out.println("\ttook " + sDuration + ", " + bandwidth + " B/ms");
		return duration;
	}
	
	private static SocketAddress serverAddress = null; 
	static SocketAddress getServerAddress()
	{
		if (serverAddress == null)
			serverAddress = getAddrFromString("hades.at.mt.ut.ee:4445");
		return serverAddress;	
	}

	static SocketAddress getAddrFromString(String str)
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
	
	private static String toHex(byte[] bytes)
	{
		String result = "";
		for (byte b: bytes) result += Integer.toHexString(b);
		return result;
	}
}

class UDPTestBandwidthWithACK
{
	UDPTestBandwidthWithACK() throws IOException 
	{
		System.out.println("Test bandwidth with ACK answer");
		String str = TestStuff.askClientOrServer();
		if (str.equals("C") || str.equals("c"))
		{
			DatagramSocket socket = new DatagramSocket();
			System.out.println("client");
			for (int i = TestStuff.msgs.length-1; i >= 0; i--)
			{
				System.out.println("\n");
				TestStuff.testBandwidthWithACK(TestStuff.msgs[i], TestStuff.getServerAddress(), socket);
			}
		}
		else
		{
			System.out.println("server");
			DatagramSocket socket = new DatagramSocket(TestStuff.getServerAddress());
			while (true)
			{
				TestStuff.receiveMessage(socket);
			}
		}
	}
}

class UDPTestBandwidthWithoutACK
{
	UDPTestBandwidthWithoutACK() throws IOException 
	{
		System.out.println("Test bandwidth without ACK answer");
		String str = TestStuff.askClientOrServer();
		if (str.equals("C") || str.equals("c"))
		{
			DatagramSocket socket = new DatagramSocket();
			System.out.println("client");
			for (int i = TestStuff.msgs.length-1; i >= 0; i--)
			{
				System.out.println("\n");
				TestStuff.testBandwidthWithoutACK(TestStuff.msgs[i], TestStuff.getServerAddress(), socket);
			}
		}
		else
		{
			System.out.println("server");
			DatagramSocket socket = new DatagramSocket(TestStuff.getServerAddress());
			while (true)
			{
				TestStuff.receiveMessage(socket);
			}
		}
	}
}

class UDPTestExchangeMsgs
{
	UDPTestExchangeMsgs() throws IOException 
	{
		System.out.println("Exchange messages of different size");
		String str = TestStuff.askClientOrServer();
		if (str.equals("C") || str.equals("c"))
		{
			DatagramSocket socket = new DatagramSocket();
			System.out.println("client");
			for (byte[] msg: TestStuff.msgs)
			{
				// send a message
				TestStuff.sendMessage(msg, TestStuff.getServerAddress(), socket);
				// receive the same message
				TestStuff.receiveMessage(socket);
			}
		}
		else
		{
			System.out.println("server");
			DatagramSocket socket = new DatagramSocket(TestStuff.getServerAddress());
			byte[] msg = null;
			while (true)
			{
				// receive a message
				msg = TestStuff.receiveMessage(socket);
				// send the message back
				TestStuff.sendMessage(msg, TestStuff.inAddr, socket);
			}
		}
	}
}

class UDPTestMsg
{
	UDPTestMsg() throws IOException 
	{
		System.out.println("Send a custom message to server");
		String str = TestStuff.askClientOrServer();
		if (str.equals("C") || str.equals("c"))
		{
			System.out.println("client");
			System.out.print("Enter message to server: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			str = br.readLine();
			DatagramSocket socket = new DatagramSocket();
			TestStuff.sendMessage(str.getBytes(), TestStuff.getServerAddress(), socket);
			System.out.print("Server received the message!");
		}
		else
		{
			System.out.println("server");
			System.out.println("Waiting for a message from client...");
			DatagramSocket socket = new DatagramSocket(TestStuff.getServerAddress());
			str = new String(TestStuff.receiveMessage(socket));
			System.out.println("received: \""+str+"\"");
		}
	}
}
