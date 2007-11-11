package ee.ut.f2f.util.nat.traversal.test;

import ee.ut.f2f.util.nat.traversal.NatMessage;
import ee.ut.f2f.util.nat.traversal.NatMessageException;
import ee.ut.f2f.util.nat.traversal.NatMessageProcessor;
import junit.framework.TestCase;

/**
 * @author admin
 *
 */
public class NatMessageProcessorTests extends TestCase {
	NatMessage nmsg = new NatMessage("Me","Temp",601,"666666");
	
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
		String encoded = null;
		try {
			encoded = "/NAT>/" + nmsg.encode() + "/NAT>/temp_6666@msn.com";
		} catch (NatMessageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		NatMessageProcessor.processIncomingNatMessage(encoded);
	}
	
	public void testSendMessage(){
		
	}
	/*
	public void test1ParseParams(){
		try{
			assertEquals("6001",(String) (proc.parseParams("ip=192&port=777&com=6001")).get("com"));
		} catch (InfoMessageParseException e){
			
		}
	}
	
	public void test2ParseParams(){
		try{
			proc.parseParams("com=192&port=777&com=6001");
			fail();
		} catch (InfoMessageParseException e){
			assertTrue(e.getMessage(), true);
		}
	}
	
	public void test3ParseParams(){
		try{
			assertNull(proc.parseParams("sss=192&port=777&fff=6001").get("com"));
		} catch (InfoMessageParseException e){
			fail(e.getMessage());
		}
	}
	
	public void test4ParseParams(){
		try{
			assertNull(proc.parseParams("sss=192&port=777&fff=").get("com"));
		} catch (InfoMessageParseException e){
			
		}
		}
	*/
}
