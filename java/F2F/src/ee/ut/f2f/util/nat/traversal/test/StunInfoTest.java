package ee.ut.f2f.util.nat.traversal.test;

import junit.framework.TestCase;
import ee.ut.f2f.util.nat.traversal.StunInfo;

public class StunInfoTest extends TestCase{
	StunInfo info;
	String localIp = "192.168.66.66";
	int localPort = 6666;
	String publicIp = "193.55.66.77";
	int publicPort = 7777;
	public String firewallType = "Open";
	
	public void testStunInfo(){
		info = new StunInfo(localIp, localPort, publicIp, publicPort, firewallType);
		//System.out.println(info.getLocalInetAddress().toString().split("/")[1]);
		assertEquals(localIp, info.getLocalInetAddress().toString().split("/")[1]);
		assertEquals(localPort, info.getLocalPort());
		assertEquals(publicIp, info.getPublicInetAddress().toString().split("/")[1]);
		assertEquals(publicPort, info.getPublicPort());
		assertEquals(firewallType,info.getFirewallType());
	}
	
	public void testIsOpen(){
		publicIp = localIp;
		info = new StunInfo(localIp, localPort, publicIp, publicPort, firewallType);
		assertTrue(info.isOpen());
	}
}
