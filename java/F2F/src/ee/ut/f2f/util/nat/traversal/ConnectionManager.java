package ee.ut.f2f.util.nat.traversal;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ConnectionManager {
	
	final static NatLogger log = new NatLogger(ConnectionManager.class);
	
	public static List<InetAddress> getSystemIpAddres(){
		ArrayList<InetAddress> systemIpList = null;
		try{
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();	
			while(interfaces.hasMoreElements()){
				NetworkInterface inet = interfaces.nextElement();
				if(inet.isUp()){
					Enumeration<InetAddress> ipAdresses = inet.getInetAddresses();
					while(ipAdresses.hasMoreElements()){
						InetAddress ip = ipAdresses.nextElement();
						systemIpList = new ArrayList<InetAddress>();
						if(!ip.isLoopbackAddress() && !ip.isLinkLocalAddress()){
							systemIpList.add(ip);
						}
					}
				}
			}
		} catch (SocketException ex){
			log.error("Unable to get the system IP addresses list : ", ex);
		}
		log.debug("Total [" + systemIpList.size() + "] addresses");
		return systemIpList;
	}
	
	public static StunInfo getStunInfo(){
		//for testing purposes
		StunInfo sinf = new StunInfo("192.168.6.166",6666,"192.168.6.166",6666,"Open");
			
		return sinf;
	}
}
