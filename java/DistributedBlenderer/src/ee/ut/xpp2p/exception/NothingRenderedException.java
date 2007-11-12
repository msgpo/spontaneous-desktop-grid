package ee.ut.xpp2p.exception;

/**
 * Exception that gets thrown when rendering fails
 * 
 * @author Jaan Neljandik
 * @created 20.10.2007
 */
public class NothingRenderedException extends Exception {

	private static final long serialVersionUID = 1L;

	public NothingRenderedException(String message) {
		super(message);
	}
}
