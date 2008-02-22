package ee.ut.f2f.mpi.examples.mpiCommTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPIDebug;
import ee.ut.f2f.core.mpi.MPITask;

public abstract class CommTestMain extends MPITask {
	public int rank = 0;
	public int size = 0;
	MPITask task = null;

	public CommTestMain() {
		super();
	}

	/**
	 * a Constructor for running multiply task at the same time
	 * 
	 * @param task
	 */
	public CommTestMain(MPITask task) {
		this.task = task;
		rank = MPI().COMM_WORLD().Rank();
		size = MPI().COMM_WORLD().Size();
	}

	public MPI MPI() {
		if (task != null) {
			return task.MPI();
		}
		return super.MPI();
	}

	public void runTask() {
		MPI().Init(this);
		rank = MPI().COMM_WORLD().Rank();
		size = MPI().COMM_WORLD().Size();
		getMPIDebug().setDebugLevel(0);
		taskBody();
		MPI().Finalize();
	}

	public MPIDebug getMPIDebug() {
		if (task != null) {
			return task.getMPIDebug();
		}
		return super.getMPIDebug();
	}

	public abstract void taskBody();
}
