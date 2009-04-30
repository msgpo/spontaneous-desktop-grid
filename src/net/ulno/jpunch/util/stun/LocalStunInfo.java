package net.ulno.jpunch.util.stun;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import net.ulno.jpunch.util.JPunchProperties;
import net.ulno.jpunch.util.LocalAddresses;
import de.javawi.jstun.test.DiscoveryTest;

public class LocalStunInfo {
	final private static Logger log = Logger.getLogger(LocalStunInfo.class);

	//The running instance of LocalStunInfo
	private static LocalStunInfo instance = null;
	
	// Current StunInfo
	private StunInfo currentStunInfo = null;
	
	//Hash table for filtered Stun servers 
	private Hashtable<InetAddress, Collection<InetSocketAddress>> filteredStunServers = 
		new Hashtable<InetAddress, Collection<InetSocketAddress>>();

	//Not filtered Stun servers
	private Collection<String> notFilteredStunServers = new ArrayList<String>();

	// common variables for tracing the state of the threads 
	private boolean updateInProgress = false;
	private boolean filteringInProcess = false;
	
	/*
	 * Default constructor
	 * Loads properties
	 * throws exception if no files found with such
	 * throws IOException in case of I/O errors
	 */
	private LocalStunInfo() {
		//load not filtered STUN servers
		String stunNotFileteredServers =
			JPunchProperties.getStringProperty(JPunchProperties.STUN_NOT_FILTERED_SERVERS);
		//set the List of not filtered STUN servers
		this.notFilteredStunServers = 
			parseNotFilteredServers(stunNotFileteredServers, ",");
	}

	/**
	 * Returns the current instance of LocalStunInfo, creates it if not exist
	 * @return current instance of <code>LocalStunInfo</code>
	 * @throws FileNotFoundException if can not found the properties file
	 * @throws IOException	if I/O error ocures while reading properties file
	 */
	public static LocalStunInfo getInstance() {
		if (instance != null)
			return instance;
		synchronized (LocalStunInfo.class) {
			if (instance != null)
				return instance;
			return (instance = new LocalStunInfo());
		}
	}
	
	/**
	 * Blocks until the StunInfo is available (network discovery is running)
	 * @return StunInfo - current Stun info
	 */
	public synchronized StunInfo getStunInfo() {
		if(LocalStunInfo.this.currentStunInfo == null){
			updateSTUNInfo();
			try {
				wait();
			} catch(InterruptedException e){}
			return LocalStunInfo.this.currentStunInfo;
		}
		return LocalStunInfo.this.currentStunInfo;
	}
	
	/**
	 * Sets current Stun info
	 * Unblocks waiting threads
	 * @param currentStunInfo
	 */
	private synchronized void setStunInfo(StunInfo currentStunInfo){
		if(LocalStunInfo.this.currentStunInfo == null){
			LocalStunInfo.this.currentStunInfo = currentStunInfo;
			notify();
		}
	}

	/*
	 * Returns the Collection of not filtered STUN servers
	 * @param String notFilteredStunServers
	 * @param String separator
	 * @return Collection of not filtered STUN servers
	 */
	private Collection<String> parseNotFilteredServers(String notFilteredStunServers, 
										   			   String separator){		
		String[] rawStunServersArray = notFilteredStunServers.split(separator);		
		Collection<String> rawStunServersCollection = Arrays.asList(rawStunServersArray);
		return rawStunServersCollection;
	}
	

	
	/**
	 * Returns active Stun servers for specific network interface
	 * @param localIp - IP address of the local network interface for
	 * which the active Stun servers would be returned
	 * @return Collection of active Stun servers
	 */
	public Collection<InetSocketAddress> getActiveStunServers(InetAddress localIp) {
		Collection<InetSocketAddress> isas = filteredStunServers.get(localIp);
		if (isas == null)
			isas = new ArrayList<InetSocketAddress>();
		return isas;
	}

	/**
	 * Forces the STUN info update thread to run
	 */
	public void updateSTUNInfo() {
		synchronized (LocalStunInfo.class) {
			if (updateInProgress) return;
			updateInProgress = true;
		}
		new StunInfoUpdateThread().start();
	}

	/**
	 * Forces filtering thread to run
	 */
	public void updateReachableServers() {
		synchronized (LocalStunInfo.this) {
			if (filteringInProcess) return;
			filteringInProcess = true;
		}
		new StunServersFilteringThread().start();
	}

