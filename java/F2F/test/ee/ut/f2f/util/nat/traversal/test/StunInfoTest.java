package ee.ut.f2f.util.nat.traversal.test;

import junit.framework.TestCase;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;
import ee.ut.f2f.util.nat.traversal.StunInfo;

public class StunInfoTest extends TestCase{

	
	public void testStunInfo(){
		StunInfo sinf = null;
		try {
			sinf = ConnectionManager.startNetworkDiscovery("stun.xten.net", 3478);
		} catch (Exception e1) {
			fail();
			e1.printStackTrace();
		}
		
		assertNotNull(sinf);
		sinf.setId("From");
		System.out.println("Stun info : " + sinf.toString());
		
	}
	public static void main(String[] args){
		junit.textui.TestRunner.run(StunInfoTest.class);
	}
}
