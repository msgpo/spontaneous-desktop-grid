package ee.ut.f2f.util.nat.traversal.test;

import de.javawi.jstun.test.DiscoveryInfo;
import junit.framework.TestCase;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;
import ee.ut.f2f.util.nat.traversal.StunInfo;

public class StunInfoTest extends TestCase{

	
	public void testStunInfo(){
		DiscoveryInfo di = null;
		try {
			di = ConnectionManager.startNetworkDiscovery("stun.xten.net", 3478);
		} catch (Exception e1) {
			fail();
			e1.printStackTrace();
		}
		
		assertNotNull(di);
		System.out.println("Discovery info : " + di.toString());
		
		//StunInfo sinf = new StunInfo(di);
		
		System.out.println(di.getLocalIP().toString());
	}
	
}
