package ee.ut.f2f.util.nat.traversal.test;

import ee.ut.f2f.util.nat.traversal.Server;
import junit.framework.TestCase;

public class ServerTests extends TestCase {
	
	public void testServer(){
		Server server = new Server(9000);
		server.run();
	}
}
