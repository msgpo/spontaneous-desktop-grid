package ee.ut.f2f.core;

public interface TaskListener
{
	void taskStarted(Task task);
	void taskStopped(Task task);
}
