package ee.ut.f2f.mpi.examples.ring;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class Ring extends MPITask {
	public void runTask() {
		int rank, size, i;
		int MSGTYPE = 1;

		MPI().Init(this);
		double startTime = MPI().Wtime();

		size = MPI().COMM_WORLD().Size();
		rank = MPI().COMM_WORLD().Rank();

		int[] r = new int[1];
		int[] s = new int[1];

		// forward my rank
		s[0] = rank;
		for (i = 0; i < size; i++) {
			getMPIDebug().println("[rank " + rank + "] sending to [rank " + (rank + 1) % size + "]");
			MPI().COMM_WORLD().Send(s, 0, 1, MPI.INT, (rank + 1) % size, MSGTYPE);
			if (rank == 0)
				MPI().COMM_WORLD().Recv(r, 0, 1, MPI.INT, size - 1, MSGTYPE);
			else
				MPI().COMM_WORLD().Recv(r, 0, 1, MPI.INT, rank - 1, MSGTYPE);
			s[0] = r[0];
		}

		getMPIDebug().println("[rank " + rank + "] finally has value " + r[0]);
		double stopTime = MPI().Wtime();
		getMPIDebug().println("Time usage = " + (stopTime - startTime) + " ms");
		MPI().Finalize();
	}
}
