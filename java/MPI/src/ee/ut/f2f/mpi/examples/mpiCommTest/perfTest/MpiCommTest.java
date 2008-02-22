package ee.ut.f2f.mpi.examples.mpiCommTest.perfTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class MpiCommTest extends CommTestMain {

	public MpiCommTest(MPITask task) {
		super(task);
	}

	public void taskBody() {
		int[] BUF_SIZE = new int[11];
		BUF_SIZE[0] = 1024;
		BUF_SIZE[1] = 2048;
		BUF_SIZE[2] = 4096;
		BUF_SIZE[3] = 8192;
		BUF_SIZE[4] = 16384;
		BUF_SIZE[5] = 32768;
		BUF_SIZE[6] = 65536;
		BUF_SIZE[7] = 131072;
		BUF_SIZE[8] = 262144;
		BUF_SIZE[9] = 524288;
		BUF_SIZE[10] = 1048576;

		for (int x = 0; x < BUF_SIZE.length; x++) {
			byte[] sendBuf = new byte[BUF_SIZE[x]];
			byte[] recvBuf = new byte[BUF_SIZE[x]];
			if (rank == 0) {
				long start = System.currentTimeMillis();
				MPI().COMM_WORLD().Send(sendBuf, 0, BUF_SIZE[x], MPI.BYTE, 1, 2);
				MPI().COMM_WORLD().Recv(recvBuf, 0, BUF_SIZE[x], MPI.BYTE, 1, 2);
				long stop = System.currentTimeMillis();
				double timeusage = stop - start;
				getMPIDebug().println("=============Loop " + x + "==========");
				double datatransfer = (BUF_SIZE[x] * 2);
				double throughput = (datatransfer * 1000) / timeusage; // B/s
				getMPIDebug().println("For Data = " + datatransfer + "B");
				getMPIDebug().println("Time Usage = " + timeusage + "ms");
				getMPIDebug().println("ThroughPut = " + throughput + "  B/s");
				getMPIDebug().println("ThroughPut = " + (throughput / 1024) + "  kB/s");
				getMPIDebug().println("ThroughPut = " + (throughput / 1048576) + "  MB/s");
				getMPIDebug().println("======================================");
			} else if (rank == 1) {
				MPI().COMM_WORLD().Recv(recvBuf, 0, BUF_SIZE[x], MPI.BYTE, 0, 2);
				MPI().COMM_WORLD().Send(recvBuf, 0, BUF_SIZE[x], MPI.BYTE, 0, 2);
			}
		}
	}
}
