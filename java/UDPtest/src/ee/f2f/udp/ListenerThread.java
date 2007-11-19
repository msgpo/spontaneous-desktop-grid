package ee.f2f.udp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ListenerThread extends Thread {
	private DatagramSocket ds = null;

	public ListenerThread(DatagramSocket ds) {
		super();
		this.ds = ds;
	}

	@Override
	public void run() {
		int size = -1;
		try{
			size = "Ping".getBytes("utf-8").length;
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
			throw  new NullPointerException("Receiving buffer size not defined");
		}
		int n = 0;
		while(true){
			DatagramPacket dp = new DatagramPacket(new byte[size], size);
			try {
				ds.receive(dp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				System.out.println("Received [" + n + "] packet : [" + (new String(dp.getData(),"utf-8")) + "] from [" + dp.getAddress().getHostAddress() + ":" + dp.getPort() + "]");
			} catch (UnsupportedEncodingException e) {
				System.out.println("Error reciving [" + n + "] packet from [" + dp.getAddress().getHostAddress() + ":" + dp.getPort() + "]");
				e.printStackTrace();
			} finally {
				n++;
			}
		}
	}
}
