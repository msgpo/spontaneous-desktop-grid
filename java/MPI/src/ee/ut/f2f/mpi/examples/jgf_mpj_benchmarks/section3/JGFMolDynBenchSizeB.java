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
package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3;

import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3.moldyn.JGFMolDynBench;

public class JGFMolDynBenchSizeB extends JGFMolDynBench {

	public void runTask() {
		getMPIDebug().setDebugLevel(0);
		/* Initialise MPI */
		MPI().Init(this);
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();

		if (rank == 0) {
			JGFInstrumentor.printHeader(3, 1, nprocess);
		}
		JGFrun(1);

		/* Finalise MPI */
		MPI().Finalize();

	}
}
