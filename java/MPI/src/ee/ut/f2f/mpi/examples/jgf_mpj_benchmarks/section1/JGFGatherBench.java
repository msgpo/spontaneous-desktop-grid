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
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.exceptions.MPIException;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil.JGFSection1;

public class JGFGatherBench extends JGFSection1 {

	private static final int INITSIZE = 1;
	private static final int MAXSIZE = 1000000;
	private static final double TARGETTIME = 10.0;
	private static final int MLOOPSIZE = 25;
	private static final int SMAX = 5000000;
	private static final int SMIN = 4;

	public JGFGatherBench(MPITask task) {
		super(task);
	}

	public void JGFrun() throws MPIException {

		int size, l, m_size;
		double logsize;
		double b_time;
		b_time = 0.0;
		double[] time = new double[1];

		m_size = 0;
		logsize = Math.log((double) SMAX) - Math.log((double) SMIN);

		/* Gather an array of doubles */

		/* Create the timers */
		if (rank == 0) {
			JGFInstrumentor.addTimer("Section1:Gather:Double", "bytes");
			JGFInstrumentor.addTimer("Section1:Gather:Barrier", "barriers");
		}

		/* loop over no of different message sizes */
		for (l = 0; l < MLOOPSIZE; l++) {

			/* Initialize the sending data */
			m_size = (int) (Math.exp(Math.log((double) SMIN) + (double) ((double) l / (double) MLOOPSIZE * logsize)));
			double[] send_arr = new double[m_size];
			double[] recv_arr = new double[m_size * nprocess];
			time[0] = 0.0;
			size = INITSIZE;

			MPI().COMM_WORLD().Barrier();

			/* Start the timer */
			while (time[0] < TARGETTIME && size < MAXSIZE) {
				if (rank == 0) {
					JGFInstrumentor.resetTimer("Section1:Gather:Double");
					JGFInstrumentor.startTimer("Section1:Gather:Double");
				}

				/* Carryout the broadcast operation */
				for (int k = 0; k < size; k++) {
					MPI().COMM_WORLD().Gather(send_arr, 0, send_arr.length, MPI.DOUBLE, recv_arr, 0, send_arr.length, MPI.DOUBLE, 0);
					MPI().COMM_WORLD().Barrier();

				}

				/* Stop the timer. Note that this reports no of bytes sent per process */
				if (rank == 0) {
					JGFInstrumentor.stopTimer("Section1:Gather:Double");
					time[0] = JGFInstrumentor.readTimer("Section1:Gather:Double");
					JGFInstrumentor.addOpsToTimer("Section1:Gather:Double", (double) size * send_arr.length * 8);
				}

				/* Broadcast time to the other processes */
				MPI().COMM_WORLD().Barrier();
				MPI().COMM_WORLD().Bcast(time, 0, 1, MPI.DOUBLE, 0);
				size *= 2;
			}

			size /= 2;

			/* determine the cost of the Barrier, subtract the cost and write out the performance time */
			MPI().COMM_WORLD().Barrier();
			if (rank == 0) {
				JGFInstrumentor.resetTimer("Section1:Gather:Barrier");
				JGFInstrumentor.startTimer("Section1:Gather:Barrier");
			}

			for (int k = 0; k < size; k++) {
				MPI().COMM_WORLD().Barrier();
			}

			if (rank == 0) {
				JGFInstrumentor.stopTimer("Section1:Gather:Barrier");
				b_time = JGFInstrumentor.readTimer("Section1:Gather:Barrier");
				JGFInstrumentor.addTimeToTimer("Section1:Gather:Double", -b_time);
				JGFInstrumentor.printperfTimer("Section1:Gather:Double", send_arr.length);
			}

		}

		/* Gather an array of objects containing a double */

		/* Create the timer */
		if (rank == 0) {
			JGFInstrumentor.addTimer("Section1:Gather:Object", "objects");
		}

		/* loop over no of different message sizes */
		for (l = 0; l < MLOOPSIZE; l++) {

			/* Initialize the sending data */
			m_size = (int) (Math.exp(Math.log((double) SMIN) + (double) ((double) l / (double) MLOOPSIZE * logsize)));
			obj_double[] send_arr_obj = new obj_double[m_size];
			obj_double[] recv_arr_obj = new obj_double[m_size * nprocess];
			for (int k = 0; k < m_size; k++) {
				send_arr_obj[k] = new obj_double(0.0);
			}
			time[0] = 0.0;
			size = INITSIZE;

			MPI().COMM_WORLD().Barrier();

			/* Start the timer */
			while (time[0] < TARGETTIME && size < MAXSIZE) {
				if (rank == 0) {
					JGFInstrumentor.resetTimer("Section1:Gather:Object");
					JGFInstrumentor.startTimer("Section1:Gather:Object");
				}

				/* Carryout the broadcast operation */
				for (int k = 0; k < size; k++) {
					MPI().COMM_WORLD().Gather(send_arr_obj, 0, send_arr_obj.length, MPI.OBJECT, recv_arr_obj, 0, send_arr_obj.length, MPI.OBJECT, 0);
					MPI().COMM_WORLD().Barrier();

				}

				/* Stop the timer */
				if (rank == 0) {
					JGFInstrumentor.stopTimer("Section1:Gather:Object");
					time[0] = JGFInstrumentor.readTimer("Section1:Gather:Object");
					JGFInstrumentor.addOpsToTimer("Section1:Gather:Object", (double) size * send_arr_obj.length);
				}

				/* Broadcast time to the other processes */
				MPI().COMM_WORLD().Barrier();
				MPI().COMM_WORLD().Bcast(time, 0, 1, MPI.DOUBLE, 0);
				size *= 2;
			}

			size /= 2;

			/* determine the cost of the Barrier, subtract the cost and write out the performance time */
			MPI().COMM_WORLD().Barrier();
			if (rank == 0) {
				JGFInstrumentor.resetTimer("Section1:Gather:Barrier");
				JGFInstrumentor.startTimer("Section1:Gather:Barrier");
			}

			for (int k = 0; k < size; k++) {
				MPI().COMM_WORLD().Barrier();
			}

			if (rank == 0) {
				JGFInstrumentor.stopTimer("Section1:Gather:Barrier");
				b_time = JGFInstrumentor.readTimer("Section1:Gather:Barrier");
				JGFInstrumentor.addTimeToTimer("Section1:Gather:Object", -b_time);
				JGFInstrumentor.printperfTimer("Section1:Gather:Object", send_arr_obj.length);
			}

		}

	}

	public void runTask() {
		/* Initialise MPI */
		MPI().Init();
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
