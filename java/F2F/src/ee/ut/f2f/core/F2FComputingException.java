package ee.ut.f2f.core;

/**
 * Superclass for exceptions in F2F framework
 */
@SuppressWarnings("serial")
public class F2FComputingException extends Exception
{
	protected F2FComputingException(String message)
	{
		super(message);
	}
	
	protected F2FComputingException(Exception e)
	{
		super(e);
	}
}
