package ee.ut.f2f.mpi.examples.mpiCommTest.perfTest;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.Request;
import ee.ut.f2f.core.mpi.Status;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class MpiAnysourceTest extends CommTestMain {

	public MpiAnysourceTest(MPITask task) {
		super(task);
	}

	public static int ROOT = 0;
	public static int MTAG = 1;

	public void taskBody() {
		Status[] s = new Status[size];
		Request[] r = new Request[size];
		char[] tmp;
		char[] sendBuf = new char[10];
		char[] recvBuf = new char[10];
		int i;

		if (rank == 0) {
			for (i = 1; i < size; i++) {
				r[i] = MPI().COMM_WORLD().Irecv(recvBuf, 0, 10, MPI.CHAR, MPI.ANY_SOURCE, MTAG);
			}
			for (i = 1; i < size; i++) {
				getMPIDebug().println("Waiting for any message ...");
				s[i] = r[i].Wait();
				String stringMsg = new String(recvBuf);
				getMPIDebug().println("Received message '" + stringMsg + "' from " + s[i].source);
			}

		} else {
			String msg = "from" + rank;
			tmp = msg.toCharArray();
			for (i = 0; i < tmp.length; i++) {
				sendBuf[i] = tmp[i];
			}
			sendBuf[i] = '\0';
			String stringMsg = new String(sendBuf);
			getMPIDebug().println("[" + stringMsg + "] " + sendBuf.length);
			if (rank == 1) {
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
				}
			}
			MPI().COMM_WORLD().Send(sendBuf, 0, 10, MPI.CHAR, ROOT, MTAG);
		}
	}
}
