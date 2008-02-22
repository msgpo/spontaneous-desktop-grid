package ee.ut.f2f.mpi.examples.mpiCommTest.perfTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class TestReduceScatter extends CommTestMain {

	public TestReduceScatter(MPITask task) {
		super(task);
	}

	public void taskBody() {
		final int BUF = 4;
		int[] sendBuf = new int[size * BUF];
		int[] recvBuf = new int[BUF];

		// Init sendBuf
		for (int i = 0; i < sendBuf.length; i++) {
			sendBuf[i] = i;
		}
		// Init recvBuf
		for (int i = 0; i < recvBuf.length; i++) {
			recvBuf[i] = 0;
		}
		int[] recvCount = new int[size];
		for (int i = 0; i < size; i++) {
			recvCount[i] = BUF;
		}
		MPI().COMM_WORLD().Reduce_scatter(sendBuf, 0, recvBuf, 0, recvCount, MPI.INT, MPI.SUM);
		if (rank == 0) {
			getMPIDebug().println("=== Rank 0 ===");
			for (int j = 0; j < BUF; j++) {
				getMPIDebug().println("%4d " + recvBuf[j]);
			}
			getMPIDebug().println("");
			for (int i = 1; i < size; i++) {
				MPI().COMM_WORLD().Recv(recvBuf, 0, BUF, MPI.INT, i, 100);
				getMPIDebug().println("=== Rank " + i + " ===");
				for (int j = 0; j < BUF; j++) {
					getMPIDebug().println("%4d " + recvBuf[j]);
				}
				getMPIDebug().println("");
			}
		} else {
			MPI().COMM_WORLD().Send(recvBuf, 0, BUF, MPI.INT, 0, 100);
		}
	}
}
