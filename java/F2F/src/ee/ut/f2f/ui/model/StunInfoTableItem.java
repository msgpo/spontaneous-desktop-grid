package ee.ut.f2f.ui.model;

import de.javawi.jstun.test.DiscoveryInfo;
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
	
	public void setTcpConnectivity(boolean canConnect) {
		tcpConnectivity = canConnect ? TCP_CAN_CONNECT : TCP_CANNOT_CONNECT;
	}
	
	public void resetTcpConnectivity() {
		tcpConnectivity = TCP_UNTESTED;
	}

	// required constructor overload as there is no super() without arguments
	public StunInfoTableItem(DiscoveryInfo discoveryInfo) {
		super(discoveryInfo);
	}
	
}
