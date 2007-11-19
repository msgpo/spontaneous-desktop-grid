package ee.ut.f2f.util.nat.traversal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;

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
		try {			
			Socket s = getSocketByID(id);
			log.debug("Sending data to client " + id);
			s.getOutputStream().write(data);
		}
		catch (NoSuchElementException e) {
			log.debug("Client " + id + " not found!");
		}
		catch (IOException e) {
			log.debug("Unable to send data to " + id);			
			e.printStackTrace();
		}
	}
	
	/**
	 * Send data to client
	 * @param id Client's Id
	 */	
	public Socket getSocketByID(String id) throws NoSuchElementException {
		Socket s = clients.get(id);
		if (s == null) {
			log.debug("Client " + id + " not found!");
			throw new NoSuchElementException("Client with ID " + id + " not found");
		}
		log.debug("Returning client " + id);
		return s;
	}
}
