package net.ulno.jpunch.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.apache.log4j.Logger;

public class LocalAddresses
{
	private final static Logger log = Logger.getLogger(LocalAddresses.class);
	
	private static LocalAddresses instance = null;
	public static LocalAddresses getInstance()
	{
		if (instance != null) return instance;
		synchronized (LocalAddresses.class)
		{
			if (instance != null) return instance;
			return (instance = new LocalAddresses());
		}
	}
	
	private LocalAddresses()
	{
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
			return;
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
	}
	
	private Collection<InetAddress> localAddresses = null;
	public Collection<InetAddress> getLocalIPv4Addresses()
	{
		return localAddresses;
	}

}
