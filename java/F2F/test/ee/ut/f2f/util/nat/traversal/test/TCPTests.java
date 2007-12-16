package ee.ut.f2f.util.nat.traversal.test;

import ee.ut.f2f.util.nat.traversal.threads.TCPTester;
import junit.framework.TestCase;

public class TCPTests extends TestCase {
	
	/*
	public void testTimeoutWaitingReport() throws InterruptedException{
		TCPTester test = new TCPTester("ssss");
		System.out.println(test);
		test.start();
		System.out.println(test);
		Thread.sleep(15000);
		System.out.println(test);
		Thread.sleep(20000);
		System.out.println(test);
	}
	*/
	public void testReceivedReport() throws InterruptedException{
		TCPTester test = new TCPTester("hhhh");
		System.out.println(test);
		test.start();
		System.out.println(test);
		Thread.sleep(15000);
		System.out.println(test);
		test.peerReported(new Integer(TCPTester.TCP_TESTER_ALIVE));
		System.out.println(test);
	}
}
