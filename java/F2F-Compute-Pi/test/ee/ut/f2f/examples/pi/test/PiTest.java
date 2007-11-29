package ee.ut.f2f.examples.pi.test;

import java.util.ArrayList;
import java.util.Collection;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.f2f.util.F2FDebug;

public class PiTest
{
	public static void main(final String[] args)
	{
		try {
			F2FComputing.initiateF2FComputing();
			
			Collection<String> jarFilesNames = new ArrayList<String>();
			jarFilesNames.add("D:\\eclipse\\workspace\\F2F-Compute-Pi\\ComputePi.jar");
			F2FDebug.show(true);
			F2FComputing.createJob(jarFilesNames, "ee.ut.f2f.examples.pi.PiMasterTask", null);
		} catch (F2FComputingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
}
