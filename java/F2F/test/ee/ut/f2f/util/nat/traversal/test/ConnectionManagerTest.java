package ee.ut.f2f.util.nat.traversal.test;

import junit.framework.TestCase;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;
import ee.ut.f2f.util.nat.traversal.StunInfo;

public class ConnectionManagerTest extends TestCase{
	
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
}
