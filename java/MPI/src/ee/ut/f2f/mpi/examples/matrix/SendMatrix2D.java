package ee.ut.f2f.mpi.examples.matrix;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class SendMatrix2D extends MPITask {
	public void runTask() {
		getMPIDebug().setDebugLevel(0);
		int x[][] = new int[10][10];
		int y[][] = new int[10][10];

		MPI().Init(this, 4, 2);
		int rank = MPI().COMM_WORLD().Rank();
		// Generate value for matrix X
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				x[i][j] = i;
				y[i][j] = 0;
			}
		}
		// Rank 0 sends matrix X, and Rank 1 store it in Y
		// Test to send 2 lines each time
		for (int i = 0; i < 10; i += 2) {
			if (rank == 0) {
				MPI().COMM_WORLD().Send(x, i, 2, MPI.OBJECT, 1, 10);
			} else {
				MPI().COMM_WORLD().Recv(y, i, 2, MPI.OBJECT, 0, 10);
			}
		}
		// Display value of matrix Y on Rank 1
		if (rank != 0) {
			for (int i = 0; i < 10; i++) {
				getMPIDebug().println("y[" + i + "][5] = " + y[i][5]);
			}
		}
		MPI().Finalize();
	}
}
