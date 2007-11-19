package ee.f2f.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class SenderThread extends Thread {
	private DatagramSocket ds = null;
	private DatagramPacket dp = null;
	private long delay = -1;

	public SenderThread(DatagramSocket ds, DatagramPacket dp, long delay) {
		super();
		this.ds = ds;
		this.dp = dp;
		this.delay = delay;
	}

	@Override
	public void run() {
		long n = 0;
		while(true){
			try {
				ds.send(dp);
				System.out.println("Sent ping [" + n + "] packet to [" + dp.getAddress().getHostAddress() + ":" + dp.getPort() + "]" );
				Thread.sleep(delay);
			} catch (IOException e) {
				System.out.println("Error sending [" + n + "] packet to [" + dp.getAddress().getHostAddress() + ":" + dp.getPort() + "]" );
				e.printStackTrace();
			} catch (InterruptedException e){
				e.printStackTrace();
				throw new NullPointerException();
			} finally {
				n++;
			}
			
		}
	}
	
	
}
