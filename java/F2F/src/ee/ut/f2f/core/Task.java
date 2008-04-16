package ee.ut.f2f.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.util.logging.Logger;

/**
 * This is the base class for all task that can be run in F2FComputing framework.
 * By extending this class and implementing runTask() method 
 * one can make distributed application and execute it in F2FComputing framework.
 * Task is a thread that corresponds to a TaskDescription. 
 * Task provides proxies of other tasks of a job to communicate with.
 */
public abstract class Task extends Thread implements Activity
{
	private final static Logger logger = Logger.getLogger(Task.class);
	/**
	 * Returns the unique ID of the task.
	 * Each task has an unique ID in a job. The master task has ID "0", and all the 
	 * slave tasks have IDs from "1" to "N", where N is the number of slave tasks.
	 */
	public String getTaskID() { return taskDescription.getTaskID(); }

	/**
	 * The description of task.
	 */
	private TaskDescription taskDescription;
	void setTaskDescription(TaskDescription taskDescription) { this.taskDescription = taskDescription; } 
		
	/**
	 * Map taskID->taskProxy, holds proxies to other tasks of a job.
	 */
	private Map<String, TaskProxy> taskProxies = new HashMap<String, TaskProxy>();
	/**
	 * This method returns all proxies to remote tasks that have been created
	 * (that means the proxies have been asked for with the method getTaskProxy()).
	 * This information can be useful in GUI to show existing connections to remote peers
	 * and the size of message queues.
	 */
	public Collection<TaskProxy> getTaskProxies() { return taskProxies.values(); }

	/**
	 * Exception that was thrown during task's execution.
	 */
	private Exception exception = null;
	/**
	 * Returns the exception that caused the task to stop (was thrown during task's execution).
	 * Returns null if such exception did not happen.
	 */
	public Exception getException() { return exception; }
	
	/**
	 * This method tries to return a proxy to a remote task with ID taskID.
	 * If the proxy to the remote task is asked the first time, it is created and saved for later use.
	 * The framework checks if a task with the given ID exists and then creates the proxy to it.
	 *  
	 * @param taskID The ID of a task to communicate with (send/receive messages to/from the task). 
	 * @return TaskProxy that can be used to send/receive messages to/from a task.
	 */
	public TaskProxy getTaskProxy(String taskID)
	{
		TaskProxy proxy = taskProxies.get(taskID);
		
		if(proxy==null)
		{
			synchronized (taskProxies)
			{
				proxy = taskProxies.get(taskID);
				if (proxy==null)
				{
					Job job = getJob();
					if (job != null)
					{
						TaskDescription taskDescription = job.getTaskDescription(taskID);
						if (taskDescription != null)
						{
							proxy = new TaskProxy(this, taskDescription);
							taskProxies.put(taskID, proxy);
						}
					}
				}
			}
		}
		return proxy;
	}
	
	/**
	 * @return The job this task belongs to.
	 */
	public Job getJob() { return F2FComputing.getJob(taskDescription.getJobID()); }

	/**
	 * This method is executed by JVM when task is started by
	 * F2FComputing framework. It calls runTask() and tries to catch any exceptions.
	 * If it catches an exception it calls printStackTrace() and exits.
	 */
	public final void run()
	{
		ActivityManager manager = ActivityManager.getDefault();
		try
		{
			manager.emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
			synchronized (F2FComputing.taskListeners)
			{
				for (TaskListener listener: F2FComputing.taskListeners)
					listener.taskStarted(this);
			}
			runTask();
			manager.emitEvent(new ActivityEvent(this, ActivityEvent.Type.FINISHED));
		}
		catch (Exception e)
		{
			manager.emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED, "error: "+ e.getMessage()));
			exception = e;
			logger.error(this.taskDescription+" exited with error", e);
		}
		stopTask();
		synchronized (F2FComputing.taskListeners)
		{
			for (TaskListener listener: F2FComputing.taskListeners)
				listener.taskStopped(this);
		}
		// if this is the Master task, the Job has finished
		if (getTaskID().equals(getJob().getMasterTaskID()))
			manager.emitEvent(new ActivityEvent(getJob(), ActivityEvent.Type.FINISHED, "Job finished"));
	}

	/**
	 * An abstract method which realization should describe the steps 
	 * of a distributed algorithm. This is the place where one can put his/her
	 * code and let it be executed by F2FComputing framework!
	 */
	public abstract void runTask();
	
	/**
	 * This method is called if a message is received from a remote task
	 * with ID remoteTaskID. The message can be read from the proper task proxy.
	 * An implementation of the Task class that wants to be notified about 
	 * incoming messages should overwrite this method and take the actions it
	 * wants (probably read the message and process it).
	 */
	void messageReceivedEvent(String remoteTaskID)
	{
	}

	public String getActivityName()
	{
		return "Task " + taskDescription.getTaskID();
	}

	public Activity getParentActivity()
	{
		return getJob();
	}
	
	private boolean bStopFlag = false;
    /**
     * This method returns TRUE until the framework allows the task 
     * to run. If this method starts to return FALSE the task should 
     * exit the runTask() method and stop all the sub-threads it 
     * has started.
     */
	public boolean isStopped() { return bStopFlag; }
	/**
	 * Framework uses this method to stop the thread. 
	 */
	public void stopTask()
	{
		bStopFlag = true;
        new Thread ()
        {
            public void run()
            {
		        taskStoppedEvent();
            }
        }.start();
	}
	/**
	 * This method is called if the task shoult stop its execution.
	 * (exit the runTask method and stop all sub-threads).
	 * Overwrite this method, if you want to react quickly on the event
     * that asks the task to stop.
	 */
	protected void taskStoppedEvent()
	{
	}
	
	/**
	 * This method returns the progress of a task.
	 * Return value less than 0 means the progress is not determined
	 * (GUI shows indeterminate progress bar in this case).
	 * Return value [0..100] means the task is 0..100% completed. 
	 * Return value greater than 100 is considered equal to 100.
	 */
	private int progress = -1;
	public int getProgress()
	{
		return progress;
	}
	/**
	 * This metod sets the progress value of the task and
	 * informs the listeners about this event.
	 * @param progress Value less than 0 means the progress is not determined
	 * (the GUI does not show the progress bar in this case).
	 * Value [0..100] means the task is 0..100% completed. 
	 * Value greater than 100 is considered equal to 100.
	 */
	protected void setProgress(int progress)
	{
		this.progress = progress;
		synchronized (F2FComputing.taskListeners)
		{
			for (final TaskListener listener: F2FComputing.taskListeners)
            {
                new Thread() 
                {
                    public void run()
                    {
				        listener.taskProgressed(Task.this);
                    }
                }.start();
            }
		}
	}
}