	/*
	 * STUN servers filtering thread (internal)
	 * Filters reachable servers using <code>isInternetReacheableFrom</code>
	 * 
	 */
	private class StunServersFilteringThread extends Thread{
		private final Logger logger = Logger.getLogger(StunServersFilteringThread.class);
		//traces the state of the thread
		private boolean filteringInProcess = false;
		
		/*
		 * Returns the state of the thread
		 */
		boolean isFiltering() {
			return filteringInProcess;
		}
		
		/*
		 * Default constructor
		 * Set up Thread name
		 */
		StunServersFilteringThread() {
			this.setName(this.getClass().getName());
		}

		public void run() {
			// reinitialize filtered STUN servers
			LocalStunInfo.this.filteredStunServers = 
				new Hashtable<InetAddress, Collection<InetSocketAddress>>();

			// get local IPv4 addresses
			Collection<InetAddress> localIps = LocalAddresses.getInstance()
					.getLocalIPv4Addresses();
			
			if (localIps.isEmpty()) {
				logger.fatal("No IPv4 addresses");
				return;
			}

			if (notFilteredStunServers.size() == 0) {
				log.warn("STUN test can not be run if no STUN " 
						  + "server specified in properties file");
				return;
			}

			log.info(String.format(
					"Found total [%d] STUN server addresses in the properties file",
					notFilteredStunServers.size()));
			log.info(String.format(
					"Found total [%d] active local IPs",localIps.size() ));

			// For reachable StunServers
			Collection<InetSocketAddress> reachableStunServers = 
				new ArrayList<InetSocketAddress>();
			
			// try if stun server is reachable from specific local ip
			for (InetAddress localIas : localIps) {
				if (isInternetReacheableFrom(localIas.getHostAddress())) {
					for (String stunServer : notFilteredStunServers) {
						String address = null;
						int port = -1;
						if (stunServer.split(":").length >= 2) {
							address = stunServer.split(":")[0];
							port = Integer.parseInt(stunServer.split(":")[1]);
						} else {
							address = stunServer;
							port = JPunchProperties.getIntegerProperty(
									JPunchProperties.STUN_SERVER_PORT);
						}
						InetAddress stunServerIas = null;
						try {
							stunServerIas = InetAddress.getByName(address);
						} catch (UnknownHostException e) {
							log.warn("Unknown StunServer [" + address + "]");
						}
						if (stunServerIas == null)
							continue;

						if (isReachable(localIas, stunServerIas)) {
							reachableStunServers.add(new InetSocketAddress(
									stunServerIas, port));
							log.info("Found reachable StunServer ["
									+ stunServerIas.getHostAddress()
									+ "] from localIp ["
									+ localIas.getHostAddress() + "]");
							// synchronized (LocalStunInfo.class){
							LocalStunInfo.this.filteredStunServers.put(localIas,
									reachableStunServers);
							// }
						} else {
							log.warn("StunServer unreachable ["
									+ stunServerIas.getHostAddress()
									+ "] from localIp ["
									+ localIas.getHostAddress() + "]");
						}
					}
				} else {
					log.debug("Ip [" + localIas.getHostAddress()
							+ "] can't reach internet");
					continue;
				}
			}
			StringBuffer sb = new StringBuffer();
			for (InetAddress localIp : LocalStunInfo.this.filteredStunServers.keySet()) {
				sb.append("\tTotal ["
						+ LocalStunInfo.this.filteredStunServers.get(localIp).size()
						+ "]" + " from localIp [" + localIp.getHostAddress()
						+ "]\n");
			}
			log.info("Finished filtering reschable StunServers\n"
					+ (sb.length() == 0 ? "\tNo reachable servers" : sb
							.toString()));
			synchronized (LocalStunInfo.class) {
				LocalStunInfo.this.filteringInProcess = false;
			}
		}
	}

	private class StunInfoUpdateThread extends Thread {
		public StunInfoUpdateThread() {
			this.setName(this.getClass().getName());
		}

