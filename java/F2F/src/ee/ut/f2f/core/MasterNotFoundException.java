package ee.ut.f2f.core;


@SuppressWarnings("serial")
public class MasterNotFoundException extends CommunicationFailedException
{
	MasterNotFoundException(Task task)
	{
		super("Task "+task.getId()+" of job "+task.getJob()+" can not communicate with master task!");
	}
}
