package ee.ut.f2f.mpi.examples.pi;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class Pi extends MPITask {
	/**
	 * Calculates pi on 4 (master + 3 peers) computers, with 2 tasks per peer ( 1 + 3 * 2 ).
	 */
	public void runTask() {
		getMPIDebug().println("Starting calculating Pi : " + getTaskID());
		int rank, size;
		double PI25DT = 3.141592653589793238462643;
		double h, sum, x;
		MPI().Init();
		size = MPI().COMM_WORLD().Size();
		rank = MPI().COMM_WORLD().Rank();

		int[] n = new int[1];
		double[] mypi = new double[1];
		double[] pi = new double[1];

		if (rank == 0) {
			n[0] = 1000000; // number of interval
		} else {
			n[0] = 1234567; // number of interval
		}

		MPI().COMM_WORLD().Bcast(n, 0, 1, MPI.INT, 0);// Send n to every peer

		h = 1.0 / (double) n[0];
		sum = 0.0;
		for (int i = rank + 1; i <= n[0]; i += size) {
			x = h * ((double) i - 0.5);
			sum += (4.0 / (1.0 + x * x));
		}
		mypi[0] = h * sum;

		// Collect results
		MPI().COMM_WORLD().Reduce(mypi, 0, pi, 0, 1, MPI.DOUBLE, MPI.SUM, 0);

		getMPIDebug().println("My part of Pi was " + mypi[0]);
		if (rank == 0) {
			getMPIDebug().println("Pi is approximately " + pi[0]);
			getMPIDebug().println("Error is " + (pi[0] - PI25DT));
		}
		MPI().Finalize();
	}
}
