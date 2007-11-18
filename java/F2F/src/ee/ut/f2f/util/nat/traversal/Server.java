package ee.ut.f2f.util.nat.traversal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;

public class Server extends Thread {

	private final static NatLogger log = new NatLogger(Client.class);
	
	ServerSocket serverSocket = null;
	Map<String, Socket> clients = new Hashtable<String, Socket>();
	
	/**
	 * Constructor, setup server to listen on specific port
	 * @param port
	 */
	public Server(int port) {
		super("F2F Server");
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		Socket clientSocket = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		while( true ){
			try {
				clientSocket = serverSocket.accept();
				log.debug("New client connected from [" + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + "]");
				bis = new BufferedInputStream( clientSocket.getInputStream() );
				bos = new BufferedOutputStream( clientSocket.getOutputStream() );
				
				log.debug("<<< [HELO]");
				bos.write("HELO".getBytes("utf-8"));
				bos.flush();
				while(true){
					byte[] bytes = new byte[bis.available()];
					bis.read(bytes);
					String received = new String(bytes,"utf-8");
					if(received != null && !"".equals(received)) log.debug(">>> [" + received + "]");
					if("BYE".equals(received)){
						log.debug("<<< [BYE]");
						bos.write(bytes);
						bos.flush();
						break;
					}
				}
				log.debug("Closing client socket");
				bis.close();
				bos.close();
				clientSocket.close();
				log.debug("Server continues listening ...");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Send data to client
	 * @param id Client's Id
	 * @param data
	 */
	public void send(String id, byte[] data){
		//TODO
	}
}
