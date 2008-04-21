package ee.ut.f2f.examples.pi.test;

import java.util.ArrayList;
import java.util.Collection;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.F2FDebug;

public class PiTest
{
	public static void main(final String[] args)
	{
		try {
			F2FComputing.initiateF2FComputing();
			Collection<F2FPeer> peers = new ArrayList<F2FPeer>();
			peers.add(F2FComputing.getLocalPeer());
			F2FComputing.createJob(null, "ee.ut.f2f.examples.pi.PiMaster2", peers);
			F2FDebug.show(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
}
