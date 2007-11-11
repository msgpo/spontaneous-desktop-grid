package ee.ut.f2f.util.nat.traversal;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkDiscoveryException;

public class ConnectionManager {
	
	final static NatLogger log = new NatLogger(ConnectionManager.class);
	
	/**
	 * 
	 * @param jStunServerName the name of JSTUN server (e.g. "stun.xten.net", "larry.gloo.net", "stun.xten.net")
	 * @param jStunServerPort the port of JSTUN server (e.g. 3478)
	 * @return DiscoveryInfo
	 * @throws Exception if something goes wrong
	 */
	public static DiscoveryInfo startNetworkDiscovery(String jStunServerName, int jStunServerPort ) throws NetworkDiscoveryException, Exception {
		Enumeration<NetworkInterface> ifaces = NetworkInterface
				.getNetworkInterfaces();
		while (ifaces.hasMoreElements()) {
			NetworkInterface iface = ifaces.nextElement();
			if (iface.isUp()) {
				Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
				while (iaddresses.hasMoreElements()) {
					InetAddress iaddress = iaddresses.nextElement();
					if (!iaddress.isLoopbackAddress()
							&& !iaddress.isLinkLocalAddress()) {
						DiscoveryTest test = new DiscoveryTest(iaddress,
								jStunServerName, jStunServerPort);
						DiscoveryInfo di =  test.test();
						return di;
					}
				}
			}
		}
		throw new NetworkDiscoveryException("Cannot get a response from the STUN server");
	}
}
