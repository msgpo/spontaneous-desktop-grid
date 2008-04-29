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
package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3.montecarlo;

// package montecarlo;

import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.exceptions.MPIException;

public class JGFMonteCarloBench extends CallAppDemo {

	public JGFMonteCarloBench() {
		super();
	}

	public JGFMonteCarloBench(MPITask task) {
		super(task);
	}

	public void runTask() {
	}

	public void JGFsetsize(int size) {
		this.size = size;
	}

	public void JGFinitialise() {

		initialise();

	}

	public void JGFapplication() throws MPIException {

		MPI().COMM_WORLD().Barrier();
		if (rank == 0) {
			JGFInstrumentor.startTimer("Section3:MonteCarlo:Run");
		}
		runiters();

		MPI().COMM_WORLD().Barrier();
		if (rank == 0) {
			JGFInstrumentor.stopTimer("Section3:MonteCarlo:Run");
		}

		if (rank == 0) {
			presults();
		}

	}

	public void JGFvalidate() {
		double refval[] = { -0.0333976656762814, -0.03215796752868655 };
		double dev = Math.abs(AppDemo.JGFavgExpectedReturnRateMC - refval[size]);
		if (dev > 1.0e-12) {
			getMPIDebug().println("Validation failed");
			getMPIDebug().println(" expectedReturnRate= " + AppDemo.JGFavgExpectedReturnRateMC + "  " + dev + "  " + size);
		}
	}

	public void JGFtidyup() {

		System.gc();
	}

	public void JGFrun(int size) throws MPIException {

		if (rank == 0) {
			JGFInstrumentor.addTimer("Section3:MonteCarlo:Total", "Solutions", size);
			JGFInstrumentor.addTimer("Section3:MonteCarlo:Run", "Samples", size);
		}

		JGFsetsize(size);

		MPI().COMM_WORLD().Barrier();
		if (rank == 0) {
			JGFInstrumentor.startTimer("Section3:MonteCarlo:Total");
		}

		JGFinitialise();
		JGFapplication();

		if (rank == 0) {
			JGFvalidate();
		}
		JGFtidyup();

		MPI().COMM_WORLD().Barrier();
		if (rank == 0) {
			JGFInstrumentor.stopTimer("Section3:MonteCarlo:Total");

			JGFInstrumentor.addOpsToTimer("Section3:MonteCarlo:Run", (double) input[1]);
			JGFInstrumentor.addOpsToTimer("Section3:MonteCarlo:Total", 1);

			JGFInstrumentor.printTimer("Section3:MonteCarlo:Run");
			JGFInstrumentor.printTimer("Section3:MonteCarlo:Total");
		}
	}

}
