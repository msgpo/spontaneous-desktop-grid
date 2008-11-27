package ee.ut.f2f.util.stun;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import de.javawi.jstun.test.DiscoveryTest;
import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.util.F2FProperties;
import ee.ut.f2f.util.LocalAddresses;
import ee.ut.f2f.util.logging.Logger;

public class LocalStunInfo
{
	final private static Logger log = Logger.getLogger(LocalStunInfo.class);
	
	final private int STUN_SERVER_DEFAULT_PORT = 3478;
	final private static int IS_REACHABLE_TIMEOUT = 5000;
	final private int MAX_WAIT_FILTERED = 100;
	final private String PING_HOST = "math.ut.ee";
	
	private InetAddress pingHost = null;
	
	private LocalStunInfo()
	{
		try{
			pingHost = InetAddress.getByName(PING_HOST);
		} catch (UnknownHostException e) {
			log.error("Unknown Ping Host [" + PING_HOST + "]");
		}
	}
	
	private static LocalStunInfo instance = null;
	public static LocalStunInfo getInstance()
	{
		if (instance != null) return instance;
		synchronized (LocalStunInfo.class)
		{
			if (instance != null) return instance;
			return (instance = new LocalStunInfo());
		}
	}

	private StunInfo stunInfo = null;
	private Hashtable<InetAddress,Collection<InetSocketAddress>> stunServers = new Hashtable<InetAddress, Collection<InetSocketAddress>>();;
	public StunInfo getStunInfo() { return stunInfo; }
	public Collection<InetSocketAddress> getStunServers(InetAddress localIp) {
		if( localIp == null ) throw new NullPointerException("localIp == null");
		Collection<InetSocketAddress> isas = stunServers.get(localIp);
		if (isas == null) isas = new ArrayList<InetSocketAddress>();
		return isas;
	}

	private boolean updateInProgress = false;
	private boolean filteringInProcess = false;
	public boolean isUpdating() { return updateInProgress; }
	public boolean isFiltering() { return filteringInProcess; }
	public InetAddress getPingHost(){
		return this.pingHost;
	}
	
	public void updateSTUNInfo()
	{
		synchronized (LocalStunInfo.class)
		{
			if (updateInProgress == true) return;
			updateInProgress = true;
		}
		
		new StunInfoUpdateThread().start();
	}
	
	public void updateReachableServers(){
		synchronized (LocalStunInfo.this) {
			if(LocalStunInfo.this.filteringInProcess) return;
			filteringInProcess = true;
		}
		
		
		new StunServersFilteringThread().start();
	}
		
	private class StunServersFilteringThread extends Thread implements Activity{
		
		public StunServersFilteringThread() {
			this.setName(this.getClass().getName());
		}
		
