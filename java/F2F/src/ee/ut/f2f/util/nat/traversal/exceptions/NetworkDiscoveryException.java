package ee.ut.f2f.util.nat.traversal.exceptions;

public class NetworkDiscoveryException extends Exception {

	private static final long serialVersionUID = 4374744310005272013L;

	public NetworkDiscoveryException() {
	}

	public NetworkDiscoveryException(String message) {
		super(message);
	}

	public NetworkDiscoveryException(Throwable cause) {
		super(cause);
	}

	public NetworkDiscoveryException(String message, Throwable cause) {
		super(message, cause);
	}

}
