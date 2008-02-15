package ee.ut.f2f.core;

/**
 * This exception is thrown if communication between peers (or tasks) failes.
 * This means a remote peer (and tasks in it) is unreachable and messages
 * can not be sent to this peer.
 */
@SuppressWarnings("serial")
public class CommunicationFailedException extends F2FComputingException 
{
	public CommunicationFailedException(String msg)
	{
		super(msg);
	}

	public CommunicationFailedException(Exception e)
	{
		super(e);
	}
}
