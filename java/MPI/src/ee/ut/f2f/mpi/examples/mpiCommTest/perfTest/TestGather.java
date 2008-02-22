package ee.ut.f2f.mpi.examples.mpiCommTest.perfTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class TestGather extends CommTestMain {

	public TestGather(MPITask task) {
		super(task);
	}

	public void taskBody() {
		int BUFSIZE = 2;
		int[] result;
		int[] newresult;
		int[] myBuf = new int[BUFSIZE];
		result = new int[size * BUFSIZE];
		newresult = new int[size * BUFSIZE];
		// init
		for (int i = 0; i < BUFSIZE; i++) {
			myBuf[i] = rank;
		}
		// ----------------------- Test GATHER ---------------------------------
		// Each process fills a buffer myBuff with its rank (BUFFSIZE times)
		// Gather to rank 0 into result array
		MPI().COMM_WORLD().Gather(myBuf, 0, BUFSIZE, MPI.INT, result, 0, BUFSIZE, MPI.INT, 0);

		if (rank == 0) {
			getMPIDebug().print("After Gather           :\t");
			for (int i = 0; i < size * BUFSIZE; i++) {
				getMPIDebug().print("" + result[i] + ",");
			}
			getMPIDebug().println("");
		}
		// ---------------------- Test ALLGATHER --------------------------------
		// reset result buffer
		for (int i = 0; i < size * BUFSIZE; i++) {
			result[i] = 0;
		}

		// On each process, data received from process j is stored at result[j]
		// e.g. for 3 processes, BUFFSIZE=2
		// before: p0 [0 0] p1 [1 1] p2 [2 2]
		// after : p0 [0 0 1 1 2 2] p1 [0 0 1 1 2 2] p2 [0 0 1 1 2 2]
		MPI().COMM_WORLD().Allgather(myBuf, 0, BUFSIZE, MPI.INT, result, 0, BUFSIZE, MPI.INT);

		// send result to rank 0 to test value
		// perform a vector-wise sum of each process and puts it on process 0
		// e.g. for 3 processes, BUFFSIZE=2
		// before: p0 [0 0 1 1 2 2] p1 [0 0 1 1 2 2] p2 [0 0 1 1 2 2]
		// after : p0 [0 0 3 3 6 6]
		MPI().COMM_WORLD().Reduce(result, 0, newresult, 0, BUFSIZE * size, MPI.INT, MPI.SUM, 0);
		if (rank == 0) {
			getMPIDebug().print("After Allgather+Reduce :\t");
			for (int i = 0; i < size * BUFSIZE; i++) {
				getMPIDebug().print("" + newresult[i] + ",");
			}
			getMPIDebug().println("");
		}
		// ---------------------------TEST SCATTER------------------------------------
		// clear myBuf,result
		for (int i = 0; i < BUFSIZE; i++) {
			myBuf[i] = 0;
		}
		for (int i = 0; i < size * BUFSIZE; i++) {
			result[i] = 0;
		}

		MPI().COMM_WORLD().Scatter(newresult, 0, BUFSIZE, MPI.INT, myBuf, 0, BUFSIZE, MPI.INT, 0);

		MPI().COMM_WORLD().Allgather(myBuf, 0, BUFSIZE, MPI.INT, result, 0, BUFSIZE, MPI.INT);
		if (rank == 0) {
			getMPIDebug().print("After Scatter+Allgather:\t");
			for (int i = 0; i < size * BUFSIZE; i++) {
				getMPIDebug().print("" + result[i] + ",");
			}
			getMPIDebug().println("");
		}
	}
}
