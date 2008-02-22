package ee.ut.f2f.mpi.examples.mpiCommTest.individual;

/*
 MPI-Java version :
 Sang Lim (slim@npac.syr.edu)
 Northeast Parallel Architectures Center at Syracuse University
 12/1/98
 */

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class Allgatherv extends CommTestMain {

	public Allgatherv(MPITask task) {
		super(task);
	}

	public void taskBody() {
		final int MAXLEN = 10;

		int i, j;
		int stride = 15;

		int out[] = new int[MAXLEN];
		int in[] = new int[MAXLEN * stride * size];
		int dis[] = new int[MAXLEN];
		int rcount[] = new int[MAXLEN];
		int ans[] = new int[MAXLEN * stride * size];

		// {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

		for (i = 0; i < MAXLEN; i++) {
			dis[i] = i * stride;
			rcount[i] = 5;
			out[i] = i;
		}
		rcount[0] = 10;

		for (i = 0; i < MAXLEN * size * stride; i++) {
			ans[i] = 0;
			in[i] = 0;
		}
		// Choopan added here to work more than 4 procs//
		for (i = 0; i < 10; i++) {
			ans[i] = i;
		}
		for (i = 0; i < MAXLEN * size * stride; i += stride) {
			for (j = 0; j < 5; j++) {
				ans[i + j] = j;
			}
		}
		// ////////////////////////////////////////////////

		if (rank == 0)
			MPI().COMM_WORLD().Allgatherv(out, 0, 10, MPI.INT, in, 0, rcount, dis, MPI.INT);
		else
			MPI().COMM_WORLD().Allgatherv(out, 0, 5, MPI.INT, in, 0, rcount, dis, MPI.INT);

		for (i = 0; i < size * stride; i++) {
			if (ans[i] != in[i]) {
				getMPIDebug().println("recived data : " + in[i] + " at [" + i + "] should be : " + ans[i] + " on proc. : " + rank);
				return;
			}
		}

		MPI().COMM_WORLD().Barrier();

		if (rank == 0)
			getMPIDebug().println("Allgatherv TEST COMPLETE\n");
	}
}
