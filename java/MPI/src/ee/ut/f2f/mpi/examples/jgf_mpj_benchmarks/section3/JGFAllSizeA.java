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

import ee.ut.f2f.core.mpi.MPIException;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil.JGFSection3;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3.moldyn.JGFMolDynBench;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3.montecarlo.JGFMonteCarloBench;
import ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.section3.raytracer.JGFRayTracerBench;

public class JGFAllSizeA extends JGFSection3 {

	public void runTask() {
		getMPIDebug().setDebugLevel(0);
		/* Initialise MPI */
		MPI().Init(this);
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();

		int size = 0;

		if (rank == 0) {
			JGFInstrumentor.printHeader(3, 0, nprocess);
		}

		JGFMolDynBench mold = new JGFMolDynBench(this);
		mold.JGFrun(size);

		JGFMonteCarloBench mc = new JGFMonteCarloBench(this);
		mc.JGFrun(size);

		JGFRayTracerBench rtb = new JGFRayTracerBench(this);
		rtb.JGFrun(size);

		/* Finalise MPI */
		MPI().Finalize();

		if (rank == 0) {
			getMPIDebug().println("JGFAllSizeA (section3) TEST COMPLETE");
		}

	}

	public void JGFsetsize(int size) {
	};

	public void JGFinitialise() throws MPIException {
	};

	public void JGFapplication() throws MPIException {
	};

	public void JGFvalidate() {
	};

	public void JGFtidyup() {
	};

	public void JGFrun(int size) throws MPIException {
	};
}
