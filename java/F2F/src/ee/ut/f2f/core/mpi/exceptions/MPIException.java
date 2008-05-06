package ee.ut.f2f.core.mpi.exceptions;

public class MPIException extends RuntimeException {
	private static final long serialVersionUID = 4000000L;

	public MPIException() {
		super();
	}

	public MPIException(String message) {
		super(message);
	}
}
