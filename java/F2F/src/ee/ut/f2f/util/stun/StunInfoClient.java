package ee.ut.f2f.util.stun;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import de.javawi.jstun.test.DiscoveryTest;
import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.logging.Logger;

public class StunInfoClient
{
	final private static Logger log = Logger.getLogger(StunInfoClient.class);
	final private int STUN_SERVER_DEFAULT_PORT = 3478;
	
	public StunInfoClient() throws Exception
	{
		loadProperties("../F2F/conf/nat-traversal.properties");
	}
	
	private List<String> stunServers = null;//SocketCommunication listener port
	private void loadProperties(String propertiesFilePath) throws Exception
	{
		// read in the properties file
		Map<String, String> props = new Hashtable<String, String>();
		BufferedReader buf = new BufferedReader(new FileReader(propertiesFilePath));
		while(buf.ready())
		{
			String temp = buf.readLine();
			if(temp.startsWith("#")) continue;
			String [] entry = temp.split("=");
			if(entry == null || entry.length == 0) continue;
			if(entry[0] == null || entry[0].equals("")) continue;
			if(entry[1] == null || entry[1].equals("")) continue;
			props.put(entry[0], entry[1]);
		}
		
		log.debug("Properies list size [" + props.size() + "]");
		for(String key : props.keySet())
		{
			log.debug("Property [" + key + "=" + props.get(key) + "]");
		}
		
		// load stun servers from the properties
		String[] stunServers = props.get("stunServers").split(",");
		if (stunServers == null || stunServers.length == 0)
			throw new Exception("No STUN servers specified in properties file!");
		
		this.stunServers = Arrays.asList(stunServers);
		
		//load another properties
	}
	
	private Boolean updateInProgress = false;
	public void updateSTUNInfo()
	{
		synchronized (updateInProgress)
		{
			if (updateInProgress == true) return;
			updateInProgress = true;
		}
		
		new StunInfoUpdateThread().start();
	}
	
	private List<InetAddress> getLocalIPs() throws SocketException
	{
		// get the network interfaces
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		
		// get local IP's
		List<InetAddress> localIPs = new ArrayList<InetAddress>();
		while (interfaces.hasMoreElements())
		{
			NetworkInterface inet = interfaces.nextElement();
			Enumeration<InetAddress> ips = inet.getInetAddresses();
			while(ips.hasMoreElements())
			{
				InetAddress ip = ips.nextElement();
				if( !ip.isLinkLocalAddress() && !ip.isLoopbackAddress() )
				{
					if(ip instanceof Inet6Address)
						continue;
					localIPs.add(ip);
				}
			}
		}
		return localIPs;
	}

	/**
	 * Controls if host can be reached from specific local ip
	 * @param yourIp
	 * @param server host
	 * @return true if is reachable, false if not
	 */
	public boolean isReachable(InetAddress yourIP, String server)
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
	
	private class StunInfoUpdateThread extends Thread implements Activity
	{
		private StunInfoUpdateThread()
		{
			super("StunInfoUpdateThread");
		}
		
		public void run()
		{
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));

			StunInfo stunInfo = null;
			try
			{
    			List<InetAddress> localIPs = getLocalIPs();
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
								stunInfo.setId(F2FComputing.getLocalPeer().getID().toString());
								log.info("Getting STUN info succeeded!");
								F2FComputing.getLocalPeer().setSTUNInfo(stunInfo);
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
			}
			catch (SocketException e)
			{
				log.debug(e.getMessage());
			}
			
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FINISHED));
			synchronized (StunInfoClient.this.updateInProgress)
			{
				StunInfoClient.this.updateInProgress = false;
			}
		}

		public String getActivityName()
		{
			return this.getName() + " id [" + this.getId() + "]";
		}

		public Activity getParentActivity() {
			return null;
		}
	}
}
