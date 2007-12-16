package ee.ut.f2f.util.nat.traversal;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import de.javawi.jstun.test.DiscoveryTest;

import ee.ut.f2f.comm.socket.SocketCommunicationProvider;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.exceptions.ConnectionManagerException;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkDiscoveryException;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkInterfaceNotFoundException;
import ee.ut.f2f.util.nat.traversal.threads.SocketCommunicationInitiator;
import ee.ut.f2f.util.nat.traversal.threads.StunInfoClient;
import ee.ut.f2f.util.nat.traversal.threads.TCPTester;

public class ConnectionManager {
	
	final static Logger log = Logger.getLogger(ConnectionManager.class);
	
	final private int STUN_SERVER_DEFAULT_PORT = 3478;
	
	private SocketCommunicationProvider socketCommunicationProvider = null;
	private boolean isStunInfoClientRunning = false;
	
	//stunServers
	private List<String> stunServers = null;
	
	//SocketCommunication listener port
	private int scPort = -1;
	
	//TCP Tester Threads
	private List<TCPTester> tcpTesters = new ArrayList<TCPTester>();
	
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
		Map<String, String> props = new Hashtable<String, String>();
		BufferedReader buf = null;
		
		try{
			buf = new BufferedReader(new FileReader(propertiesFilePath));
		} catch (FileNotFoundException e){
			log.error("Properties file [" + propertiesFilePath + "] not found", e);
			throw new ConnectionManagerException("Properties file [" + propertiesFilePath + "] not found",e);
		}
		
		//read properties file
		try{
			while(buf.ready()){
				String temp = buf.readLine();
				if(temp.startsWith("#")) continue;
				String [] entry = temp.split("=");
				if(entry == null || entry.length == 0) continue;
				if(entry[0] == null || entry[0].equals("")) continue;
				if(entry[1] == null || entry[1].equals("")) continue;
				props.put(entry[0], entry[1]);
			}
		} catch (IOException e){
			log.error("Unable to load properties from file [" + propertiesFilePath + "]", e);
			throw new ConnectionManagerException("Unable to load properties from file [" + propertiesFilePath + "]",e);
		}
		
		log.debug("Properies list size [" + props.size() + "]");
		for(String key : props.keySet()){
			log.debug("Property [" + key + "=" + props.get(key) + "]");
		}
		//load stun servers from properties
		String[] stunServers = props.get("stunServers").split(",");
		if (stunServers == null || stunServers.length == 0) throw new ConnectionManagerException("No addresses specified in properties file");
		this.stunServers = Arrays.asList(stunServers);
		
