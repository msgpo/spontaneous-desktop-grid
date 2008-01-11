package ee.ut.f2f.core;

import java.util.UUID;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.core.F2FPeer;
import junit.framework.TestCase;

public class F2FPeerTest extends TestCase {

	public void testAddCommProvider()
	{
		F2FPeer peer = new F2FPeer(UUID.randomUUID(), "");
		TestCommProvider prov1 = new TestCommProvider(1);
		TestCommProvider prov2 = new TestCommProvider(2);
		TestCommProvider prov3 = new TestCommProvider(3);
		TestCommProvider prov4 = new TestCommProvider(4);
		TestCommProvider prov5 = new TestCommProvider(5);
		TestCommProvider prov6 = new TestCommProvider(6);
		int res = peer.addCommProvider(prov2);
		assertEquals(res, 0);
		res = peer.addCommProvider(prov5);
		assertEquals(res, 0);
		res = peer.addCommProvider(prov4);
		assertEquals(res, 1);
		res = peer.addCommProvider(prov5);
		assertEquals(res, -1);
		res = peer.addCommProvider(prov3);
		assertEquals(res, 2);
		res = peer.addCommProvider(prov6);
		assertEquals(res, 0);
		res = peer.addCommProvider(prov1);
		assertEquals(res, 5);
		res = peer.addCommProvider(null);
		assertEquals(res, -2);
	}

	private class TestCommProvider implements CommunicationProvider
	{
		int weight;
		TestCommProvider(int w)
		{
			weight = w;
		}
		
		public int getWeight() {
			return weight;
		}

		public void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException {		}
	}
}
