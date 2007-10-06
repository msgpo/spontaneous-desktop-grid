package ee.ut.f2f.core;

import java.io.Serializable;
import java.util.Map;

/**
 * Description of a task that specifies identifier of the task, 
 * a class that has to be executed and 
 * a peer where it has to be executed. 
 */
@SuppressWarnings("serial")
class TaskDescription implements Serializable
{
	/**
	 * The unique ID of a task in a job.
	 */
	private String taskID;
	String getTaskID() { return taskID; }
	
	/**
	 * Map CommLayerID->peerID, specifies the ID of a peer where 
	 * the described class should be executed in the context of one 
	 * or more communication layers.
	 * PS: right now, only a peer where master task is executed can be specified 
	 * in the context of multiple communication layers.  
	 */
	Map<String, String> mapComm2Peer;
	
	/**
	 * The unique ID of the corresponding job.
	 */
	private String jobID;
	String getJobID() { return jobID; }
	
	/**
	 * The name of the executable class that contains algorithm of described task.
	 */
	String className;

	TaskDescription(String jobID, String taskID, 
			Map<String, String> Comm2Peer, String className) 
	{
		this.taskID = taskID;
		this.mapComm2Peer = Comm2Peer;
		this.jobID = jobID;
		this.className = className;
	}
	
	public String toString()
	{
		return
		"TaskDescription:" +
		"[taskID="+taskID+
		"][peerIdmap="+mapComm2Peer+
		"][jobID="+jobID+
		"][className="+className+
		"]";
	}
}
