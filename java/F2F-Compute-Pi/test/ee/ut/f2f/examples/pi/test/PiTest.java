package ee.ut.f2f.examples.pi.test;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.F2FDebug;

public class PiTest
{
	public static void main(final String[] args)
	{
		try {
			F2FComputing.initiateF2FComputing();
			F2FComputing.createJob(null, "ee.ut.f2f.examples.pi.PiMasterTask", null);
			F2FDebug.show(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
}
