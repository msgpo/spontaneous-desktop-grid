package ee.ut.f2f.mpi.examples.fileTransfer;

import java.io.BufferedReader;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class RemoteDataTest extends MPITask {

	public void runTask() {
		getMPIDebug().setDebugLevel(0);
		int rank;

		MPI().Init();

		rank = MPI().COMM_WORLD().Rank();

		String myfilename = new String("ee/ut/f2f/mpi/examples/fileTransfer/sample.dat");
		String file = "";
		try {
			BufferedReader in = getFailFromClasspath(myfilename);
			while (in.ready()) {
				file = file + in.readLine();
			}
			getMPIDebug().println(file);
			in.close();

		} catch (Exception e) {
			getMPIDebug().println("Viga faili leidmisel");
			getMPIDebug().println("Error " + e.getMessage());
		}
		int len = file.length();
		getMPIDebug().println("Pikkus :" + len);
		int vlen[] = new int[1];
		// ----- rank 0 broadcasts the file length to all other processes
		if (rank == 0)
			vlen[0] = len;

		MPI().COMM_WORLD().Bcast(vlen, 0, 1, MPI.INT, 0);

		// ----- all processes except 0 compare the length received with
		// ----- the length of the file received file

		if (rank > 0) {
			if (len != vlen[0]) {
				getMPIDebug().println("[Error] received length (" + vlen[0] + ") differ from length of fuile transfered (" + len + ")");
				MPI().Finalize();
				return;
			}
		}

		// --- now, compares contents

		char rfile[] = new char[len]; // received file through MPI.Bcast

		if (rank == 0) { // -- rank 0 send its file contents
			rfile = file.toCharArray();
		}

		MPI().COMM_WORLD().Bcast(rfile, 0, len, MPI.CHAR, 0);

		if (rank > 0) { // -- other ranks compare message received with file received (stored on disk)
			String rfile_ = new String(rfile);
			getMPIDebug().println(" disk :" + file);
			getMPIDebug().println("BCAST :" + rfile_);

			if (!(file.equals(rfile_))) {
				getMPIDebug().println("[Error] read on disk and received BCAST are different");
			} else {
				getMPIDebug().println("[Succes] File transfer ok.");
			}
		}
		MPI().Finalize();
	}
}
