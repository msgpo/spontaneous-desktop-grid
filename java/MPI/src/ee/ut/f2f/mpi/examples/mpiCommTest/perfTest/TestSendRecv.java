package ee.ut.f2f.mpi.examples.mpiCommTest.perfTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class TestSendRecv extends CommTestMain {

	public TestSendRecv(MPITask task) {
		super(task);
	}

	public void taskBody() {
		if (size != 2) {
			getMPIDebug().println("Run only with 2 processes");
			return;
		}
		int MAXBUF = 8;
		int TAG = 1;
		int[] sendBuf = new int[MAXBUF];
		int[] recvBuf = new int[MAXBUF];

		// Test Send and Receive message of same size
		if (rank == 0) {
			// Clear recvBuf
			for (int i = 0; i < MAXBUF; i++) {
				recvBuf[i] = 0;
			}
			MPI().COMM_WORLD().Recv(recvBuf, 0, MAXBUF, MPI.INT, 1, TAG);
			getMPIDebug().println("SendCount = RecvCount");
			getMPIDebug().println("RecvData = ");
			for (int i = 0; i < MAXBUF; i++) {
				getMPIDebug().println("%4d " + recvBuf[i]);
			}
			getMPIDebug().println("");

		} else {
			// Set sendBuf
			for (int i = 0; i < MAXBUF; i++) {
				sendBuf[i] = i;
			}
			MPI().COMM_WORLD().Send(sendBuf, 0, MAXBUF, MPI.INT, 0, TAG);
		}

		// Test Send message is smaller than Receive message
		if (rank == 0) {
			// Clear recvBuf
			for (int i = 0; i < MAXBUF; i++) {
				recvBuf[i] = 0;
			}
			MPI().COMM_WORLD().Recv(recvBuf, 0, MAXBUF, MPI.INT, 1, TAG);
			getMPIDebug().println("SendCount(" + MAXBUF + ") < RecvCount(" + (MAXBUF / 2) + ")");
			getMPIDebug().println("RecvData = ");
			for (int i = 0; i < MAXBUF; i++) {
				getMPIDebug().println("%4d " + recvBuf[i]);
			}
			getMPIDebug().println("");

		} else {
			// Set sendBuf
			for (int i = 0; i < MAXBUF; i++) {
				sendBuf[i] = i;
			}
			MPI().COMM_WORLD().Send(sendBuf, 0, MAXBUF / 2, MPI.INT, 0, TAG);
		}

		// Test Send message is bigger than Receive message
		if (rank == 0) {
			// Clear recvBuf
			for (int i = 0; i < MAXBUF; i++) {
				recvBuf[i] = 0;
			}
			MPI().COMM_WORLD().Recv(recvBuf, 0, MAXBUF / 2, MPI.INT, 1, TAG);
			getMPIDebug().println("SendCount(" + MAXBUF + ") < RecvCount(" + (MAXBUF / 2) + ")");
			getMPIDebug().println("RecvData = ");
			for (int i = 0; i < MAXBUF; i++) {
				getMPIDebug().println("%4d " + recvBuf[i]);
			}
			getMPIDebug().println("");

		} else {
			// Set sendBuf
			for (int i = 0; i < MAXBUF; i++) {
				sendBuf[i] = i;
			}
			MPI().COMM_WORLD().Send(sendBuf, 0, MAXBUF, MPI.INT, 0, TAG);
		}
	}
}
