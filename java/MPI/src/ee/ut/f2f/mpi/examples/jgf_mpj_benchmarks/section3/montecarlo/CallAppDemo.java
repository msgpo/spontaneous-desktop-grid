/**************************************************************************
 *                                                                         *
 *             Java Grande Forum Benchmark Suite - MPJ Version 1.0         *
 *                                                                         *
 *                            produced by                                  *
 *                                                                         *
 *                  Java Grande Benchmarking Project                       *
 *                                                                         *
 *                                at                                       *
 *                                                                         *
 *                Edinburgh Parallel Computing Centre                      *
 *                                                                         *
 *                email: epcc-javagrande@epcc.ed.ac.uk                     *
 *                                                                         *
 *      Original version of this code by Hon Yau (hwyau@epcc.ed.ac.uk)     *
 *                                                                         *
 *      This version copyright (c) The University of Edinburgh, 2001.      *
 *                         All rights reserved.                            *
 *                                                                         *
 **************************************************************************/
package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3.montecarlo;

import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.exceptions.MPIException;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil.JGFSection3;

/**
 * Wrapper code to invoke the Application demonstrator.
 * 
 * @author H W Yau
 */
public abstract class CallAppDemo extends JGFSection3 {

	public CallAppDemo() {
		super();
	}

	public CallAppDemo(MPITask task) {
		super(task);
	}

	public int size;
	int datasizes[] = { 10000, 60000 };
	int input[] = new int[2];
	AppDemo ap = null;

	public void initialise() {

		input[0] = 1000;
		input[1] = datasizes[size];
		/* Aamir -- Data/hitData can be changed to basically anything */
		String dirName = "ee/ut/f2f/mpi/examples/jgf_mpj_benchmarks/section3/montecarlo";
		String filename = "hitData.txt";
		ap = new AppDemo(this, dirName, filename, (input[0]), (input[1]));
		ap.initSerial();
	}

	public void runiters() throws MPIException {
		ap.runSerial();
	}

	public void presults() {
		ap.processSerial();
	}

}
