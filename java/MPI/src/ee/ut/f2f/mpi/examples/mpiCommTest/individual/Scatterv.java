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

public class Scatterv extends CommTestMain {

	public Scatterv(MPITask task) {
		super(task);
	}

	public void taskBody() {
		final int MAXLEN = 10;
		int i = 0, stride = 15;
		int out[] = new int[size * stride];
		int in[] = new int[MAXLEN];
		int dis[] = new int[MAXLEN];
		int scount[] = new int[MAXLEN];

		for (i = 0; i < MAXLEN; i++) {
			dis[i] = i * stride;
			scount[i] = 5;
			in[i] = 0;
		}
		scount[0] = 10;
		for (i = 0; i < size * stride; i++)
			out[i] = i;
		MPI().COMM_WORLD().Scatterv(out, 0, scount, dis, MPI.INT, in, 0, scount[rank], MPI.INT, 0);
		String[] messbuf = new String[1];

		if (rank == 0) {
			getMPIDebug().println("Original array on root...");
			for (i = 0; i < size * stride; i++)
				getMPIDebug().print(out[i] + " ");
			getMPIDebug().println("");
			getMPIDebug().println("Result on proc 0...");
			getMPIDebug().println("Stride = 15 " + "Count = " + scount[0]);
			for (i = 0; i < MAXLEN; i++)
				getMPIDebug().print(in[i] + " ");
			getMPIDebug().println("");
			// Reproduces output of original test case, but deterministically
			int nmess = size < 3 ? size : 3;
			for (int t = 1; t < nmess; t++) {
				MPI().COMM_WORLD().Recv(messbuf, 0, 1, MPI.STRING, t, 0);
				getMPIDebug().print(messbuf[0]+ " ");
			}
			getMPIDebug().println("");
		}

		if (rank == 1) {
			StringBuffer mess = new StringBuffer();
			mess.append("Result on proc 1...\n");
			mess.append("Stride = 15 " + "Count = " + scount[1] + "\n");
			for (i = 0; i < MAXLEN; i++)
				mess.append(in[i] + " ");
			mess.append("\n");
			messbuf[0] = mess.toString();
			MPI().COMM_WORLD().Send(messbuf, 0, 1, MPI.STRING, 0, 0);
		}

		if (rank == 2) {
			StringBuffer mess = new StringBuffer();
			mess.append("Result on proc 2...\n");
			mess.append("Stride = 15 " + "Count = " + scount[2] + "\n");
			for (i = 0; i < MAXLEN; i++)
				mess.append(in[i] + " ");
			mess.append("\n");
			messbuf[0] = mess.toString();
			MPI().COMM_WORLD().Send(messbuf, 0, 1, MPI.STRING, 0, 0);
		}

		if (rank == 0)
			getMPIDebug().println("Scatterv TEST COMPLETE\n");

	}
}