		public void run() {

			// filter reachable servers
			LocalStunInfo.this.updateReachableServers();

			// wait for first filtered address
			int timeout = JPunchProperties.getIntegerProperty(
							JPunchProperties.STUN_FILTERING_TIMEOUT);
			for (int n = 0; n < timeout; n++) {
				try {
					if (!LocalStunInfo.this.filteredStunServers.isEmpty())
						break;
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			if (LocalStunInfo.this.filteredStunServers.values().isEmpty()) {
				log.warn("No reachable servers in properties file");
				return;
			}

			StunInfo stunInfo = null;
			for (InetAddress ip : LocalStunInfo.this.filteredStunServers.keySet()) {
				for (InetSocketAddress stunServer : LocalStunInfo.this.filteredStunServers.get(ip)) {
					String address = stunServer.getAddress().getHostAddress();
					int port = stunServer.getPort();
					if (isReachable(ip, address)) {
						DiscoveryTest diTest = new DiscoveryTest(ip, address,
								port);
						try {
							log.info("Discovering network from local ip ["
									+ ip.getHostAddress() + "]");
							log.info("Using StunServer [" + address + ":"
									+ port + "]");
							stunInfo = new StunInfo(diTest.test());
							stunInfo.setLocalIP(ip.getHostAddress());
							log.info("Getting STUN info succeeded!");
							LocalStunInfo.this.setStunInfo(stunInfo);
							break;
						} catch (Exception e) {
							log.error(
									"Error discovering network, using stun server ["
											+ address + ":" + port
											+ "] from localIp ["
											+ ip.getHostAddress() + "]", e);
							// TODO: if error - stunServers list is becoming out
							// of date
							// remove failed server from list
							// if there is only one server left in list -
							// updateFilteredList
						}
					} else {
						// log.debug("Stun server [" + address + ":" + port + "]
						// is unreachable from localIp [" + ip.getHostAddress()
						// + "], trying next ip");
					}
				}
				if (stunInfo != null)
					break;
			}
			synchronized (LocalStunInfo.class) {
				LocalStunInfo.this.updateInProgress = false;
			}
		}
	}

	/**
	 * Controls if host can be reached from specific local ip
	 * 
	 * @param yourIp
	 * @param server
	 *            host
	 * @return true if is reachable, false if not
	 */
	public static boolean isReachable(InetAddress yourIP, String server) {
		InetAddress serverIP = null;
		try{
			serverIP = InetAddress.getByName(server);
		} catch (UnknownHostException e) {
			log.warn("Unknown host [" + server + "], can't reach");
		}
		if (serverIP == null) return false;
		return isReachable(yourIP, serverIP);
	}

	/**
	 * Controls if host can be reached from specific local ip
	 * 
	 * @param yourIp
	 * @param serverIp
	 * @return true if is reachable, false if not
	 */
	public static boolean isReachable(InetAddress yourIP, InetAddress serverIP) {
		NetworkInterface eth = null;
		try{
			eth = NetworkInterface.getByInetAddress(yourIP);
		} catch (SocketException e) {
			log.error("Unable to get NetworkInterface by IP [" + yourIP.getHostAddress() + "]",e);
		}
		if (eth == null){
			log.warn("Unable to get NetworkInterface by IP [" + yourIP.getHostAddress() + "]");
			return false;
		}
		boolean b = false;
		try {
			int maxTtl = JPunchProperties.getIntegerProperty(
							JPunchProperties.ITEST_MAX_TTL);
			int pingTimeout = JPunchProperties.getIntegerProperty(
								JPunchProperties.ITEST_PING_TIMEOUT);
			b = serverIP.isReachable(eth, maxTtl, pingTimeout);
		} catch (IOException e){
			log.error("Unable to ping host [" + serverIP.getHostName() + "] from "
					+ " local IP [" + yourIP.getHostAddress() + "]", e);
		}
		return b;
	}
	
	public static boolean isInternetReacheableFrom(String localIp){
		
		int pingTimeout = JPunchProperties.getIntegerProperty(
				JPunchProperties.ITEST_PING_TIMEOUT);
		String pingHost = JPunchProperties.getStringProperty(
						JPunchProperties.ITEST_PING_HOST);
		int pingPort = JPunchProperties.getIntegerProperty(
				JPunchProperties.ITEST_PING_PORT);
		
		InetSocketAddress remoteAddr = new InetSocketAddress(pingHost,pingPort);
		InetSocketAddress localAddr = new InetSocketAddress(localIp,0);

		Socket soc = new Socket();
		try {
			soc.bind(localAddr);
			soc.connect(remoteAddr, pingTimeout);
			soc.close();
			return true;
		} catch (SocketTimeoutException e){
			log.warn("Internet test timeout on local IP [" 
					+ localAddr.getAddress().getHostAddress() + "]"
					+ " can not reach [" + remoteAddr.getHostName() 
					+ ":" + remoteAddr.getPort() + "] return false");
			return false;
		} catch (IOException e) {
			log.error("Internet test failure on local IP [" 
					+ localAddr.getAddress().getHostAddress() + "]"
					+ " port [" + localAddr.getPort() + "] return false", e);
			return false;
		}
	}
}
