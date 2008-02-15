package ee.ut.f2f.core;

public class NotSerializableTaskException extends F2FComputingException
{
	private static final long serialVersionUID = 2949271342617810984L;

	NotSerializableTaskException(Task task)
	{
		super("Task "+task.getClass()+" is not Serializable!");
	}
}
