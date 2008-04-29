package ee.ut.f2f.mpi.examples.mpiCommTest;

import java.util.Date;

import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Allgather;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Allgatherv;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Allreduce;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Allreduce_maxminloc;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Alltoall;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Alltoallv;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Barrier;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Bcast;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Gather;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Gatherv;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Reduce;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Reduce_scatter;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Scan;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Scatter;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.Scatterv;
import ee.ut.f2f.mpi.examples.mpiCommTest.individual.SendRecv;

public class CommTestIndividual extends MPITask {

	public void runTask() {
		MPI().Init();
		getMPIDebug().setDebugLevel(0);
		getMPIDebug().println((new Date()) + " ************************************** Starting Allgather");
		(new Allgather(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Allgatherv");
		(new Allgatherv(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Allreduce_maxminloc");
		(new Allreduce_maxminloc(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Allreduce");
		(new Allreduce(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Alltoall");
		(new Alltoall(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Alltoallv");
		(new Alltoallv(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Barrier");
		(new Barrier(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Bcast");
		(new Bcast(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Gather");
		(new Gather(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Gatherv");
		(new Gatherv(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Reduce_scatter");
		(new Reduce_scatter(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Reduce");
		(new Reduce(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Scan");
		(new Scan(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Scatter");
		(new Scatter(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting Scatterv");
		(new Scatterv(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** Starting SendRecv");
		(new SendRecv(this)).taskBody();
		getMPIDebug().println((new Date()) + " ************************************** All done");
		MPI().Finalize();
	}
}
