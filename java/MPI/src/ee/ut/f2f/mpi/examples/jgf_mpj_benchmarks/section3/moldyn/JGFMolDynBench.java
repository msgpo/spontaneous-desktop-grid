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
package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3.moldyn;

import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.exceptions.MPIException;

public class JGFMolDynBench extends md {

	public JGFMolDynBench() {
		super();
	}

	public JGFMolDynBench(MPITask task) {
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
			JGFInstrumentor.startTimer("Section3:MolDyn:Run");
		}

		runiters();

		MPI().COMM_WORLD().Barrier();
		if (rank == 0) {
			JGFInstrumentor.stopTimer("Section3:MolDyn:Run");
		}
	}

	public void JGFvalidate() {
		double refval[] = { 1731.4306625334357, 7397.392307839352 };
		double dev = Math.abs(ek - refval[size]);
		if (dev > 1.0e-12) {
			getMPIDebug().println("Validation failed");
			getMPIDebug().println("Kinetic Energy = " + ek + "  " + dev + "  " + size);
		}
	}

	public void JGFtidyup() {

		one = null;
		System.gc();
	}

	public void JGFrun(int size) throws MPIException {

		if (rank == 0) {
			JGFInstrumentor.addTimer("Section3:MolDyn:Total", "Solutions", size);
			JGFInstrumentor.addTimer("Section3:MolDyn:Run", "Interactions", size);
		}

		JGFsetsize(size);

		if (rank == 0) {
			JGFInstrumentor.startTimer("Section3:MolDyn:Total");
		}

		JGFinitialise();
		JGFapplication();
		JGFvalidate();
		JGFtidyup();

		if (rank == 0) {
			JGFInstrumentor.stopTimer("Section3:MolDyn:Total");

			JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Run", (double) interactions);
			JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Total", 1);

			JGFInstrumentor.printTimer("Section3:MolDyn:Run");
			JGFInstrumentor.printTimer("Section3:MolDyn:Total");
		}

	}

}
