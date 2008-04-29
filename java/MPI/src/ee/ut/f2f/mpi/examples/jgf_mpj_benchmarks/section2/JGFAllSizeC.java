/**************************************************************************
 *
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

import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil.JGFSection2;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section2.crypt.JGFCryptBench;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section2.lufact.JGFLUFactBench;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section2.series.JGFSeriesBench;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section2.sor.JGFSORBench;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section2.sparsematmult.JGFSparseMatmultBench;

public class JGFAllSizeC extends JGFSection2 {

	public void runTask() {
		getMPIDebug().setDebugLevel(0);
		/* Initialise MPI */
		MPI().Init();
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();

		int size = 2;

		if (rank == 0) {
			JGFInstrumentor.printHeader(2, 2, nprocess);
		}
		JGFSeriesBench se = new JGFSeriesBench(this);
		se.JGFrun(size);

		JGFLUFactBench lub = new JGFLUFactBench(this);
		lub.JGFrun(size);

		JGFCryptBench cb = new JGFCryptBench(this);
		cb.JGFrun(size);

		JGFSORBench jb = new JGFSORBench(this);
		jb.JGFrun(size);

		JGFSparseMatmultBench smm = new JGFSparseMatmultBench(this);
		smm.JGFrun(size);

		/* Finalise MPI */
		MPI().Finalize();

		if (rank == 0) {
			getMPIDebug().println("JGFAllSizeC section2 TEST COMPLETE");
		}
	}
}
