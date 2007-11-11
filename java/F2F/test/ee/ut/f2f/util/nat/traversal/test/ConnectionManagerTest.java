package ee.ut.f2f.util.nat.traversal.test;

import junit.framework.TestCase;
import de.javawi.jstun.test.DiscoveryInfo;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;

public class ConnectionManagerTest extends TestCase{
	
	public void testgetStunInfoData(){
		try {
			DiscoveryInfo di = ConnectionManager.startNetworkDiscovery("stun.xten.net", 3478);
			assertNotNull(di);
			System.out.println(di);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
