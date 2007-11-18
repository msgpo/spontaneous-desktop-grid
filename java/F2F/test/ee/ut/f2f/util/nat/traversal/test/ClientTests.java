package ee.ut.f2f.util.nat.traversal.test;

import ee.ut.f2f.util.nat.traversal.Client;
import junit.framework.TestCase;


public class ClientTests extends TestCase {
	
	public void testClient(){
		Client client = new Client("127.0.0.1",9000);
		client.run();
	}
}
