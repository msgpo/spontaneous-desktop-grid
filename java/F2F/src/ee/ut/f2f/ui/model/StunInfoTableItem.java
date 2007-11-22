package ee.ut.f2f.ui.model;

import ee.ut.f2f.util.nat.traversal.StunInfo;

public class StunInfoTableItem extends StunInfo {

	private static final long serialVersionUID = 1L;
	
	public final int
		TCP_UNTESTED = 0,
		TCP_CAN_CONNECT = 1,
		TCP_CANNOT_CONNECT = 2
	;
	
	private int tcpConnectivity = TCP_UNTESTED;

	public boolean canConnectViaTCP() {
		return tcpConnectivity == TCP_CAN_CONNECT;
	}
	
	public boolean isTcpConnectivityTested() {
		return tcpConnectivity != TCP_UNTESTED;
	}
	
	public String getTcpConnectivity() {
		switch (tcpConnectivity) {
			case TCP_CAN_CONNECT:
				return "Yes";
			case TCP_CANNOT_CONNECT:
				return "No";
			default:
				return "Untested";
		}
	}
	
	public void setTcpConnectivity(boolean canConnect) {
		tcpConnectivity = canConnect ? TCP_CAN_CONNECT : TCP_CANNOT_CONNECT;
	}
	
	public void resetTcpConnectivity() {
		tcpConnectivity = TCP_UNTESTED;
	}

	// required constructor overload as there is no super() without arguments
	public StunInfoTableItem(StunInfo sinf) {
		super();
		this.setBlockedUDP(sinf.isBlockedUDP());
		this.setFullCone(sinf.isFullCone());
		this.setId(sinf.getId());
		this.setLocalIp(sinf.getLocalIp());
		this.setOpenAccess(sinf.isOpenAccess());
		this.setPortRestrictedCone(sinf.isPortRestrictedCone());
		this.setPublicIP(sinf.getPublicIP());
		this.setRestrictedCone(sinf.isRestrictedCone());
		this.setSymmetricCone(sinf.isSymmetricCone());
		this.setSymmetricUDPFirewall(sinf.isSymmetricUDPFirewall());
		
	}
	
}
