package ee.ut.f2f.comm;

@SuppressWarnings("serial")
public class CommunicationException extends Exception
{
	public CommunicationException()
	{
		super();
	}

	public CommunicationException(Exception e)
	{
		super(e);
	}

	public CommunicationException(String message)
	{
		super(message);
	}
	
	public CommunicationException(String message, Exception e)
	{
		super(message, e);
	}
}
