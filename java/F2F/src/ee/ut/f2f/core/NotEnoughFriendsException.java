package ee.ut.f2f.core;

/**
 * This exception is thrown if a master task wants to submit some tasks
 * but not enough peers/friends have allowed to use their CPU.
 */
@SuppressWarnings("serial")
public class NotEnoughFriendsException extends F2FComputingException
{
	NotEnoughFriendsException(int goal, int result)
	{
		super(result+" friend(s) allowed to start a task but needed "+goal+"!");
	}
}
