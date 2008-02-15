package ee.ut.f2f.core;


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
