package ee.ut.f2f.util.nat.traversal.test;

import junit.framework.TestCase;

public class ConnectionManagerTest extends TestCase{
	
	public void testGetStunInfo(){

	}
	
	
	/*
	public void testGetLocalIps(){
		List<InetAddress> ips = null;
		try {
			ips = cm.getLocalIps();
		} catch (NetworkInterfaceNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		} catch (ConnectionManagerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		assertNotNull(ips);
		
		for(InetAddress ip : ips){
			System.out.println(ip.getHostAddress());
		}
	}
	*/
	
	/*
	public void testIsReachable(){
		InetAddress ip = null;
		try {
			ip = InetAddress.getByName("192.168.1.66");
		} catch (UnknownHostException e) {
			e.printStackTrace();
			fail();
		}
		assertNotNull(ip);
		
		assertTrue(cm.isReachable(ip, "stun.xten.net"));
	}
	*/
	
	/*
	public void testLoadProperties(){
		cm.run();
	}
	*/
	/*
	public void testgetStunInfoData(){
		try {
			StunInfo sinf = ConnectionManager.startNetworkDiscovery("stun.xten.net", 3478);
			assertNotNull(sinf);
			sinf.setId("From");
			System.out.println(sinf);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	*/
}
