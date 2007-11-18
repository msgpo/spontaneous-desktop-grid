package ee.ut.f2f.util.nat.traversal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;

public class Server extends Thread {

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
		while( true ){
			try {
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Send data to client
	 * @param id Client's Id (example temp_7777@msn.com)
	 * @param data
	 */
	public void send(String id, byte[] data){
		//TODO
	}
}
