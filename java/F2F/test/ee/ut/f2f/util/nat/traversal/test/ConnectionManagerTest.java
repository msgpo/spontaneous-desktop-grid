package ee.ut.f2f.util.nat.traversal.test;

import junit.framework.TestCase;
import de.javawi.jstun.test.DiscoveryInfo;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;

public class ConnectionManagerTest extends TestCase{
	
	public void testgetStunInfoData(){
		ConnectionManager cmt = new ConnectionManager();
		try {
			DiscoveryInfo di = cmt.startNetworkDiscovery("stun.xten.net", 3478);
			//assertNotNull(si);
			System.out.println(di);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
