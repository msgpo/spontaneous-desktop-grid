package ee.ut.f2f.util.stun;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;

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
	
	private LocalStunInfo()
	{	
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
	public StunInfo getStunInfo() { return stunInfo; }

	private boolean updateInProgress = false;
	public boolean isUpdating() { return updateInProgress; }
	
	public void updateSTUNInfo()
	{
		synchronized (LocalStunInfo.class)
		{
			if (updateInProgress == true) return;
			updateInProgress = true;
		}
		
		new StunInfoUpdateThread().start();
	}
	
	private class StunInfoUpdateThread extends Thread implements Activity
	{		
		public void run()
		{
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
			
			Collection<InetAddress> localIPs = LocalAddresses.getInstance().getLocalIPv4Addresses();
			if (localIPs.isEmpty())
			{
				log.warn("STUN test can not be run if local machine has no IPs");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED, "no NICs"));
				return;
			}
			Collection<String> stunServers = F2FProperties.getF2FProperties().getSTUNProperties().stunServers;
			if (stunServers == null || stunServers.size() == 0)
			{
				log.warn("STUN test can not be run if no STUN server specified in properties file");
				ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED));
				return;
			}

			StunInfo stunInfo = null;
			log.info("Found total [" + stunServers.size() + "] STUN server addresses in the properties file");
			log.info("Found total [" + localIPs.size() + "] active local IPs");
			
			for(String stunServer : stunServers)
			{
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
				for(InetAddress ip : localIPs)
				{
					if(isReachable(ip, address))
					{
						DiscoveryTest diTest = new DiscoveryTest(ip, address, port);
						try
						{
							log.info("Discovering network from local ip [" + ip.getHostAddress() + "]");
							log.info("Using StunServer [" + address + ":" + port + "]");
							stunInfo = new StunInfo(diTest.test());
							stunInfo.setLocalIP(ip.getHostAddress());
							log.info("Getting STUN info succeeded!");
							LocalStunInfo.this.stunInfo = stunInfo;
							break;
						}
						catch (Exception e)
						{
							log.error("Error discovering network, using stun server [" + address + ":" + port + "] from localIp [" + ip.getHostAddress() + "]", e);
						}
					}
					else
					{
						//log.debug("Stun server [" + address + ":" + port + "] is unreachable from localIp [" + ip.getHostAddress() + "], trying next ip");
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
			return "StunInfoUpdateThread";
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
			return serverIP.isReachable(eth, 0, 5000);
		}
		catch (Exception e)
		{
			log.warn(e.getMessage());
			return false;
		}
	}
}
