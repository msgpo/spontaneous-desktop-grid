package ee.ut.f2f.mpi.examples.mpiCommTest.individual;

/****************************************************************************

 MESSAGE PASSING INTERFACE TEST CASE SUITE

 Copyright IBM Corp. 1995

 IBM Corp. hereby grants a non-exclusive license to use, copy, modify, and
 distribute this software for any purpose and without fee provided that the
 above copyright notice and the following paragraphs appear in all copies.

 IBM Corp. makes no representation that the test cases comprising this
 suite are correct or are an accurate representation of any standard.

 In no event shall IBM be liable to any party for direct, indirect, special
 incidental, or consequential damage arising out of the use of this software
 even if IBM Corp. has been advised of the possibility of such damage.

 IBM CORP. SPECIFICALLY DISCLAIMS ANY WARRANTIES INCLUDING, BUT NOT LIMITED
 TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS AND IBM
 CORP. HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 ****************************************************************************

 These test cases reflect an interpretation of the MPI Standard.  They are
 are, in most cases, unit tests of specific MPI behaviors.  If a user of any
 test case from this set believes that the MPI Standard requires behavior
 different than that implied by the test case we would appreciate feedback.

 Comments may be sent to:
 Richard Treumann
 treumann@kgn.ibm.com

 ****************************************************************************

 MPI-Java version :
 Sung-Hoon Ko(shko@npac.syr.edu)
 Northeast Parallel Architectures Center at Syracuse University
 03/22/98

 ****************************************************************************/

import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.mpi.examples.mpiCommTest.CommTestMain;

public class Barrier extends CommTestMain {

	public Barrier(MPITask task) {
		super(task);
	}

	public void taskBody() {
		int i;

		if (size < 2) {
			getMPIDebug().println("MUST RUN WITH AT LEAST 2 TASKS");
			return;
		}

		for (i = 0; i < 250000 * rank; i++)
			;

		// getMPIDebug().println
		// (" TASK "+me+" BEFORE BARRIER, TIME = "+(MPI.Wtime()));

		MPI().COMM_WORLD().Barrier();

		// getMPIDebug().println
		// (" TASK "+rank+" AFTER BARRIER, TIME = "+(MPI.Wtime()));

		if (rank == 0)
			getMPIDebug().println("Barrier TEST COMPLETE\n");

	}
}
