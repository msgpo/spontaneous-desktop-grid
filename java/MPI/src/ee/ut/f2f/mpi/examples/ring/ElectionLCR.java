/**
 * Election algorithm on a ring "LCR" after the names of Le Lann,Chang, and Roberts.
 *
 * The Communication is unidirectional.  The size of the ring is not known.
 * Only the leader have to perform output as leader. 
 * Uses the comparison on UIDs of every node.  
 * Process with the largest UID output as leader.
  
 * Each process sends its identifier around the ring, when a process receives 
 * an incoming identifier, it compare that identifier to its own.
 * If the incoming identifier is greater then its own, it keeps passing the identifier.
 * If it is less than its own, it discard it and 
 * if it is equal to its own the process declare itself as leader.
 *
 * The leader declares itself leader after n rounds (n = size of the ring)
 **/
package ee.ut.f2f.mpi.examples.ring;

import java.util.Random;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.Request;
import ee.ut.f2f.core.mpi.Status;

public class ElectionLCR extends MPITask {
	public void runTask() {
		int rank, size, nbround;
		int TAGUID = 1;
		int TAGSTATE = 2;

		MPI().Init();
		double startTime;
		Request req = null;
		Status status = null;

		size = MPI().COMM_WORLD().Size();
		rank = MPI().COMM_WORLD().Rank();

		int mynum;
		int[] r = new int[1];
		int[] s = new int[1];
		int e;
		getMPIDebug().println("rank=" + rank + " size=" + size);
		for (e = 0; e < 10; e++) {
			getMPIDebug().println("\nElection number : " + e);
			getMPIDebug().println("=======================================================");
			// generates an uid (assumes it is unique)
			mynum = (new Random()).nextInt(1000);
			s[0] = mynum;
			getMPIDebug().println("[rank " + rank + "] generates uid=" + mynum + ". Let us start.");
			nbround = 1;
			startTime = MPI().Wtime();
			MPI().COMM_WORLD().Send(s, 0, 1, MPI.INT, (rank + 1) % size, TAGUID);

			// loop receiving message from left neighbourg on ring,
			while (true) {
				req = MPI().COMM_WORLD().Irecv(r, 0, 1, MPI.INT, (rank == 0 ? size - 1 : rank - 1), MPI.ANY_TAG);
				status = req.Wait();
				// ----- Election phase -------
				if (status.tag == TAGUID) {
					if (r[0] > s[0]) {
						MPI().COMM_WORLD().Send(r, 0, 1, MPI.INT, (rank + 1) % size, TAGUID);
					} else {
						if (r[0] == s[0]) {
							getMPIDebug().println("[rank " + rank + "] After " + nbround + " rounds, I know I am the (unique) leader with " + s[0]);
							// I am the unique leader: initiate now another round to broadcast a halting state
							MPI().COMM_WORLD().Send(r, 0, 1, MPI.INT, (rank + 1) % size, TAGSTATE);
							// ok, the message will eventually come back. Consumes the mesage and Stop after this.
							MPI().COMM_WORLD().Recv(r, 0, 1, MPI.INT, (rank == 0 ? size - 1 : rank - 1), TAGSTATE);
							break;
						}
						// else ( r < s ) do nothing
					}
				}
				// ---- Halting phase -------
				if (status.tag == TAGSTATE) {
					getMPIDebug().println("[rank " + rank + "] i just get informed " + r[0] + " is elected.");
					MPI().COMM_WORLD().Send(r, 0, 1, MPI.INT, (rank + 1) % size, TAGSTATE);
					break;
				}
				nbround++;
			}
			double stopTime = MPI().Wtime();
			getMPIDebug().println("Time usage = " + (stopTime - startTime) + " ms");
			getMPIDebug().println("Number of iterations: " + nbround);
		}
		MPI().Finalize();
	}
}
