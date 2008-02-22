package ee.ut.f2f.mpi.examples.mpiCommTest.perfTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class Bcast extends CommTestMain {

	public Bcast(MPITask task) {
		super(task);
	}

	public static int VECTSIZE = 100;

	public void taskBody() {
		int root;
		int[] buff = new int[VECTSIZE];

		// -- single value broadcast
		for (root = 0; root < size; root++) {
			buff[0] = root;
			if (rank == root)
				getMPIDebug().println("[rank " + rank + "] Broadcast single value " + buff[0]);
			getMPIDebug().print("[rank " + rank + "] Root is now rank " + root + ". Expecting to receive " + root + ". Received ... ");

			MPI().COMM_WORLD().Bcast(buff, 0, 1, MPI.INT, root);
			getMPIDebug().print("" + buff[0]);

			// -- now all processes must have received root's buffer. Check this.
			if (buff[0] != root) {
				getMPIDebug().println("** Error: Bcast test failed: process " + rank + " received " + buff[0] + " when root broadcasted " + root);
				return;
			}
			getMPIDebug().println(". ok.");
		}
		// -- vector of values broadcast
		for (root = 0; root < size; root++) {
			int i;
			for (i = 0; i < VECTSIZE; i++)
				buff[i] = root;
			if (rank == root)
				getMPIDebug().println("[rank " + rank + "] Broadcast vector of values " + buff[VECTSIZE - 1]);
			getMPIDebug().print("[rank " + rank + "] Root is now rank " + root + ". Expecting to receive " + VECTSIZE + " values " + root + ". Received ... ");

			MPI().COMM_WORLD().Bcast(buff, 0, VECTSIZE, MPI.INT, root);
			getMPIDebug().print("" + buff[VECTSIZE - 1]);

			// -- now all processes must have received root's buffer. Check this.
			for (i = 0; i < VECTSIZE; i++) {
				if (buff[i] != root) {
					getMPIDebug().println("** Error: Bcast test failed: process " + rank + " received " + buff[i] + " at position " + i + "in buffer, when root broadcasted " + root);
					return;
				}
			}
			getMPIDebug().println(". ok.");
		}
	}
}
