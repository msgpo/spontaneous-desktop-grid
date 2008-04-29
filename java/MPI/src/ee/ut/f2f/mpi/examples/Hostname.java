package ee.ut.f2f.mpi.examples;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class Hostname extends MPITask {
	/**
	 * Ask for the name of the working peers (itself and first node)
	 */
	public void runTask() {
		int rank, size;
		MPI().Init();

		rank = MPI().COMM_WORLD().Rank();
		size = MPI().COMM_WORLD().Size();
		String rcvbuf[] = new String[size];
		String sndbuf[] = new String[1];
		sndbuf[0] = MPI().Get_processor_name();
		getMPIDebug().println("*)" + sndbuf[0]);
		MPI().COMM_WORLD().Gather(sndbuf, 0, 1, MPI.STRING, rcvbuf, 0, 1, MPI.STRING, 0);
		if (rank == 0) {
			for (int i = 0; i < size; i++) {
				getMPIDebug().println(i + "-" + rcvbuf[i]);
			}
		}
		MPI().Finalize();
	}
}
