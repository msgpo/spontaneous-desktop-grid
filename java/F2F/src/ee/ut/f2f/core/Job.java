package ee.ut.f2f.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;

/**
 * Job is an entity that represents an implementation of an 
 * algorithm (in jar file(s)) and descriptions of tasks that 
 * can be run in F2F framework to execute the algorithm.
 */
public class Job implements Serializable, Activity
{
	private static final long serialVersionUID = 2726160570171222435L;
	
	/**
	 * The identifier of the master task of the job.
	 */
	private String masterTaskId = null;
	public String getMasterTaskID() { return masterTaskId; }
	void setMasterTaskID(String id) { this.masterTaskId = id; }
	
	/**
	 * The identifier of the job.
	 */
	private String jobID = null;
	public String getJobID() { return jobID; }
	
	/**
	 * Map TaskID->TaksDescription, holds TaskDescriptions this job is aware of.
	 */
	private Map<String, TaskDescription> taskDescriptions = new HashMap<String, TaskDescription>();
	void addTaskDescriptions(Collection<TaskDescription> tasks)
	{
		for(TaskDescription task: tasks)
			taskDescriptions.put(task.getTaskID(), task);
	}
	void addTaskDescription(TaskDescription task) { taskDescriptions.put(task.getTaskID(), task); }
	public TaskDescription getTaskDescription(String taskID) { return taskDescriptions.get(taskID); }
	public Collection<TaskDescription> getTaskDescriptions() { return taskDescriptions.values(); }
	public Collection<String> getTaskIDs() { return taskDescriptions.keySet(); }
	
	/**
	 * Location of jar and other files that are needed for this job.
	 */
	private transient java.io.File workingDirectory = null;
	
	/**
	 * Loader of custom classes in the job.
	 */
	private transient ClassLoader classLoader = null;
	ClassLoader getClassLoader() { return classLoader; }
	
	/**
	 * Map TaskID->Taks, contains tasks that are running in this node.
	 */
	private transient Map<String, Task> tasks = null;
	public Collection<Task> getTasks() { return tasks.values(); }
	/**
	 * Appends given task to this job.
	 */
	void addTask(Task task) { tasks.put(task.getTaskID(), task); }
	/**
	 * Returns a task with given ID or NULL if such task is not found in this job.
	 */
	Task getTask(String taskID) { return tasks.get(taskID); }

	/**
	 * Collection of peers that have been selected for this job during job creation. 
	 * This list is not NULL only for master node.
	 */
	private transient Collection<F2FPeer> peers = null;
	public Collection<F2FPeer> getPeers() { return peers; }
	
	private transient Collection<F2FPeer> workingPeers = null;
	Collection<F2FPeer> getWorkingPeers() { return workingPeers; }
	void addWorkingPeer(F2FPeer peer)
	{ 
		if (workingPeers == null) workingPeers = new ArrayList<F2FPeer>();
		if (!workingPeers.contains(peer)) workingPeers.add(peer);
	}

	private transient CPURequests cpuRequests = null;
	CPURequests getCPURequests() { return cpuRequests; }
	
	/**
	 * The collection of jar files that hold custom classes of the job.
	 */
	private Collection<F2FJarFile> jarFiles = null;
	
	/**
	 * @param rootDirectory parent directory of job working directory
	 * @param jobID
	 * @throws IOException 
	 * @throws F2FComputingException 
	 */
	Job(java.io.File rootDirectory, String jobID, Collection<String> jarFilesNames, Collection<F2FPeer> peers) throws IOException
	{
		this.jobID = jobID;
		this.jarFiles = new ArrayList<F2FJarFile>();
		if (jarFilesNames != null)
			for (String fileName: jarFilesNames)
				jarFiles.add(new F2FJarFile(fileName.trim()));
		this.peers = peers;
		initialize(rootDirectory);
		ActivityManager.getDefault().emitEvent(
				new ActivityEvent(this,
						ActivityEvent.Type.STARTED, "Job created"));

		cpuRequests = new CPURequests(this, peers);
		cpuRequests.start();
	}

	/**
	 * Initialization of a job. 
	 * This includes initialization of jar files the job includes and 
	 * initialization of a class loader that can load classes specific 
	 * to the job.
	 * @param rootDirectory
	 * @throws IOException 
	 * @throws F2FComputingException 
	 */
	void initialize(java.io.File rootDirectory) throws IOException
	{
		workingDirectory = new java.io.File(rootDirectory, this.jobID);
		workingDirectory.mkdir();
		tasks = new HashMap<String, Task>();
		// initialize jar files the job includes
		for(F2FJarFile jar :jarFiles)
		{
			FileOutputStream fileStream = null;
			try
			{
				java.io.File jarFile = new java.io.File(workingDirectory, jar.getName());
				// if according jar file exists do nothing ...
				if (jarFile.exists()) continue;
				// ... else create this file
				fileStream = new FileOutputStream(jarFile);
				fileStream.write(jar.getData());
			}
			finally
			{
				IOUtils.closeQuietly(fileStream);				
			}
		}
		// initialize class loader of this job
		Collection<URL> urls = new ArrayList<URL>();
		for(F2FJarFile file: jarFiles)
		{
			urls.add(new java.io.File(workingDirectory, file.getName()).toURI().toURL());
		}
		classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), Job.class.getClassLoader());
	}

	/**
	 * Generate an ID for a task. 
	 * IDs are generated in sequence 0,1,2,... 
	 * Master task gets ID 0, and following N tasks get IDs from 1 to N.
	 */
	String newTaskId() { return ""+(lastTaskId++); }
	private int lastTaskId = 0;
	
	/**
	 * This method of a job should be used to create and 
	 * add new tasks of type className to the job. 
	 * The method waits until taskCount peers of given peers have allowed 
	 * to use their PC (request was sent during job creation),
	 * then new tasks are added to the job and executed in corresponding nodes.
	 * 
	 * @param className The name of the class that should be executed as new task.
	 * @param taskCount The number of tasks to make. If it is less than 1
	 *  the method throws RuntimeError.
	 * @param peers The collection of peers to where new tasks should be sent.
	 * 	This collection should hold at least taskCount peers, otherwise the method
	 *  throws RuntimeError. 
	 */
	public void submitTasks(String className, int taskCount, Collection<F2FPeer> peers) throws F2FComputingException, ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		if (getTask(getMasterTaskID()) == null)
			throw new NotMasterException();
		if (peers == null || peers.size() < taskCount)
			throw new NotEnoughPeersException(taskCount, peers == null ? 0: peers.size());
		for (F2FPeer peer: peers)
			if (!getPeers().contains(peer))
				throw new NotJobPeerException(peer, this);
		F2FComputing.submitTasks(this, className, taskCount, peers);
	}
	
	private class F2FJarFile implements Serializable
	{
		private static final long serialVersionUID = -6267263735468032833L;
		/**
		 * The name of the file.
		 */
		private String name;
		String getName() { return name; }
		
		/**
		 * The data in the file.
		 */
		private byte[] data;
		byte[] getData() { return data; }
		
		/**
		 * @param absName Absolute name of a file.
		 * @throws F2FComputingException 
		 */
		F2FJarFile(String absName) throws IOException
		{
			this.name = new java.io.File(absName).getName();
			this.data = IOUtils.toByteArray(new FileInputStream(absName));
		}
	}
	
	public String getActivityName() {
		return jobID;
	}
	public Activity getParentActivity() {
		return null;
	}
	
	public String toString()
	{
		return jobID;
	}
}