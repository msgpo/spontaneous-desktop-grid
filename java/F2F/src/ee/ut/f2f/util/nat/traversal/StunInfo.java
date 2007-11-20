package ee.ut.f2f.util.nat.traversal;

import java.io.Serializable;

import de.javawi.jstun.test.DiscoveryInfo;

public class StunInfo implements Serializable{

	private String id;
	
	private boolean openAccess = false;
	private boolean blockedUDP = false;
	private boolean fullCone = false;
	private boolean restrictedCone = false;
	private boolean portRestrictedCone = false;
	private boolean symmetricCone = false;
	private boolean symmetricUDPFirewall = false;
	private String publicIP = null;
	private String localIp = null;
	
	private static final long serialVersionUID = 4721470383254301632L;

	public StunInfo(){
		
	}
	
	public StunInfo(DiscoveryInfo discoveryInfo) {
		if(discoveryInfo == null) throw new NullPointerException("Null argument discoveryInfo");
		this.openAccess = discoveryInfo.isOpenAccess();
		this.blockedUDP = discoveryInfo.isBlockedUDP();
		this.fullCone = discoveryInfo.isFullCone();
		this.restrictedCone = discoveryInfo.isRestrictedCone();
		this.portRestrictedCone = discoveryInfo.isPortRestrictedCone();
		this.symmetricCone = discoveryInfo.isSymmetricCone();
		this.symmetricUDPFirewall = discoveryInfo.isSymmetricUDPFirewall();
		if (discoveryInfo.getPublicIP() != null){
			this.publicIP = discoveryInfo.getPublicIP().getHostAddress();
		}
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	
	public boolean isOpenAccess() {
		return openAccess;
	}

	public void setOpenAccess(boolean openAccess) {
		this.openAccess = openAccess;
	}

	public boolean isBlockedUDP() {
		return blockedUDP;
	}

	public void setBlockedUDP(boolean blockedUDP) {
		this.blockedUDP = blockedUDP;
	}

	public boolean isFullCone() {
		return fullCone;
	}

	public void setFullCone(boolean fullCone) {
		this.fullCone = fullCone;
	}

	public boolean isRestrictedCone() {
		return restrictedCone;
	}

	public void setRestrictedCone(boolean restrictedCone) {
		this.restrictedCone = restrictedCone;
	}

	public boolean isPortRestrictedCone() {
		return portRestrictedCone;
	}

	public void setPortRestrictedCone(boolean portRestrictedCone) {
		this.portRestrictedCone = portRestrictedCone;
	}

	public boolean isSymmetricCone() {
		return symmetricCone;
	}

	public void setSymmetricCone(boolean symmetricCone) {
		this.symmetricCone = symmetricCone;
	}

	public boolean isSymmetricUDPFirewall() {
		return symmetricUDPFirewall;
	}

	public void setSymmetricUDPFirewall(boolean symmetricUDPFirewall) {
		this.symmetricUDPFirewall = symmetricUDPFirewall;
	}

	public String getPublicIP() {
		return publicIP;
	}

	public void setPublicIP(String publicIP) {
		this.publicIP = publicIP;
	}

	public String getLocalIp() {
		return localIp;
	}

	public void setLocalIp(String localIp) {
		this.localIp = localIp;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("[\nid: " + getId() + '\n');
		sb.append("Local IP address: ");
		sb.append(localIp);
		sb.append("\n");
		
		sb.append("Result: ");
		if (openAccess) sb.append("Open access to the Internet.\n");
		if (blockedUDP) sb.append("Firewall blocks UDP.\n");
		if (fullCone) sb.append("Full Cone NAT handles connections.\n");
		if (restrictedCone) sb.append("Restricted Cone NAT handles connections.\n");
		if (portRestrictedCone) sb.append("Port restricted Cone NAT handles connections.\n");
		if (symmetricCone) sb.append("Symmetric Cone NAT handles connections.\n");
		if (symmetricUDPFirewall) sb.append ("Symmetric UDP Firewall handles connections.\n");
		if (!openAccess && !blockedUDP && !fullCone && !restrictedCone && !portRestrictedCone && !symmetricCone && !symmetricUDPFirewall) sb.append("unkown\n");
		sb.append("Public IP address: ");
		if (publicIP != null) {
			sb.append(publicIP);
		} else {
			sb.append("unknown");
		}
		sb.append("\n]");
		
		return sb.toString();
	}
}
