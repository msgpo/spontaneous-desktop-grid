package ee.ut.f2f.mpi.examples.mpiCommTest;

import java.util.Date;

import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.perfTest.AlltoAll;
import ee.ut.f2f.mpi.examples.mpiCommTest.perfTest.Bcast;
import ee.ut.f2f.mpi.examples.mpiCommTest.perfTest.MpiAnysourceTest;
import ee.ut.f2f.mpi.examples.mpiCommTest.perfTest.MpiCommTest;
import ee.ut.f2f.mpi.examples.mpiCommTest.perfTest.TestGather;
import ee.ut.f2f.mpi.examples.mpiCommTest.perfTest.TestReduceScatter;
import ee.ut.f2f.mpi.examples.mpiCommTest.perfTest.TestSendRecv;

public class CommTestPref extends MPITask {

	public void runTask() {
		MPI().Init();
		getMPIDebug().setDebugLevel(0);
		getMPIDebug().println((new Date()) + " ************************************** Starting AlltoAll");
		(new AlltoAll(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Bcast");
		(new Bcast(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting MpiAnysourceTest");
		(new MpiAnysourceTest(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting MpiCommTest");
		(new MpiCommTest(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting TestGather");
		(new TestGather(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting TestReduceScatter");
		(new TestReduceScatter(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting TestSendRecv");
		(new TestSendRecv(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** All done");
		MPI().Finalize();
	}
}
