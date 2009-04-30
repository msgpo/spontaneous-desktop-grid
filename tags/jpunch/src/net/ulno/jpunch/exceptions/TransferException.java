package net.ulno.jpunch.exceptions;

/**
 * 
 * @author artjom85
 *
 */
public class TransferException extends Exception {
	private static final long serialVersionUID = -8371831623367892322L;

	public static enum State {
		FAILED,
		ABORTED
	}
	
	private State state;
	
	public TransferException(State state) {
		super();
		this.state = state;
	}

	public TransferException(String message, State state, Throwable cause) {
		super(message, cause);
		this.state = state;
	}

	public TransferException(String message, State state) {
		super(message);
		this.state = state;
	}

	public TransferException(State state, Throwable cause) {
		super(cause);
		this.state = state;
	}
	
	public State getState(){
		return this.state;
	}
}
