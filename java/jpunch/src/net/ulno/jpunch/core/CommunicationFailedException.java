package net.ulno.jpunch.core;

/**
 * This exception is thrown if communication between peers (or tasks) failes.
 * This means a remote peer (and tasks in it) is unreachable and messages
 * can not be sent to this peer.
 */
@SuppressWarnings("serial")
public class CommunicationFailedException extends Exception 
{
	public CommunicationFailedException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CommunicationFailedException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public CommunicationFailedException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public CommunicationFailedException(String msg)
	{
		super(msg);
	}

	public CommunicationFailedException(Exception e)
	{
		super(e);
	}
}
