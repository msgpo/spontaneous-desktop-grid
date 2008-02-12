package ee.ut.f2f.core;

import java.io.Serializable;
import java.util.UUID;

/**
 * Description of a task that specifies identifier of the task, 
 * a class that has to be executed and 
 * a peer where it has to be executed. 
 */
public class TaskDescription implements Serializable
{
	private static final long serialVersionUID = 6393763867640026109L;

	/**
	 * The unique ID of a task in a job.
	 */
	private String taskID;
	public String getTaskID() { return taskID; }
	
	/**
	 * Specifies the ID of a peer where the described class should be executed  
	 */
	private UUID peerID;
	public UUID getPeerID() { return peerID; }
	
	/**
	 * The unique ID of the corresponding job.
	 */
	private String jobID;
	public String getJobID() { return jobID; }
	
	/**
	 * The name of the executable class that contains algorithm of described task.
	 */
	private String className;
	String getClassName() { return className; }

	TaskDescription(String jobID, String taskID, 
			UUID peerID, String className) 
	{
		this.taskID = taskID;
		this.peerID = peerID;
		this.jobID = jobID;
		this.className = className;
	}
	
	public String toString()
	{
		return
		"TaskDescription:" +
		"[taskID="+taskID+
		"][peerID="+peerID+
		"][jobID="+jobID+
		"][className="+className+
		"]";
	}
}
