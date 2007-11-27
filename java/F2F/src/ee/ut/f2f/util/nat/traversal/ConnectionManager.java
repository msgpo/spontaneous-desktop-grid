package ee.ut.f2f.util.nat.traversal;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import de.javawi.jstun.test.DiscoveryTest;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.exceptions.ConnectionManagerException;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkDiscoveryException;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkInterfaceNotFoundException;

public class ConnectionManager {
	
	final static Logger log = Logger.getLogger(ConnectionManager.class);
	
	final private int STUN_SERVER_DEFAULT_PORT = 3478;
	
	private boolean isThreadRunning = false;
	
	//stunServers
	private List<String> stunServers = null;
	
	//stun info client
	private StunInfoClient stClient = null;
	
	//Table of TCP clients
	private Map<String, Socket> tcpClients = new Hashtable<String, Socket>();
	
	public ConnectionManager(String propertiesFilePath) throws ConnectionManagerException{
		loadProperties(propertiesFilePath);
	}
	
	public List<String> getStunServers(){
		return this.stunServers;
	}
	
	/**
	 * Controls if host can be reached from specific local ip
	 * @param yourIp
	 * @param server host
	 * @return true if is reachable, false if not
	 */
	public boolean isReachable(InetAddress yourIp, String server) throws NetworkInterfaceNotFoundException{
		try {
			InetAddress serverIp = InetAddress.getByName(server);
			NetworkInterface eth = NetworkInterface.getByInetAddress(yourIp);
			if ( eth == null ) {
				log.error("No interface found by local ip [" + yourIp.getHostAddress() + "]",null);
				throw new NetworkInterfaceNotFoundException();
			}
			return serverIp.isReachable(eth, 0, 5000);
		} catch (UnknownHostException e) {
			log.error("Unable to find host by name [" + server + "]",e);
			return false;
		} catch (IOException e) {
			log.error("Error pinging host [" + server + "] from local ip [" + yourIp.getHostAddress() + "]", e);
			return false;
		}
	}
	
	private void loadProperties(String propertiesFilePath) throws ConnectionManagerException{
		Resource fs = new FileSystemResource(propertiesFilePath);
		Properties props = null;
		try {
			props = PropertiesLoaderUtils.loadProperties(fs);
		} catch (IOException e) {
			throw new ConnectionManagerException("Unable to read properties form the file [" + propertiesFilePath + "]");
		}
		//load stun servers from properties
		String[] stunServers = props.getProperty("stunServers").split(",");
		if (stunServers == null || stunServers.length == 0) throw new ConnectionManagerException("No Stun servers specified in properties file");
		this.stunServers = Arrays.asList(stunServers);
		
		//load another properties
	}
	
	private List<InetAddress> getLocalIps() throws NetworkInterfaceNotFoundException, ConnectionManagerException{
		//Get interfaces
		Enumeration<NetworkInterface> interfaces = null;
		try{
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e){
			ConnectionManagerException ex = new NetworkInterfaceNotFoundException(e);
			interfaces = null;
			log.error(ex.getLocalizedMessage(),ex);
			throw ex;
		} finally {
			if (interfaces == null || !interfaces.hasMoreElements()) throw new NetworkInterfaceNotFoundException();
		}
		
		List<InetAddress> localIps = new ArrayList<InetAddress>();
		//Get Local Ip's
		try {
			while (interfaces.hasMoreElements()) {
				NetworkInterface inet = interfaces.nextElement();
				if (inet.isUp() && !inet.isVirtual()) {
					Enumeration<InetAddress> ips = inet.getInetAddresses();
					while(ips.hasMoreElements()){
						InetAddress ip = ips.nextElement();
						if( !ip.isLinkLocalAddress() && !ip.isLoopbackAddress() ){
							if(ip instanceof Inet6Address)
								continue;
							localIps.add(ip);
						}
					}
				}
			}
		} catch (SocketException e) {
			ConnectionManagerException ex = new ConnectionManagerException("Could not access the Network Interface",e);
			log.error(ex.getLocalizedMessage(),e);
			throw ex;
		} finally {
			if(localIps == null || localIps.isEmpty()) throw new ConnectionManagerException("Could not get the local ip addresses");
		}
		return localIps;
	}
	
	public  StunInfo startNetworkDiscovery() throws ConnectionManagerException, NetworkDiscoveryException {
		StunInfo sinf = null;
		//get local ips
		List<InetAddress> localIps = getLocalIps();
		for(String stunServer : stunServers){
			String address = null;
			int port = -1;
			if(stunServer.split(":").length >=2){
				address = stunServer.split(":")[0];
				port = Integer.parseInt(stunServer.split(":")[1]);
			} else {
				address = stunServer;
				port = STUN_SERVER_DEFAULT_PORT;
			}
			for(InetAddress ip : localIps){
				if(localIps.size() == 1 || isReachable(ip, address)){
					DiscoveryTest diTest = new DiscoveryTest(ip, address, port);
						try{
							sinf = new StunInfo(diTest.test());
							sinf.setLocalIp(ip.getHostAddress());
							log.debug("Network discovered from local ip [" + ip.getHostAddress() + "] using stun server [" + address + ":" + port + "]");
							return sinf;
						} catch (Exception e){
							log.error("Error discovering network, using stun server [" + address + ":" + port + "] from localIp [" + ip.getHostAddress() + "]", e);
							break;
						}
				} else {
					log.debug("Stun server [" + address + ":" + port + "] is unreachable from localIp [" + ip.getHostAddress() + "], trying next ip");
				}
			}
		}
		throw new NetworkDiscoveryException("No reliable stun server in list, or limited network connectivity");
	}
	
	public void refreshStunInfo(){
		if (!isThreadRunning()){
			StunInfoClient stClient = new StunInfoClient();
			stClient.start();
		} else {
			log.warn("StunInfoClient Thread is allready running");
		}
	}
	
	
	public void getLocalStunInfo(boolean forceReload) {
		String id = F2FComputing.getLocalPeer().getID().toString();
		StunInfo sinf = F2FComputingGUI.controller.getStunInfoTableModel().get(id);
		if (sinf == null || forceReload) {
		  refreshStunInfo();
		}
	}
	
	public Map<String, Socket> getTcpClients(){
		return tcpClients;
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
		Socket s = tcpClients.get(id);
		if (s == null) {
			log.debug("Client " + id + " not found!");
			throw new NoSuchElementException("Client with ID " + id + " not found");
		}
		log.debug("Returning client " + id);
		return s;
	}

	public boolean isThreadRunning() {
		return isThreadRunning;
	}

	public void setThreadRunning(boolean isThreadRunning) {
		this.isThreadRunning = isThreadRunning;
	}
}
