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
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.exceptions.MPIException;

public abstract class JGFSection1 extends MPITask {

	public final int INITSIZE = 10000;
	public final int MAXSIZE = 1000000000;
	public final double TARGETTIME = 1.0;
	public JGFInstrumentor JGFInstrumentor = new JGFInstrumentor(this);
	MPITask task = null;

	public int nprocess = 0;
	public int rank = 0;

	public JGFSection1() {
		super();
	}

	public JGFSection1(MPITask task) {
		this.task = task;
		JGFInstrumentor = ((JGFSection1) task).JGFInstrumentor;
		rank = MPI().COMM_WORLD().Rank();
		nprocess = MPI().COMM_WORLD().Size();
	}

	public MPI MPI() {
		if (task != null) {
			return task.MPI();
		}
		return super.MPI();
	}

	public abstract void JGFrun() throws MPIException;

}
