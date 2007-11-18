package ee.ut.f2f.util.nat.traversal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	
	private String ip = null;
	private int port = -1;
	
	private Socket soc = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    
    private final static NatLogger log = new NatLogger(Client.class);
	
	/**
	 * Constructor
	 * @param ip
	 * @param port
	 */
	public Client(String ip, int port){
		if (ip == null || port <=0 ) throw new IllegalArgumentException();
		this.ip = ip;
		this.port = port;
	}
	
	/**
	 * Connect to ip and port specified in constructor
 	 */
	public void connect(){
		try {
			soc = new Socket(ip, port);
            log.debug("Connecting to server [" + ip + ":" + port + "]" );
            //TODO ByteArrayInputStream and ByteArrayOutputStream should be used
            out = new PrintWriter(soc.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Send data to server
	 * @param data
	 */
	public void send(byte[] data){
		//TODO
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
