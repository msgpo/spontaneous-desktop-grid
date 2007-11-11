package ee.ut.f2f.util.nat.traversal;

import java.io.Serializable;
import java.net.InetAddress;

import de.javawi.jstun.test.DiscoveryInfo;

public class StunInfo implements Serializable{

	private String id;
	
	private DiscoveryInfo discoveryInfo;
	
	private static final long serialVersionUID = 4721470383254301632L;

	public StunInfo(DiscoveryInfo discoveryInfo) {
		this.discoveryInfo = discoveryInfo;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DiscoveryInfo getDiscoveryInfo() {
		return discoveryInfo;
	}

	public void setDiscoveryInfo(DiscoveryInfo discoveryInfo) {
		this.discoveryInfo = discoveryInfo;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("id: " + getId() + '\n');
		sb.append(discoveryInfo.toString());
		return sb.toString();
	}
}
