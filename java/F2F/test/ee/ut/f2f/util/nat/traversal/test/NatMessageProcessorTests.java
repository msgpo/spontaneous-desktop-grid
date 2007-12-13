package ee.ut.f2f.util.nat.traversal.test;

import ee.ut.f2f.util.nat.traversal.NatMessage;
import junit.framework.TestCase;

/**
 * 
 *
 */
public class NatMessageProcessorTests extends TestCase {
	NatMessage nmsg = new NatMessage("Me","Temp",601,null);
	
	/**
	 * 
	 */
	public NatMessageProcessorTests() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public NatMessageProcessorTests(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	

	public void testProcessMessage(){
		try {
			//NatMessageProcessor.processIncomingNatMessage(nmsg);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testSendMessage(){
		
	}
}
