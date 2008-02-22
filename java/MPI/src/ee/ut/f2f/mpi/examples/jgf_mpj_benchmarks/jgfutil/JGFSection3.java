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

package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPIException;
import ee.ut.f2f.core.mpi.MPITask;

public abstract class JGFSection3 extends MPITask {
	public JGFInstrumentor JGFInstrumentor = new JGFInstrumentor(this);
	MPITask task = null;
	public int nprocess = 0;
	public int rank = 0;

	public JGFSection3() {
		super();
	}

	public JGFSection3(MPITask task) {
		this.task = task;
		JGFInstrumentor = ((JGFSection3) task).JGFInstrumentor;
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();
	}

	public MPI MPI() {
		if (task != null) {
			return task.MPI();
		}
		return super.MPI();
	}

	public abstract void JGFsetsize(int size);

	public abstract void JGFinitialise() throws MPIException;

	public abstract void JGFapplication() throws MPIException;

	public abstract void JGFvalidate();

	public abstract void JGFtidyup();

	public abstract void JGFrun(int size) throws MPIException;
}
