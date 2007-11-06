package ee.ut.f2f.util.nat.traversal;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class StunInfo{
	private InetSocketAddress localAddress;
	private InetSocketAddress publicAddress;
	private String firewallType;
	
	public StunInfo(InetSocketAddress localAddress, InetSocketAddress publicAddress, String firewallType) {
		this.localAddress = localAddress;
		this.publicAddress = publicAddress;
		this.firewallType = firewallType;
	}
	
	public StunInfo(String localIpAddress,
					int localPort,
					String publicIpAddress, 
					int publicPort, 
					String firewallType){
		this(new InetSocketAddress(localIpAddress, localPort),
			 new InetSocketAddress(publicIpAddress, publicPort),
			 firewallType);
	}

	public void setLocalInetSocketAddress(InetSocketAddress localAddress){
		this.localAddress = localAddress;
	}
	
	public void setPublicInetSocketAddress(InetSocketAddress publicAddress){
		this.publicAddress = publicAddress;
	}
	
	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	public InetSocketAddress getPublicAddress() {
		return publicAddress;
	}

	public void setFirewallType(String firewallType) {
		this.firewallType = firewallType;
	}
	
	public String getFirewallType() {
		return firewallType;
	}
	
	public InetAddress getLocalInetAddress(){
		return localAddress.getAddress();
	}
	
	public InetAddress getPublicInetAddress(){
		return publicAddress.getAddress();
	}
	
	public int getLocalPort(){
		return localAddress.getPort();
	}
	
	public int getPublicPort(){
		return publicAddress.getPort();
	}
	
	public boolean isOpen(){
		if (getLocalInetAddress().equals(getPublicInetAddress())) return true;
		return false;
	}
}
