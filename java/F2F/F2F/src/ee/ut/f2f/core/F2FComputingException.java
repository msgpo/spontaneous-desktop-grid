package ee.ut.f2f.core;

@SuppressWarnings("serial")
public class F2FComputingException extends Exception
{
	public F2FComputingException()
	{
		super();
	}

	public F2FComputingException(Exception e)
	{
		super(e);
	}

	public F2FComputingException(String message)
	{
		super(message);
	}
	
	public F2FComputingException(String message, Exception e)
	{
		super(message, e);
	}
}