		public void run(){
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
			
			//redefine reachable servers
			LocalStunInfo.this.stunServers = new Hashtable<InetAddress, Collection<InetSocketAddress>>();
			
			Collection<InetAddress> localIps = LocalAddresses.getInstance().getLocalIPv4Addresses();
			if (localIps.isEmpty())
			{
				log.warn("STUN test can not be run if local machine has no IPs");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED, "no NICs"));
				return;
			}
			Collection<String> rawStunServers = F2FProperties.getF2FProperties().getSTUNProperties().stunServers;
			if (rawStunServers == null || rawStunServers.size() == 0)
			{
				log.warn("STUN test can not be run if no STUN server specified in properties file");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED));
				return;
			}

			log.info("Found total [" + rawStunServers.size() + "] STUN server addresses in the properties file");
			log.info("Found total [" + localIps.size() + "] active local IPs");
			
			Collection<InetSocketAddress> reachableStunServers = new ArrayList<InetSocketAddress>();
			// try if stun server is reachable from specific local ip
			for (InetAddress localIas : localIps) {
				if (isReachable(localIas, LocalStunInfo.this.getPingHost())){
					for(String stunServer : rawStunServers){
						String address = null;
						int port = -1;
						if(stunServer.split(":").length >= 2)
						{
							address = stunServer.split(":")[0];
							port = Integer.parseInt(stunServer.split(":")[1]);
						}
						else
						{
							address = stunServer;
							port = STUN_SERVER_DEFAULT_PORT;
						}
						InetAddress stunServerIas = null;
						try{
							stunServerIas = InetAddress.getByName(address);
						} catch (UnknownHostException e){
							log.warn("Unknown StunServer [" + address + "]",e);
						}
						if (stunServerIas == null)
							continue;
						
					
						if (isReachable(localIas, stunServerIas)) {
							reachableStunServers.add(new InetSocketAddress(stunServerIas, port));
							log.info("Found reachable StunServer ["
									+ stunServerIas.getHostAddress() 
									+ "] from localIp ["
									+ localIas.getHostAddress()
									+ "]");
							//synchronized (LocalStunInfo.class){
								LocalStunInfo.this.stunServers.put(localIas, reachableStunServers);
							//}
						} else {
							log.warn("StunServer unreachable ["
									+ stunServerIas.getHostAddress() 
									+ "] from localIp ["
									+ localIas.getHostAddress()
									+ "]");
						}
					}
				} else {
					log.debug("Ip [" + localIas.getHostAddress() + "] can't reach internet");
					continue;
				}
			}
			StringBuffer sb = new StringBuffer();
			for(InetAddress localIp : LocalStunInfo.this.stunServers.keySet()){
				sb.append("\tTotal [" + LocalStunInfo.this.stunServers.get(localIp).size() + "]"
							  + " from localIp [" + localIp.getHostAddress() + "]\n");
			}
			log.info("Finished filtering reschable StunServers\n"
						+ (sb.length() == 0 ? "\tNo reachable servers" :sb.toString()));
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FINISHED));
			synchronized (LocalStunInfo.class)
			{
				LocalStunInfo.this.filteringInProcess = false;
			}
		}
		
		public String getActivityName() {
			return this.getName();
		}

		public Activity getParentActivity() {
			return null;
		}
		
	}
	
	private class StunInfoUpdateThread extends Thread implements Activity
	{		
		public StunInfoUpdateThread() {
			this.setName(this.getClass().getName());
		}
		
		public void run()
		{
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
						
			//filter reachable servers
			LocalStunInfo.this.updateReachableServers();
			
			//wait for first filtered address
			for(int n = 0; n < MAX_WAIT_FILTERED; n++){
				try{
					if(!LocalStunInfo.this.stunServers.isEmpty()) break;
					Thread.sleep(500);
				} catch (InterruptedException e){}
			}
			if(LocalStunInfo.this.stunServers.values().isEmpty()){
				log.warn("No reachable servers in properties file");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this, 
						ActivityEvent.Type.FAILED, 
						"No reachable servers in property file"));
				return;
			}
			
			StunInfo stunInfo = null;
			for (InetAddress ip : LocalStunInfo.this.stunServers.keySet()) {
				for (InetSocketAddress stunServer : LocalStunInfo.this.stunServers
						.get(ip)) {
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
							LocalStunInfo.this.stunInfo = stunInfo;
							break;
						} catch (Exception e) {
							log.error(
									"Error discovering network, using stun server ["
											+ address + ":" + port
											+ "] from localIp ["
											+ ip.getHostAddress() + "]", e);
							//TODO: if error - stunServers list is becoming out of date
							//remove failed server from list
							//if there is only one server left in list - updateFilteredList
						}
					} else {
						// log.debug("Stun server [" + address + ":" + port + "]
						// is unreachable from localIp [" + ip.getHostAddress()
						// + "], trying next ip");
					}
				}
				if (stunInfo != null) break;
			}
			
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FINISHED));
			synchronized (LocalStunInfo.class)
			{
				LocalStunInfo.this.updateInProgress = false;
			}
		}

		public String getActivityName()
		{
			return this.getName();
		}

		public Activity getParentActivity() {
			return null;
		}
	}

	/**
	 * Controls if host can be reached from specific local ip
	 * @param yourIp
	 * @param server host
	 * @return true if is reachable, false if not
	 */
	public static boolean isReachable(InetAddress yourIP, String server)
	{
		try
		{
			InetAddress serverIP = InetAddress.getByName(server);
			NetworkInterface eth = NetworkInterface.getByInetAddress(yourIP);
			if (eth == null) return false;
			return serverIP.isReachable(eth, 0, LocalStunInfo.IS_REACHABLE_TIMEOUT);
		}
		catch (Exception e)
		{
			log.warn("Network error",e);
			return false;
		}
	}
	
	/**
	 * Controls if host can be reached from specific local ip
	 * @param yourIp
	 * @param serverIp
	 * @return true if is reachable, false if not
	 */
	public static boolean isReachable(InetAddress yourIP, InetAddress serverIP)
	{
		try
		{
			NetworkInterface eth = NetworkInterface.getByInetAddress(yourIP);
			if (eth == null) return false;
			return serverIP.isReachable(eth, 0, LocalStunInfo.IS_REACHABLE_TIMEOUT);
		}
		catch (Exception e)
		{
			log.warn("Network error",e);
			return false;
		}
	}
}
