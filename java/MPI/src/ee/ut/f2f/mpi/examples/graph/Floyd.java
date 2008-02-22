package ee.ut.f2f.mpi.examples.graph;

/**
 *   Floyd-Warshall's all-pairs shortest path
 *
 *   Given an nxn matrix of distances between pairs of
 *   vertices, this MPI program computes the shortest path
 *   between every pair of vertices.
 *
 *   This program shows:
 *      how to dynamically allocate multidimensional arrays
 *      how one process can handle I/O for the others
 *      broadcasting of a vector of elements
 *      messages with different tags
 *
 *   Adapted from the C program from Michael J. Quinn
 *
 *   parallelization: Given the matrix cost (a_{i,j} is the cost to go from i to j), 
 *   in the  k th step, each task requires, in addition to its local data, 
 *   the values , a_{k,0}, ..., a_{k,n-1}, that is, the  k th row of  a. 
 *   Hence, we specify that the task with this row broadcast it to all other tasks
 **/

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class Floyd extends MPITask {
/*
	int a[][] = { { 0, 99, 1, 1, 1, 1, 1, 1, 1, 99 }, 
				  { 99, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, 
				  { 99, 1, 0, 1, 1, 1, 1, 1, 1, 1 }, 
				  { 99, 1, 1, 0, 1, 1, 1, 1, 1, 1 }, 
				  { 99, 1, 1, 1, 0, 1, 1, 1, 1, 1 }, 
				  { 99, 1, 1, 1, 1, 0, 1, 1, 1, 1 }, 
				  { 99, 1, 1, 1, 1, 1, 0, 1, 1, 1 }, 
				  { 99, 1, 1, 1, 1, 1, 1, 0, 1, 1 },
				  { 99, 1, 1, 1, 1, 1, 1, 1, 0, 1 }, 
				  { 99, 1, 1, 1, 1, 1, 1, 1, 1, 0 } };
	//*/

	int m[][] = { { 0, 2, 4, 99 }, 
				  { 2, 0, 3, 5 }, 
				  { 4, 1, 0, 2 }, 
				  { 99, 5, 2, 0 } };

	private int getBlockOwner(int j, int p, int n) {
		return ((p * (j + 1) - 1) / n);
	}

	private int getBlockLow(int id, int p, int n) {
		// starting line of block
		return (id * n / p);
	}

	private int getBlockHigh(int id, int p, int n) {
		// ending line of block
		return (getBlockLow(id + 1, p, n) - 1);
	}

	private int getBlockSize(int id, int p, int n) {
		// number of lines in block
		return (getBlockHigh(id, p, n) - getBlockLow(id, p, n) + 1);
	}

	public void printMatrix(String title, int[][] m) {
		getMPIDebug().println("--" + title + "--");
		StringBuffer row = null;
		for (int i = 0; i < m.length; i++) {
			row = new StringBuffer();
			for (int j = 0; j < 4; j++) {
				row.append(m[i][j] + " ");
			}
			row.append("\n");
			getMPIDebug().println(row.toString());
		}
		getMPIDebug().println("--");
	}

	/* --------------------- send ------------------------ */
	public int[][] distributeMatrix(int id, int p, int[][] fullmatrix) {
		int n = fullmatrix.length; /* Columns in matrix */
		int bs = getBlockSize(id, p, n); // local block size;
		int[][] a = new int[n][n]; // local block to compute with
		int[] sendBuf = new int[n * n];
		int[] recvBuf = new int[bs * n];
		int root = 0; // chosen root process

		// flatten matrix
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++)
				sendBuf[i * n + j] = fullmatrix[i][j];

		MPI().COMM_WORLD().Scatter(sendBuf, // sendBuffer
				getBlockLow(id, p, n) * n, // sendOffset
				bs * n, // sendCount
				MPI.INT, // sendType
				recvBuf, // recvBuffer
				0, // recvOffset
				bs * n, // recvCount
				MPI.INT, // recvType
				root); // root
		for (int i = 0; i < bs; i++)
			for (int j = 0; j < n; j++)
				a[getBlockLow(id, p, n) + i][j] = recvBuf[i * n + j];
		return (a);
	}

	/* --------------------- local computations ------------------------ */
	public void computeShortestPaths(int id, int p, int[][] a) {
		int n = a.length; /* Columns in matrix */
		int i, j, k;
		int offset; /* Local index of broadcast row */
		int root; /* Process controlling row to be bcast */
		int[] tmp; /* Holds the broadcast row */

		getMPIDebug().println("Starting to compute all pairs shortest paths for " + n + " vertices ...");
		tmp = new int[n];
		for (k = 0; k < n; k++) {
			root = getBlockOwner(k, p, n);
			if (root == id) {
				// offset = k - getBlockLow(id,p,n);
				offset = k;
				getMPIDebug().println("k=" + k + " low=" + getBlockLow(id, p, n));
				getMPIDebug().println("[rank " + id + "] k=" + k + " root=" + root + ",offset=" + offset);
				for (j = 0; j < n; j++)
					tmp[j] = a[offset][j];
				getMPIDebug().println("tmp <- a [" + offset + "][0.." + ((int) n - 1) + "]");
			}
			getMPIDebug().println("Broadcast tmp");
			MPI().COMM_WORLD().Bcast(tmp, 0, n, MPI.INT, root);
			for (i = getBlockLow(id, p, n); i <= getBlockHigh(id, p, n); i++)
				// for (i = 0; i <= getBlockSize(id,p,n); i++)
				for (j = 0; j < n; j++) {
					getMPIDebug().println("(i=" + i + ",j=" + j + ",k=" + k + " : a[i][j]=" + a[i][j] + " vs. a[i][k]=" + a[i][k] + ", tmp[" + j + "]=" + tmp[j]);
					if (a[i][k] + tmp[j] < a[i][j]) {
						a[i][j] = a[i][k] + tmp[j];
						getMPIDebug().println("update! a[i][j] <- " + ((int) a[i][k] + (int) tmp[j]));
					}
				}
		}
		printMatrix("Final a", a);

	}

	/* --------------------- initiate MPI program ------------------------ */
	public void runTask() {
		int id; /* Process rank */
		int p; /* Number of processes */
		int a[][]; /* matrix block */

		MPI().Init(this,3,2);
		p = MPI().COMM_WORLD().Size();
		id = MPI().COMM_WORLD().Rank();

		printMatrix("original m", m);

		double startTime = MPI().Wtime();

		a = distributeMatrix(id, p, m);
		printMatrix("partial m=a", a);

		computeShortestPaths(id, p, a);
		double stopTime = MPI().Wtime();

		getMPIDebug().println("Time usage = " + (stopTime - startTime) + " ms");
		MPI().Finalize();
	}
}
