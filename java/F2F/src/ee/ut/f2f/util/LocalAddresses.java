package ee.ut.f2f.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import ee.ut.f2f.util.logging.Logger;

public class LocalAddresses
{
	private final static Logger log = Logger.getLogger(LocalAddresses.class);
	private static Collection<InetAddress> localAddresses = null;
	
	public static Collection<InetAddress> getLocalIPv4Addresses()
	{
		if (localAddresses != null) return localAddresses;
		
		synchronized (LocalAddresses.class)
		{
			if (localAddresses != null) return localAddresses;

			localAddresses = new ArrayList<InetAddress>();
			// get the network interfaces
			Enumeration<NetworkInterface> interfaces;
			try
			{
				interfaces = NetworkInterface.getNetworkInterfaces();
			}
			catch (SocketException e)
			{
				e.printStackTrace();
				log.debug(e.getMessage());
				return localAddresses;
			}
			
			// get local IP's
			while (interfaces.hasMoreElements())
			{
				NetworkInterface inet = interfaces.nextElement();
				Enumeration<InetAddress> ips = inet.getInetAddresses();
				while (ips.hasMoreElements())
				{
					InetAddress ip = ips.nextElement();
					if ( !ip.isLinkLocalAddress() && !ip.isLoopbackAddress() )
					{
						if (ip instanceof Inet4Address) localAddresses.add(ip);
					}
				}
			}
			return localAddresses;
		}
	}

}
