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
 *                                                                         *
 *      This version copyright (c) The University of Edinburgh, 2001.      *
 *                         All rights reserved.                            *
 *                                                                         *
 **************************************************************************/
package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section2;

import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section2.sor.JGFSORBench;

public class JGFSORBenchSizeA extends JGFSORBench {

	public void runTask() {
		getMPIDebug().setDebugLevel(0);
		/* Initialise MPI */
		MPI().Init(this);
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();
		if (rank == 0) {
			JGFInstrumentor.printHeader(2, 0, nprocess);
		}
		JGFrun(0);

		/* Finalise MPI */
		MPI().Finalize();

	}
}
