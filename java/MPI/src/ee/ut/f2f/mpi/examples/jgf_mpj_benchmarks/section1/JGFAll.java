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
package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section1;

import ee.ut.f2f.core.mpi.MPIException;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil.JGFSection1;

public class JGFAll extends JGFSection1 {

	public void JGFrun() throws MPIException {
	}

	public void runTask() {
		getMPIDebug().setDebugLevel(0);
		MPI().Init(this);
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();

		if (rank == 0) {
			JGFInstrumentor.printHeader(1, 0, nprocess);
		}

		JGFAlltoallBench ata = new JGFAlltoallBench(this);
		ata.JGFrun();

		JGFBarrierBench ba = new JGFBarrierBench(this);
		ba.JGFrun();

		// this throws an exception if i un-comment the above tests.
		// the exception is thrown while
		JGFBcastBench bc = new JGFBcastBench(this);
		bc.JGFrun();

		// Hangs for objects
		JGFGatherBench ga = new JGFGatherBench(this);
		ga.JGFrun();

		// JGFPingPongBench pp = new JGFPingPongBench(rank, nprocess);
		// pp.JGFrun();

		JGFReduceBench rd = new JGFReduceBench(this);
		rd.JGFrun();

		JGFScatterBench sc = new JGFScatterBench(this);
		sc.JGFrun();

		/* Finalise MPI */
		MPI().Finalize();

		if (rank == 0) {
			getMPIDebug().println("JGFAll Section1 TEST COMPLETE");
		}

	}
}
