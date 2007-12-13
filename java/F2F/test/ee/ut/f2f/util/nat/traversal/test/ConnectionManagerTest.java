package ee.ut.f2f.util.nat.traversal.test;

import ee.ut.f2f.util.nat.traversal.ConnectionManager;
import ee.ut.f2f.util.nat.traversal.exceptions.ConnectionManagerException;
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
	
	
	public void testLoadProperties(){
		ConnectionManager cm = null;
		try{
			cm = new ConnectionManager("conf/nat-traversal.properties");
		} catch (ConnectionManagerException e){
			e.printStackTrace();
			fail();
		}
		assertNotNull(cm);
		assertNotNull(cm.getStunServers());
		assertTrue(cm.getStunServers().size() != 0);
		System.out.println ("Stun servers: [" + cm.getStunServers() + "]");
		assertTrue(cm.getScPort() != -1);
		System.out.println ("Socket communication port: [" + cm.getScPort() + "]");
	}
	
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
