/***************************************************************************
 *   Filename: LocalAddresses.java
 *   Author: artjom.lind@ut.ee
 ***************************************************************************
 *   Copyright (C) 2009 by Ulrich Norbisrath
 *   devel@mail.ulno.net
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as
 *   published by the Free Software Foundation; either version 2 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the
 *   Free Software Foundation, Inc.,
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ***************************************************************************
 *   Description:
 *   For collecting the information about local network interfaces
 ***************************************************************************/
package net.ulno.jpunch.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.apache.log4j.Logger;

/**
 * For collecting the information about local network interfaces
 * @author artjom.lind@ut.ee
 *
 */
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
