package ee.ut.f2f.core.mpi.exceptions;

/**
 * This exception is thrown when MPI.Init is already called and someone wants to call it again.
 * 
 * @author Andres Luuk
 */
public class MPIAlreadyInitializedException extends RuntimeException {
	private static final long serialVersionUID = 4000000L;

	public MPIAlreadyInitializedException() {
		super();
	}

	public MPIAlreadyInitializedException(String message) {
		super(message);
	}
}
