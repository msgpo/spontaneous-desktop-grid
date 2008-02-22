package ee.ut.f2f.mpi.examples.mpiCommTest.individual;

/*
 MPI-Java version :
 Sang Lim (slim@npac.syr.edu)
 Northeast Parallel Architectures Center at Syracuse University
 12/2/98
 */

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class Alltoallv extends CommTestMain {

	public Alltoallv(MPITask task) {
		super(task);
	}

	public void taskBody() {
		int i = 0, j, stride = 15;
		int out[] = new int[size * stride];
		int in[] = new int[size * stride];
		int sdis[] = new int[size];
		int scount[] = new int[size];
		int rdis[] = new int[size];
		int rcount[] = new int[size];
		int ans[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44 };

		for (i = 0; i < size; i++) {
			sdis[i] = i * stride;
			scount[i] = 15;
			rdis[i] = i * 15;
			rcount[i] = 15;
		}

		if (rank == 0)
			for (i = 0; i < size; i++)
				scount[i] = 10;

		rcount[0] = 10;
		for (j = 0; j < size; j++)
			for (i = 0; i < stride; i++) {
				out[i + j * stride] = i + rank * stride;
				in[i + j * stride] = 0;
			}
		MPI().COMM_WORLD().Alltoallv(out, 0, scount, sdis, MPI.INT, in, 0, rcount, rdis, MPI.INT);
		for (i = 0; i < size * stride; i++)
			if (ans[i] != in[i])
				getMPIDebug().println("recived data : " + in[i] + "at [" + i + "]  should be : " + ans[i] + " on proc. : " + rank);

		MPI().COMM_WORLD().Barrier();
		if (rank == 0)
			getMPIDebug().println("Alltoallv TEST COMPLETE\n");

	}
}
