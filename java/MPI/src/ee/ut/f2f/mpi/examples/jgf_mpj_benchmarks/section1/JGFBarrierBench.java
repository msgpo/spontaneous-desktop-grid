/**************************************************************************
 *                                                                         *
 *         Java Grande Forum Benchmark Suite - MPJ Version 1.0             *
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

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPIException;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil.JGFSection1;

public class JGFBarrierBench extends JGFSection1 {

	private static final int INITSIZE = 1;
	private static final int MAXSIZE = 1000000;
	private static final double TARGETTIME = 10.0;

	public JGFBarrierBench(MPITask task) {
		super(task);
	}

	public void JGFrun() throws MPIException {

		int size;
		double[] time = new double[1];

		/* Create the timer */
		if (rank == 0) {
			JGFInstrumentor.addTimer("Section1:Barrier", "barriers");
		}

		time[0] = 0.0;
		size = INITSIZE;

		MPI().COMM_WORLD().Barrier();

		/* Start the timer */
		while (time[0] < TARGETTIME && size < MAXSIZE) {
			if (rank == 0) {
				JGFInstrumentor.resetTimer("Section1:Barrier");
				JGFInstrumentor.startTimer("Section1:Barrier");
			}

			/* Carryout the barrier operation */
			for (int k = 0; k < size; k++) {
				MPI().COMM_WORLD().Barrier();
			}

			/* Stop the timer */
			if (rank == 0) {
				JGFInstrumentor.stopTimer("Section1:Barrier");
				time[0] = JGFInstrumentor.readTimer("Section1:Barrier");
				JGFInstrumentor.addOpsToTimer("Section1:Barrier", (double) size);
			}

			/* Broadcast time to the other processes */
			MPI().COMM_WORLD().Barrier();
			MPI().COMM_WORLD().Bcast(time, 0, 1, MPI.DOUBLE, 0);
			size *= 2;
		}

		/* Print the timing information */
		if (rank == 0) {
			JGFInstrumentor.printperfTimer("Section1:Barrier");
		}

	}

	public void runTask() {
		/* Initialise MPI */
		MPI().Init(this);
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();

		if (rank == 0) {
			JGFInstrumentor.printHeader(1, 0, nprocess);
		}
		JGFrun();
		/* Finalise MPI */
		MPI().Finalize();

	}

}
