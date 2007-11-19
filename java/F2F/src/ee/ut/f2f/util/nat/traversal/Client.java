package ee.ut.f2f.util.nat.traversal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client extends Thread {
	
	private String ip = null;
	private int port = -1;
	
	private Socket soc = null;
    private BufferedOutputStream out = null;
    private BufferedInputStream in = null;
    
    private final static NatLogger log = new NatLogger(Client.class);
	
	/**
	 * Constructor
	 * @param ip
	 * @param port
	 */
	public Client(String ip, int port){
		super("F2F Client");
		if (ip == null || port <=0 ) throw new IllegalArgumentException();
		this.ip = ip;
		this.port = port;
	}
	
	/**
	 * Connect to ip and port specified in constructor
 	 */
	@Override
	public void run(){
		try {
			soc = new Socket(ip, port);
            log.debug("Connecting to server [" + ip + ":" + port + "]" );
            out = new BufferedOutputStream(soc.getOutputStream());
            in = new BufferedInputStream(soc.getInputStream());
            
            while(true){
            	byte[] bytes = new byte[in.available()];
            	in.read(bytes);
            	String received = new String(bytes,"utf-8");
            
            	if(received != null && !"".equals(received)) log.debug(">>> [" + received + "]");
            	if("HELO".equals(received)){
                    bytes = "BYE".getBytes("utf-8");
                    log.debug("<<< [BYE]");
                    out.write(bytes);
                    out.flush();
            	} else if ("BYE".equals(received)){
            		break;
            	}
            }
            log.debug("Closing client");
            in.close();
            out.close();
            soc.close();
            
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
		try {
			out.write(data);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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
	
	public BufferedOutputStream getOutputStream() {
		return out;
	}

	public BufferedInputStream getInputStream() {
		return in;
	}

}
