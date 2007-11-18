package ee.ut.f2f.util.nat.traversal.exceptions;



public class NetworkInterfaceNotFoundException extends ConnectionManagerException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 420839583709506210L;

	/**
	 * 
	 */
	public NetworkInterfaceNotFoundException() {
		super("Could not get Network Interface");
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public NetworkInterfaceNotFoundException(Throwable cause) {
		super("Could not get Network Interface",cause);
	}


}