		this.scPort = Integer.parseInt(props.get("socketCommunicationPort"));
		
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
		//try {
			while (interfaces.hasMoreElements()) {
				NetworkInterface inet = interfaces.nextElement();
				//if (inet.isUp() && !inet.isVirtual()) {
					Enumeration<InetAddress> ips = inet.getInetAddresses();
					while(ips.hasMoreElements()){
						InetAddress ip = ips.nextElement();
						if( !ip.isLinkLocalAddress() && !ip.isLoopbackAddress() ){
							if(ip instanceof Inet6Address)
								continue;
							localIps.add(ip);
						}
					}
				//}
			}
		/*
		} catch (SocketException e) {
			ConnectionManagerException ex = new ConnectionManagerException("Could not access the Network Interface",e);
			log.error(ex.getLocalizedMessage(),e);
			throw ex;
		} finally {
		*/
			if(localIps == null || localIps.isEmpty()) throw new ConnectionManagerException("Could not get the local ip addresses");
		//}
		return localIps;
	}
	
	public  StunInfo startNetworkDiscovery() throws ConnectionManagerException, NetworkDiscoveryException {
		StunInfo sinf = null;
		//get local ips
		List<InetAddress> localIps = getLocalIps();
		log.info("Found total [" + stunServers.size() + "] addresses in properties file");
		log.info("Found total [" + localIps.size() + "] active local IP's");
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
							log.info("Discovering network from local ip [" + ip.getHostAddress() + "]");
							log.info("Using StunServer [" + address + ":" + port + "]");
							sinf = new StunInfo(diTest.test());
							sinf.setLocalIp(ip.getHostAddress());
							if (sinf != null) log.info("Network discovered successfully");
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
	
	public void refreshLocalStunInfo(){
		if (!isStunInfoClientRunning()){
			StunInfoClient stClient = new StunInfoClient();
			stClient.start();
		} else {
			log.warn("StunInfoClient Thread is allready running");
		}
	}
	
	public StunInfo getLocalStunInfo(){
		String id = F2FComputing.getLocalPeer().getID().toString();
		return F2FComputingGUI.controller.getStunInfoTableModel().get(id);
	}
	
	public StunInfo getLocalStunInfo(boolean forceReload) {
		log.debug("Get local StunInfo from StunInfoTableModel");
		String id = F2FComputing.getLocalPeer().getID().toString();
		StunInfo sinf = F2FComputingGUI.controller.getStunInfoTableModel().get(id);
		if (sinf == null || forceReload) {
			log.debug("No local StunInfo found in table, refreshing local StunInfo");
			refreshLocalStunInfo();
		}
		while(sinf == null){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				log.error(e);
			}
			sinf = F2FComputingGUI.controller.getStunInfoTableModel().get(id);
		}
		return sinf;
	}

	public boolean isStunInfoClientRunning() {
		return isStunInfoClientRunning;
	}

	public void setStunInfoClientRunning(boolean isStunInfoClientRunning) {
		this.isStunInfoClientRunning = isStunInfoClientRunning;
	}

	public SocketCommunicationProvider getSocketCommunicationProvider() {
		return socketCommunicationProvider;
	}

	public void setSocketCommunicationProvider(
			SocketCommunicationProvider socketCommunicationProvider) {
		this.socketCommunicationProvider = socketCommunicationProvider;
	}
	
	public void initiateSocketCommunicationProvider(){
		if(socketCommunicationProvider != null){
			log.debug("SocketCommunicationProvider allready initiated");
			return;
		} else {
			SocketCommunicationInitiator scInit = null;
			if(this.scPort == -1){
				scInit = new SocketCommunicationInitiator();
			} else {
				scInit = new SocketCommunicationInitiator(scPort);
			}
			scInit.start();
		}
	}

	public int getScPort() {
		return scPort;
	}

	public void setScPort(int scPort) {
		this.scPort = scPort;
	}
	
	public void addToSocketCommunicationProvider(final StunInfo sinf){
		new Thread(new Runnable(){
			public void run() {
				while(socketCommunicationProvider == null){
					initiateSocketCommunicationProvider();
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			   	log.debug("Adding new SocketPeer to SocketCommunicationProvider [" + sinf.getLocalIp() + "] id [" + sinf.getId() + "]");
			   	getSocketCommunicationProvider().addFriend(sinf.getLocalIp(), getScPort());
			   	
			   	//Add socket communication layer to F2FPeer
			   	F2FPeer f2fpeer = F2FComputingGUI.controller.getFriendModel().getF2FPeerById(sinf.getId());
			   	if(f2fpeer != null){
			   		log.debug("Adding SocketCommunicationProvider to F2Fpeer by id [" + sinf.getId() + "]");
			   		f2fpeer.addCommProvider(getSocketCommunicationProvider());
			   	} else {
			   		log.error("No F2FPeers found by id [" + sinf.getId() + "]");
			   	}
			}
		}).start();
	}
	
	public void initiateTCPTester(String peerId){
		if(getTCPTester(peerId) != null){
			log.debug("TCPTester [" + peerId + "] allready initialized");
			log.debug(getTCPTester(peerId));
		} else {
			TCPTester tt = new TCPTester(peerId);
			tt.start();
			tcpTesters.add(tt);
		}
	}
	
	public TCPTester getTCPTester(String peerId){
		for(TCPTester tt : tcpTesters){
			if(tt.getPeerId().equals(peerId)){
				return tt;
			}
		}
		log.debug("No TCP testers found by id [" + peerId + "], return null");
		return null;
	}
}
