package ee.ut.f2f.util.nat.traversal.test;

import de.javawi.jstun.test.DiscoveryInfo;
import junit.framework.TestCase;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;

public class ConnectionManagerTest extends TestCase{
	
	public void testgetStunInfoData(){
		ConnectionManager cmt = new ConnectionManager();
		try {
			DiscoveryInfo di = cmt.startNetworkDiscovery("stun.xten.net", 3478);
			System.out.println(di);
		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
}
