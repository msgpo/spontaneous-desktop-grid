package ee.ut.f2f.util.nat.traversal;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import ee.ut.f2f.util.nat.traversal.exceptions.ConnectionManagerException;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkDiscoveryException;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkInterfaceNotFoundException;

public class ConnectionManager {
	
	final static NatLogger log = new NatLogger(ConnectionManager.class);
	
	/**
	 * 
	 * @param StunServerName the name of JSTUN server (e.g. "stun.xten.net", "larry.gloo.net", "stun.xten.net")
	 * @param StunServerPort the port of JSTUN server (e.g. 3478)
	 * @return StunInfo
	 * @throws ConnectionManagerException 
	 * @throws Exception if something goes wrong
	 */
	public static StunInfo startNetworkDiscovery(String stunServerName, int stunServerPort ) throws ConnectionManagerException, NetworkDiscoveryException {
		StunInfo sinf = null;
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
		try {
			while (interfaces.hasMoreElements()) {
				NetworkInterface inet = interfaces.nextElement();
				if (inet.isUp() && !inet.isVirtual()) {
					Enumeration<InetAddress> ips = inet.getInetAddresses();
					while(ips.hasMoreElements()){
						InetAddress ip = ips.nextElement();
						if( !ip.isLinkLocalAddress() && !ip.isLoopbackAddress() ){
							localIps.add(ip);
						}
					}
				}
			}
		} catch (SocketException e) {
			ConnectionManagerException ex = new ConnectionManagerException("Could not access Network Interface",e);
			log.error(ex.getLocalizedMessage(),e);
			throw ex;
		} finally {
			if(localIps == null || localIps.isEmpty()) throw new ConnectionManagerException("Could not get local ip addresses");
		}
		
		DiscoveryInfo forDi = null;
		//Get Discovery Info
		for(InetAddress ip : localIps){
			DiscoveryTest diTest = new DiscoveryTest(ip, stunServerName, stunServerPort);
			try {
				forDi = diTest.test();
			} catch (UnknownHostException e) {
				NetworkDiscoveryException ex = new NetworkDiscoveryException("Stun Server not reachable [" + stunServerName + "]", e);
				log.error(ex.getLocalizedMessage(), e);
				throw ex;
			} catch (Exception e) {
				NetworkDiscoveryException ex = new NetworkDiscoveryException("Error discovering network on local ip [" + ip.getHostAddress() + 
						"] using Stun server [" + stunServerName + "]", e);
				log.error(ex.getLocalizedMessage(), e);
				throw ex;
			}
			if(forDi != null){
				//Filter the best
				if(sinf == null){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else if (sinf.isOpenAccess()){
					continue;
				} else if (!sinf.isOpenAccess() && forDi.isOpenAccess()){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else if (sinf.isFullCone()){
					continue;
				} else if (!sinf.isFullCone() && forDi.isFullCone()){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else if (sinf.isRestrictedCone()){
					continue;
				} else if (!sinf.isRestrictedCone() && forDi.isRestrictedCone()){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else if (sinf.isPortRestrictedCone()){
					continue;
				} else if (!sinf.isPortRestrictedCone() && forDi.isPortRestrictedCone()){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else if (sinf.isSymmetricCone()){
					continue;
				} else if (!sinf.isSymmetricCone() && forDi.isPortRestrictedCone()){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else if (sinf.isBlockedUDP() && !sinf.isBlockedUDP()){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else if (sinf.isSymmetricUDPFirewall() && !sinf.isSymmetricUDPFirewall()){
					sinf = new StunInfo(forDi);
					sinf.setLocalIp(ip.getHostAddress());
				} else {
					continue;
				}
			}
		}
		
		return sinf;
	}
}
