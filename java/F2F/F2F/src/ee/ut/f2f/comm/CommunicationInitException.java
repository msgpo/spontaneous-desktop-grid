package ee.ut.f2f.comm;

@SuppressWarnings("serial")
public class CommunicationInitException extends CommunicationException
{
	public CommunicationInitException()
	{
		super();
	}
	
	public CommunicationInitException(Exception e)
	{
		super(e);
	}

	public CommunicationInitException(String msg)
	{
		super(msg);
	}

	public CommunicationInitException(String msg, Exception e)
	{
		super(msg, e);
	}
}