package ee.ut.f2f.core.mpi;

/**
 * No collective operation
 */
public class OpNull extends MPI_User_function {
	// Implement Call method for MAX operation
	public void Call(Object invec, int inOffset, Object inoutvec, int inoutOffset, int count, Datatype type) {
		// Do nothing :P
	}
}
