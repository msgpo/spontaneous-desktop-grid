package ee.ut.f2f.mpi.examples.pi;

import java.util.Date;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class Pi extends MPITask {
	/**
	 * Calculates pi on 4 (master + 3 peers) computers, with 2 tasks per peer ( 1 + 3 * 2 ).
	 */
	public void runTask() {
		getMPIDebug().println("******************************************************** " + (new Date()));
		getMPIDebug().println("Start " + (new Date()));
		int rank, size, i;
		double PI25DT = 3.141592653589793238462643;
		double h, sum, x;
		MPI().Init(this, 4, 2);

		getMPIDebug().println("1");
		double startTime = MPI().Wtime();

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

		getMPIDebug().println("2 rank=" + rank + " size=" + size);
		MPI().COMM_WORLD().Bcast(n, 0, 1, MPI.INT, 0);

		getMPIDebug().println("3 n[0]=" + n[0]);
		h = 1.0 / (double) n[0];
		sum = 0.0;
		for (i = rank + 1; i <= n[0]; i += size) {
			x = h * ((double) i - 0.5);
			sum += (4.0 / (1.0 + x * x));
		}
		mypi[0] = h * sum;

		getMPIDebug().println("4 mypi[0]=" + mypi[0] + " h=" + h + " sum=" + sum);
		MPI().COMM_WORLD().Reduce(mypi, 0, pi, 0, 1, MPI.DOUBLE, MPI.SUM, 0);

		getMPIDebug().println("5 pi[0]=" + pi[0]);
		if (rank == 0) {
			getMPIDebug().println("Pi is approximately " + pi[0]);
			getMPIDebug().println("Error is " + (pi[0] - PI25DT));
			double stopTime = MPI().Wtime();
			getMPIDebug().println("Time usage = " + (stopTime - startTime) + " ms");
		}

		getMPIDebug().println("7");
		MPI().Finalize();
	}
}
