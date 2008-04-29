package ee.ut.f2f.mpi.examples.fdTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class FDTest extends MPITask {

	public void runTask() {
		// This BUF takes to much memory
		int BUF_SIZE = 10000;
		int numloop = 0;

		MPI().Init(3);
		int myRank = MPI().COMM_WORLD().Rank();

		long[] sendBuf = new long[BUF_SIZE];
		long[] recvBuf = new long[BUF_SIZE];
		while (true) {
			numloop++;
			if (myRank == 0) {
				getMPIDebug().println("Assign new value for sendBuf");
				for (int i = 0; i < BUF_SIZE; i++) {
					sendBuf[i] = numloop;
				}

				getMPIDebug().println("================================================");
				getMPIDebug().println("Send/Recv Loop: " + numloop);
				getMPIDebug().println("================================================");

				MPI().COMM_WORLD().Send(sendBuf, 0, BUF_SIZE, MPI.LONG, 1, 2);
				MPI().COMM_WORLD().Recv(recvBuf, 0, BUF_SIZE, MPI.LONG, 1, 2);

				// ///////// Verification the result of recvBuf with sendBuf ///////////
				getMPIDebug().println("Recv[0] = " + recvBuf[0]);
				boolean passed = true;
				for (int i = 0; i < BUF_SIZE; i++) {
					if (recvBuf[i] != numloop) {
						passed = false;
						break;
					}
				}
				if (passed) {
					getMPIDebug().println("The verification is successful");
				} else {
					getMPIDebug().println("The verification is FAILED!!");
				}
				getMPIDebug().println("================================================\n");
				// //////////////////////////////////////////////////////////////////////

			} else {
				getMPIDebug().println("wait for result");
				MPI().COMM_WORLD().Recv(recvBuf, 0, BUF_SIZE, MPI.LONG, 0, 2);
				MPI().COMM_WORLD().Send(recvBuf, 0, BUF_SIZE, MPI.LONG, 0, 2);
			}
			wait(100);
			if (numloop > 10) {
				break;
			}
		}
		MPI().Finalize();
	}
}
