package ee.ut.f2f.core;

/**
 * This exception is thrown if a slave task tries to submit some new tasks.
 */
@SuppressWarnings("serial")
public class NotMasterException extends F2FComputingException
{
	NotMasterException()
	{
		super("Tasks can only be submitted from master task!");
	}
}
