package ee.ut.f2f.mpi.examples.mpiCommTest.perfTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class AlltoAll extends CommTestMain {

	public AlltoAll(MPITask task) {
		super(task);
	}

	public void taskBody() {
		byte[] sendtable;
		byte[] recvtable;
		double[] mytimeusage = new double[1];
		double[] maxtimeusage = new double[1];

		for (int BASESIZE = 2; BASESIZE < 2000000; BASESIZE *= 2) {

			int tablesize = BASESIZE * size;
			sendtable = new byte[tablesize];
			recvtable = new byte[tablesize];

			for (int i = 0; i < tablesize; i++) {
				sendtable[i] = (byte) rank;
				recvtable[i] = 0;
			}

			double startTime = MPI().Wtime();
			MPI().COMM_WORLD().Alltoall(sendtable, 0, BASESIZE, MPI.BYTE, recvtable, 0, BASESIZE, MPI.BYTE);
			double stopTime = MPI().Wtime();
			mytimeusage[0] = stopTime - startTime;
			MPI().COMM_WORLD().Reduce(mytimeusage, 0, maxtimeusage, 0, 1, MPI.DOUBLE, MPI.MAX, 0);
			if (rank == 0)
				getMPIDebug().println("" + BASESIZE + "\t\t" + (maxtimeusage[0] / 1000.0));

		}
	}
}
