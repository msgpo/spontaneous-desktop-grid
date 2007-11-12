package ee.ut.f2f.util.nat.traversal.test;

import de.javawi.jstun.test.DiscoveryInfo;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;
import ee.ut.f2f.util.nat.traversal.NatMessage;
import ee.ut.f2f.util.nat.traversal.StunInfo;
import ee.ut.f2f.util.nat.traversal.exceptions.NatMessageException;
import junit.framework.TestCase;

public class NatMessageTests extends TestCase {
	NatMessage message = new NatMessage("From", "To",NatMessage.REPORT_STUN_INFO,null);
	
	public void testEncodeDecode(){
		DiscoveryInfo di = null;
		try {
			di = ConnectionManager.startNetworkDiscovery("stun.xten.net", 3478);
		} catch (Exception e1) {
			fail();
			e1.printStackTrace();
		}
		
		assertNotNull(di);
		System.out.println("DiscoveryInfo : " + di.toString());
		
		StunInfo sinf = new StunInfo(di);
		sinf.setId("From");
		System.out.println("StunInfo :" + sinf.toString());
		
		message.setContent(sinf);
		System.out.println("NatMessage :" + message.toString());
		
		//encoding
		String encoded = null;
		try {
			encoded = message.encode();
		} catch (NatMessageException e) {
			fail(e.getMessage() + "\n" + e.getStackTrace());
		}
		assertNotNull(encoded);
		
		System.out.println("Encoded NatMessage :" + encoded);
		
		//decoding
		NatMessage nmsg = null;
		try{
			nmsg = new NatMessage(encoded);
		} catch (NatMessageException e){
			fail(e.getMessage() + "\n" + e.getStackTrace());
		}
		assertNotNull(nmsg);
		
		assertEquals(NatMessage.REPORT_STUN_INFO, nmsg.getType());
		assertEquals("From", nmsg.getFrom());
		assertEquals("To", nmsg.getTo());
		//assertEquals(sinf, nmsg.getContent());
		
		System.out.println("Decoded NatMessage :" + nmsg.toString());
	}
	
	/*
	public void testUUDecoder(){
		byte[] bytes = new byte[0];
		String encoded = null;
		UUEncoder uenc = new UUEncoder();
		UUDecoder udec = new UUDecoder();
		
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(message);
			bytes = baos.toByteArray();
			oos.close();
			baos.close();
			encoded = uenc.encodeBuffer(bytes);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		
		System.out.println(bytes);
		System.out.println();
		
		System.out.println(encoded);
		System.out.println();

		
		byte[] bytes2 = new byte[0];
		try {
			bytes2 = udec.decodeBufferToByteBuffer(encoded).array();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println(bytes2);
		System.out.println();
		
		NatMessage msg = null;
		try{
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes2));
			msg = (NatMessage) ois.readObject();
			ois.close();
		} catch (IOException e){
			e.printStackTrace();
			fail();
		} catch (ClassNotFoundException e){
			fail(e.getMessage());	
		}
		
		System.out.println(msg.getType() + ":" + msg.getContentType() + ":" + msg.getContent());
	}
	*/
}
