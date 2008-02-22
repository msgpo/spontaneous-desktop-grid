package ee.ut.f2f.mpi.examples.mpiCommTest.individual;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class SendRecv extends CommTestMain {

	public SendRecv(MPITask task) {
		super(task);
	}

	public void taskBody() {
		int[][] sendData = new int[1][1];
		int[][] recvData = new int[1][1];
		int TAG = 1;

		sendData[0][0] = rank;

		int source;
		if (rank == 0) {
			source = size - 1;
		} else {
			source = rank - 1;
		}
		MPI().COMM_WORLD().Sendrecv(sendData[0], 0, 1, MPI.INT, (rank + 1) % size, TAG, recvData[0], 0, 1, MPI.INT, source, TAG);
		getMPIDebug().println("Rank = " + rank + ": got data = " + recvData[0][0]);

	}
}
