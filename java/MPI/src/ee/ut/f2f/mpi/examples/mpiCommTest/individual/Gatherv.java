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

public class Gatherv extends CommTestMain {

	public Gatherv(MPITask task) {
		super(task);
	}

	public void taskBody() {
		final int MAXLEN = 10;
		int i;
		int stride = 15;

		int out[] = new int[MAXLEN];
		int in[] = new int[MAXLEN * stride * size];
		int dis[] = new int[MAXLEN];
		int rcount[] = new int[MAXLEN];
		int ans[] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		for (i = 0; i < MAXLEN; i++) {
			dis[i] = i * stride;
			rcount[i] = 5;
			out[i] = 1;
		}
		rcount[0] = 10;

		for (i = 0; i < MAXLEN * size * stride; i++) {
			in[i] = 0;
		}

		if (rank == 0)
			MPI().COMM_WORLD().Gatherv(out, 0, 10, MPI.INT, in, 0, rcount, dis, MPI.INT, 0);
		else
			MPI().COMM_WORLD().Gatherv(out, 0, 5, MPI.INT, in, 0, rcount, dis, MPI.INT, 0);

		if (rank == 0) {
			for (i = 0; i < size * stride; i++)
				if (ans[i] != in[i])
					getMPIDebug().println("recived data : " + in[i] + "at [" + i + "] should be : " + ans[i]);
		}

		MPI().COMM_WORLD().Barrier();
		if (rank == 0)
			getMPIDebug().println("Gatherv TEST COMPLETE\n");

	}
}
