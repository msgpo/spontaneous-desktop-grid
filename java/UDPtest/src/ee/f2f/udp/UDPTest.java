package ee.f2f.udp;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UDPTest {
	public static void main(String[] argv) throws SocketException{
		if(argv.length >= 5){
					long delay = Long.parseLong(argv[0]);
			
					int local = Integer.parseInt(argv[2]);
					InetSocketAddress localIp = new InetSocketAddress(argv[1],local);
					
					DatagramSocket soc = new DatagramSocket(localIp);
					soc.setReuseAddress(true);
					Thread listener = new ListenerThread(soc);
					listener.start();
					
					int remote = Integer.parseInt(argv[4]);
					InetSocketAddress remoteIp = new InetSocketAddress(argv[3],remote);
					
					byte[] data = new byte[0];
					try{
						data = "Ping".getBytes("utf-8");
					} catch (UnsupportedEncodingException e){
						e.printStackTrace();
						throw new NullPointerException("Sending buffer not defined");
					}
					
					DatagramPacket dp = new DatagramPacket(new byte[data.length], data.length);
					dp.setData(data);
					dp.setAddress(remoteIp.getAddress());
					dp.setPort(remote);
					
					Thread sender = new SenderThread(soc,dp,delay);
					sender.start();

		} else {
			throw new IllegalArgumentException("Wrong arguments, should be [delay ms][LocalIp][LocalIp][DestIp][DestPort]");
		}
	}
}
