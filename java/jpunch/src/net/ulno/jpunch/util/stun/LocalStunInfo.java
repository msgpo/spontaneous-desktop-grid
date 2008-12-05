package net.ulno.jpunch.util.stun;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import de.javawi.jstun.test.DiscoveryTest;
import net.ulno.jpunch.util.LocalAddresses;
import net.ulno.jpunch.util.logging.Logger;

public class LocalStunInfo {
	final private static Logger log = Logger.getLogger(LocalStunInfo.class);

	final private int STUN_SERVER_DEFAULT_PORT = 3478;
	final private static int PING_TIMEOUT = 5000;
	final private static int MAX_TTL = 0;
	final private int MAX_WAIT_FILTERED = 100;
	final private static String PING_HOST = "www.google.com";
	final private static int PING_PORT = 80;
	

	private LocalStunInfo() {

	}

	private static LocalStunInfo instance = null;

	public static LocalStunInfo getInstance() {
		if (instance != null)
			return instance;
		synchronized (LocalStunInfo.class) {
			if (instance != null)
				return instance;
			return (instance = new LocalStunInfo());
		}
	}

	private StunInfo stunInfo = null;
	private Hashtable<InetAddress, Collection<InetSocketAddress>> stunServers = new Hashtable<InetAddress, Collection<InetSocketAddress>>();;
	private Collection<String> rawStunServers = null;

	public void setRawStunServers(Collection<String> rawStunServers){
		this.rawStunServers = rawStunServers;
	}
	
	public synchronized StunInfo getStunInfo() {
		if(LocalStunInfo.this.stunInfo == null){
			updateSTUNInfo();
			try {
				wait();
			} catch(InterruptedException e){
				
			}
			return LocalStunInfo.this.stunInfo;
		}
		return LocalStunInfo.this.stunInfo;
	}
	
	public synchronized void setStunInfo(StunInfo stunInfo){
		if(LocalStunInfo.this.stunInfo == null){
			LocalStunInfo.this.stunInfo = stunInfo;
			notify();
		}
	}

	public Collection<InetSocketAddress> getStunServers(InetAddress localIp) {
		if (localIp == null)
			throw new NullPointerException("localIp == null");
		Collection<InetSocketAddress> isas = stunServers.get(localIp);
		if (isas == null)
			isas = new ArrayList<InetSocketAddress>();
		return isas;
	}

	private boolean updateInProgress = false;
	private boolean filteringInProcess = false;

	public boolean isUpdating() {
		return updateInProgress;
	}

	public boolean isFiltering() {
		return filteringInProcess;
	}

	public void updateSTUNInfo() {
		synchronized (LocalStunInfo.class) {
			if (updateInProgress == true)
				return;
			updateInProgress = true;
		}

		new StunInfoUpdateThread().start();
	}

	public void updateReachableServers() {
		synchronized (LocalStunInfo.this) {
			if (LocalStunInfo.this.filteringInProcess)
				return;
			filteringInProcess = true;
		}

		new StunServersFilteringThread().start();
	}

	private class StunServersFilteringThread extends Thread{

		public StunServersFilteringThread() {
			this.setName(this.getClass().getName());
		}

		public void run() {
			// redefine reachable servers
			LocalStunInfo.this.stunServers = 
				new Hashtable<InetAddress, Collection<InetSocketAddress>>();

			Collection<InetAddress> localIps = LocalAddresses.getInstance()
					.getLocalIPv4Addresses();
			if (localIps.isEmpty()) {
				log
						.warn("STUN test can not be run if local machine has no IPs");
				return;
			}

			if (rawStunServers == null || rawStunServers.size() == 0) {
				log.warn("STUN test can not be run if no STUN " 
						  + "server specified in properties file");
				return;
			}

			log.info("Found total [" + rawStunServers.size()
					+ "] STUN server addresses in the properties file");
			log.info("Found total [" + localIps.size() + "] active local IPs");

			Collection<InetSocketAddress> reachableStunServers = 
				new ArrayList<InetSocketAddress>();
			// try if stun server is reachable from specific local ip
			for (InetAddress localIas : localIps) {
				if (isInternetReacheableFrom(localIas.getHostAddress())) {
					for (String stunServer : rawStunServers) {
						String address = null;
						int port = -1;
						if (stunServer.split(":").length >= 2) {
							address = stunServer.split(":")[0];
							port = Integer.parseInt(stunServer.split(":")[1]);
						} else {
							address = stunServer;
							port = STUN_SERVER_DEFAULT_PORT;
						}
						InetAddress stunServerIas = null;
						try {
							stunServerIas = InetAddress.getByName(address);
						} catch (UnknownHostException e) {
							log.warn("Unknown StunServer [" + address + "]", e);
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
							LocalStunInfo.this.stunServers.put(localIas,
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
			for (InetAddress localIp : LocalStunInfo.this.stunServers.keySet()) {
				sb.append("\tTotal ["
						+ LocalStunInfo.this.stunServers.get(localIp).size()
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
			for (int n = 0; n < MAX_WAIT_FILTERED; n++) {
				try {
					if (!LocalStunInfo.this.stunServers.isEmpty())
						break;
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			if (LocalStunInfo.this.stunServers.values().isEmpty()) {
				log.warn("No reachable servers in properties file");
				return;
			}

			StunInfo stunInfo = null;
			for (InetAddress ip : LocalStunInfo.this.stunServers.keySet()) {
				for (InetSocketAddress stunServer : LocalStunInfo.this.stunServers.get(ip)) {
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
			b = serverIP.isReachable(eth, MAX_TTL, PING_TIMEOUT);
		} catch (IOException e){
			log.error("Unable to ping host [" + serverIP.getHostName() + "] from "
					+ " local IP [" + yourIP.getHostAddress() + "]", e);
		}
		return b;
	}
	
	public static boolean isInternetReacheableFrom(String localIp){
		
		InetSocketAddress remoteAddr = new InetSocketAddress(PING_HOST,PING_PORT);
		InetSocketAddress localAddr = new InetSocketAddress(localIp,0);
		
		Socket soc = new Socket();
		try {
			soc.bind(localAddr);
			soc.connect(remoteAddr, PING_TIMEOUT);
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
