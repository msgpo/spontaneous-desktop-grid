package ee.ut.f2f.util.stun;

import java.net.InetAddress;

import de.javawi.jstun.test.DiscoveryTest;
import junit.framework.TestCase;
import ee.ut.f2f.util.stun.StunInfo;

public class StunInfoTest extends TestCase{

	
	public void testStunInfo(){
		StunInfo sinf = null;
		try {
			DiscoveryTest diTest = new DiscoveryTest(InetAddress.getByName("192.168.10.187"),"stun.xten.net",3478);
			sinf = new StunInfo(diTest.test());
			sinf.setLocalIP("192.168.10.187");
		} catch (Exception e1) {
			e1.printStackTrace();
			fail();
		}
		
		assertNotNull(sinf);
		sinf.setId("From");
		System.out.println("Stun info : " + sinf.toString());
		
	}
	public static void main(String[] args){
		junit.textui.TestRunner.run(StunInfoTest.class);
	}
}
