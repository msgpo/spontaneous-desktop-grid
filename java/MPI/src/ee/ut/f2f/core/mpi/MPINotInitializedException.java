package ee.ut.f2f.core.mpi;

/**
 * This exception is thrown when we MPI.Init is not called or something is called after MPI.Finalize.
 * 
 * @author Andres Luuk
 */
public class MPINotInitializedException extends RuntimeException {
	private static final long serialVersionUID = 4000000L;

	public MPINotInitializedException() {
		super();
	}

	public MPINotInitializedException(String message) {
		super(message);
	}
}
