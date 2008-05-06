package ee.ut.f2f.core.mpi.exceptions;
/**
 * This exception is thrown when we have lost a peer 
 * so that we can't complete the job.
 * (Lost the master or all peers in one peer group).
 * 
 * @author Andres Luuk
 */
public class MPITerminateException extends RuntimeException {
	private static final long serialVersionUID = 4000000L;

	public MPITerminateException() {
		super();
	}

	public MPITerminateException(String message) {
		super(message);
	}
}
