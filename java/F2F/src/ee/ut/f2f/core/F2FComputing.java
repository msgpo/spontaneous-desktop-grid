package ee.ut.f2f.core;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import ee.ut.f2f.comm.CommunicationFactory;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.CustomObjectInputStream;
import ee.ut.f2f.util.F2FDebug;

/**
 * This is the core class of F2F framework. It provides methods to create new
 * jobs and tasks.
 */
public class F2FComputing
{
	// private static final Logger LOG = LogManager.getLogger(F2FComputing.class);
	/**
	 * The time how long to wait for the answers of REQUEST_FOR_CPU before
	 * throwing error that there are not enough CPUs
	 */
	private static final long REQUEST_FOR_CPUS_TIMEOUT = 10000;

	/**
	 * Collection of all communication layer instances that are initialized and
	 * can be used for communication with other nodes.
	 */
	private static Collection<CommunicationLayer> communicationLayers = null;
	static Collection<CommunicationLayer> getCommunicationLayers() { return communicationLayers; }

	/**
	 * Map jobID->Job, contains all the jobs that the F2F framework is aware
	 * at. New jobs can be added by user by GUI or received from other nodes.
	 */
	private static Map<String, Job> jobs = new HashMap<String, Job>();
	public static Collection<Job> getJobs() { return jobs.values(); }
	static Job getJob(String jobID) { return jobs.get(jobID); }
	
	/**
	 * The parent directory for all job directories.
	 */
	private static java.io.File rootDirectory;

	/**
	 * Peers who allow their CPU to be used for F2F.
	 */
	private static Map<String, Collection<Peer>> reservedPeers = new HashMap<String, Collection<Peer>>();

	/**
	 * Private constructor for singleton implementation.
	 * 
	 * @throws CommunicationInitException
	 */
	private F2FComputing(java.io.File rootDirectory)
	{
		workHandler = new WorkHandler();
		rootDirectory.mkdir();
		F2FComputing.rootDirectory = rootDirectory;
		communicationLayers = CommunicationFactory.getInitializedCommunicationLayers();
		for(CommunicationLayer communicationLayer :communicationLayers)
		{
			communicationLayer.addListener(workHandler);
		}
	}

	/**
	 * Initiates F2FComputing in rootDirectory which will be parent directory for all
	 * job directories.
	 * 
	 * @param rootDirectory Parent directory of all job directories
	 * @throws F2FComputingException 
	 * @throws CommunicationInitException
	 */
	public static F2FComputing initiateF2FComputing(java.io.File rootDirectory) throws F2FComputingException
	{
		if (workHandler != null)
			throw new F2FComputingException("F2FComputing already intiated, initiateF2FComputing() was called more than once!");
		
		return new F2FComputing(rootDirectory);
	}

	/**
	 * Initiates F2FComputing in ./__F2F_ROOTDIRECTORY which will be parent directory for all
	 * job directories.
	 * 
	 * @throws CommunicationInitException
	 * @throws F2FComputingException 
	 */
	public static F2FComputing initiateF2FComputing() throws F2FComputingException
	{
		String tempName = "__F2F_ROOTDIRECTORY";
		tempName = tempName.replaceAll("\\W", "_");
		return initiateF2FComputing(new java.io.File(tempName));
	}

	/**
	 * Creates new job and executes its master task. This method is ment to be
	 * used from GUI (for example, an user selects jar files that contain needed
	 * algorithm and selects peers whose computational power he/she wants
	 * to use, specifies the name of master task class and initiates start of
	 * the job by pressing "Start" button).
	 * 
	 * @param jarFiles Jar files that contain an algorithm that has to be executed.
	 * @param masterTaskClassName The name of class that contains the implementation of master task.
	 * @param peers The peers that have been selected to be 'slaves' of the algorithm.
	 * @throws F2FComputingException 
	 */
	public static Job createJob(Collection<String> jarFilesNames, String masterTaskClassName, Collection<Peer> peers) throws F2FComputingException
	{
		// create a job
		String jobID = newJobID();
		Job job = new Job(rootDirectory, jobID, jarFilesNames, peers);
		F2FDebug.println("\tCreated new job with ID: " + jobID);
		// add job to jobs map
		jobs.put(jobID, job);
		// set the master task ID of the job
		String masterTaskID = job.newTaskId();
		job.setMasterTaskID(masterTaskID);
		// create the description of master task and add it to the job
		Map<String, String> localPeerIDmap = new HashMap<String, String>();
		for (CommunicationLayer commLayer: communicationLayers)
		{
			localPeerIDmap.put(commLayer.getID(), commLayer.getLocalPeer().getID());
		}
		TaskDescription masterTaskDescription = 
			new TaskDescription( 
					jobID, 
					masterTaskID, 
					//null, 
					localPeerIDmap, 
					masterTaskClassName
			);
		job.addTaskDescription(masterTaskDescription);
		// create a task based on master task description and execute it	
		try
		{
			Task task = newTask(masterTaskDescription);
			task.start();
		}
		catch (Exception e)
		{
			F2FDebug.println("\tError starting a master task: "+masterTaskDescription + e);
		}
		return job;
	}

	/**
	 * Generate unique ID for a job.
	 */
	private static String newJobID() { return "Job"+(++lastJobID); }
	private static int lastJobID = new Random(System.currentTimeMillis()).nextInt();
		
	/**
	 * This method is used to create new tasks.
	 * 
	 * The method asks computational power from given peers and waits
	 * until taskCount positive answers have been returned, then new tasks'
	 * descriptions are created and sent to corresponding peers.
	 * Also all ohter tasks of the job are informed of new tasks so that 
	 * they could communicate.
	 * 
	 * @param jobID The ID of the job to which new tasks should be added.
	 *  If the jobID is not valid the method throws RuntimeError.
	 * @param className The name of the class that should be executed as new task.
	 * @param taskCount The number of tasks to make. If it is less than 1
	 *  the method throws RuntimeError.
	 * @param peers The collection of peers to where new tasks should be sent.
	 * 	This collection should hold at least taskCount peers, otherwise the method
	 *  throws RuntimeError. 
	 * @return The job to which new tasks were added.
	 * @throws F2FComputingException 
	 */
	static void submitTasks(String jobID, String className,
			int taskCount, Collection<Peer> peers) throws F2FComputingException
	{
		F2FDebug.println("\tSubmitting " + taskCount + " tasks of " + className);
		if (taskCount <= 0)
			throw new F2FComputingException("Can not submit 0 tasks!");
		if (peers == null || peers.size() == 0)
			throw new F2FComputingException("Can not submit tasks if no peers are given!");
		if (peers.size() < taskCount)
			throw new F2FComputingException("Not enough peers selected!");
		Job job = jobs.get(jobID);
		if (job == null)
			throw new F2FComputingException("Can not submit tasks to unknown job ("+jobID+")!");
		ClassLoader loader = job.getClassLoader();
		try
		{
			Class clazz = loader.loadClass(className);
			@SuppressWarnings("unused")
			Task task = (Task) clazz.newInstance();
		}
		catch (Exception e)
		{
			throw new F2FComputingException("Could not initialize task " + className, e);
		}

		// ask peers for CPU power
		reservedPeers.put(jobID, new ArrayList<Peer>());
		F2FDebug.println("\tSending REQUEST_FOR_CPU to: " + peers + ".");
		F2FMessage message = 
			new F2FMessage(F2FMessage.Type.REQUEST_FOR_CPU, jobID, null, null, null);
		for (Peer peer : peers)
		{
			try {
				peer.sendMessage(message);
			} catch (Exception e) {
				F2FDebug.println("" + e);
			}
		}
		// wait for answers
		while (true)
		{
			if (reservedPeers.get(jobID).size() >= taskCount) break;
			try
			{
				synchronized(reservedPeers.get(jobID))
				{
					reservedPeers.get(jobID).wait(REQUEST_FOR_CPUS_TIMEOUT);
				}
			}
			catch (InterruptedException e)
			{// timeout
				reservedPeers.remove(jobID);
				throw new F2FComputingException("Not enough available CPUs!");
			}
		}
		
		// now we know in which peers new tasks should be started
		Peer[] peersToBeUsed = new Peer[taskCount];
		Iterator<Peer> reservedPeersIterator = reservedPeers.get(jobID).iterator();
		for (int i = 0; i < taskCount; i++) peersToBeUsed[i] = reservedPeersIterator.next();
		
		// create descriptions of new tasks and add them to the job
		Collection<TaskDescription> newTaskDescriptions = new ArrayList<TaskDescription>();
		for (Peer peer: peersToBeUsed)
		{
			Map<String, String> peerIDmap = new HashMap<String, String>();
			peerIDmap.put(peer.getCommunicationLayer().getID(),
					peer.getID());
			TaskDescription newTaskDescription = 
				new TaskDescription(
					jobID, job.newTaskId(),
					peerIDmap, className);
			job.addTaskDescription(newTaskDescription);
			newTaskDescriptions.add(newTaskDescription);
			F2FDebug.println("\tAdded new taskdescription to the job: "
					+ newTaskDescription);
		}
		
		// send Job to those peers who are new for this job and ...
		Collection<Peer> newPeers = new ArrayList<Peer>();
		for (Peer peer: peersToBeUsed)
		{
			if (job.getWorkingPeers() == null || !job.getWorkingPeers().contains(peer)) newPeers.add(peer);
		}
		job.addWorkingPeers(newPeers);
		F2FMessage messageJob = 
			new F2FMessage(F2FMessage.Type.JOB, null, null, null, job);
		for (Peer peer: newPeers)
		{
			try {
				peer.sendMessage(messageJob);
			} catch (CommunicationFailedException e) {
				F2FDebug.println("\tError sending the job to a peer. " + e);
			}
		}
		// ... notify other peers about additional tasks
		F2FMessage messageTasks = 
			new F2FMessage(F2FMessage.Type.TASKS, job.getJobID(), null, null, newTaskDescriptions);
		for (Peer peer: peersToBeUsed)
		{
			if (newPeers.contains(peer)) continue;
			try {
				peer.sendMessage(messageTasks);
			} catch (CommunicationFailedException e) {
				F2FDebug.println("\tError sending new tasks to a peer. " + e);
			}
		}
				
		reservedPeers.remove(jobID);
	}

	/**
	 * Creates new task based on given TaskDescription. The task is then ready
	 * to be executed/started. New task is created in two situations: a) user
	 * submits new job usin GUI, and F2F framework creates the master task of
	 * this job; b) F2F framework gets JOB message from an other node and
	 * finds out that this job contains task/tasks that has/have to be executed
	 * in this node.
	 * 
	 * @param taskDescription
	 *            The description of the task to create.
	 * @return Created task
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static Task newTask(TaskDescription taskDescription)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException
	{
		// this job must definetely exist!
		Job job = jobs.get(taskDescription.getJobID());
		ClassLoader loader = job.getClassLoader();
		F2FDebug.println("\tLoading class: " + taskDescription.className);
		Class clazz = loader.loadClass(taskDescription.className);
		Task task = (Task) clazz.newInstance();
		task.setTaskDescription(taskDescription);
		job.addTask(task);
		return task;
	}

	/**
	 * @param jobID
	 *            the id of the job whose loader is requested.
	 * @return the class loader of the specific job; <code>null</code> if
	 *         there is no loader for this job
	 */
	public static ClassLoader getJobClassLoader(String jobID) {
		// If there is no such job
		if (jobs == null || !jobs.containsKey(jobID))
			return null;
		// Return the classloader.
		return jobs.get(jobID).getClassLoader();
	}

	/**
	 * This method is mainly ment for GUI to show available peers.
	 * 
	 * @return All peers that are known through communication layers.
	 * @throws CommunicationFailedException
	 */
	public static Collection<Peer> getPeers()
			throws CommunicationFailedException
	{
		Collection<Peer> peers = new java.util.ArrayList<Peer>();
		for (CommunicationLayer commLayer : communicationLayers)
			peers.addAll(commLayer.getPeers());
		return peers;
	}

	/**
	 * Handles F2F framework messages and forwards messages sent between tasks.
	 */
	static WorkHandler workHandler = null;

	/**
	 * Handles F2F framework messages and forwards messages sent between tasks.
	 */
	class WorkHandler implements CommunicationListener
	{
		private void startJobTasks(Job job)
		{
			// find the tasks, that are not created yet and are
			// ment for execution in the peer, and start them
			for (CommunicationLayer communicationLayer : communicationLayers) {
				for (TaskDescription taskDesc : job.getTaskDescriptions()) {
					if (job.getTask(taskDesc.getTaskID()) == null &&
						taskDesc.mapComm2Peer.containsKey(communicationLayer.getID()) &&
						taskDesc.mapComm2Peer.get(communicationLayer.getID())
							.equals(communicationLayer.getLocalPeer().getID()))
					{
						try
						{
							F2FDebug.println(
									"\tStarting a task: "
									+ taskDesc);
							Task task = newTask(taskDesc);
							task.start();
						}
						catch (Exception e)
						{
							F2FDebug.println("\tError starting a task: "
									+ taskDesc
									+ e);
						}
					}
				}
			}
		}
		@SuppressWarnings("unchecked")
		public void messageRecieved(Object message, Peer fromPeer)
		{
			F2FMessage f2fMessage = null;
			try
			{
				f2fMessage = (F2FMessage) message;
			}
			catch (Exception e)
			{
				F2FDebug.println("\tError with casting message to F2FMessage");
				return;
			}
			if (f2fMessage.getType() == F2FMessage.Type.REQUEST_FOR_CPU)
			{
				try
				{
					F2FDebug.println("\tgot REQUEST_FOR_CPU. aswer it with RESPONSE_FOR_CPU");
					F2FMessage responseMessage = 
						new F2FMessage(
							F2FMessage.Type.RESPONSE_FOR_CPU, 
							f2fMessage.getJobID(), null, null,
							Boolean.TRUE);
					fromPeer.sendMessage(responseMessage);
				}
				catch (CommunicationFailedException e)
				{
					e.printStackTrace();
				}
			}
			else if (f2fMessage.getType() == F2FMessage.Type.RESPONSE_FOR_CPU)
			{
				if ((Boolean)f2fMessage.getData() &&
					reservedPeers.get(f2fMessage.getJobID()) != null)
				{
					synchronized(reservedPeers.get(f2fMessage.getJobID()))
					{
						reservedPeers.get(f2fMessage.getJobID()).add(fromPeer);
						reservedPeers.get(f2fMessage.getJobID()).notifyAll();
					}
				}
			}
			else if (f2fMessage.getType() == F2FMessage.Type.JOB)
			{
				F2FDebug.println("\tgot JOB");
				Job job = (Job) f2fMessage.getData();
				// check if we know this job already
				if (jobs.containsKey(job.getJobID()))
				{
					F2FDebug.println("\tERROR!!! Received a job that is already known!");
					return;
				}
				try
				{
					job.initialize(rootDirectory);
					jobs.put(job.getJobID(), job);
					startJobTasks(job);
				}
				catch (F2FComputingException e)
				{
					F2FDebug.println("\tERROR!!! " + e);
				}
			}
			else if (f2fMessage.getType() == F2FMessage.Type.TASKS)
			{
				F2FDebug.println("\tgot TASKS");
				Job job = getJob(f2fMessage.getJobID());
				if (job == null)
				{
					F2FDebug.println("\tERROR!!! Received tasks for unknown job");
					return;
				}
				Collection<TaskDescription> taskDescriptions = (Collection<TaskDescription>) f2fMessage.getData();
				job.addTaskDescriptions(taskDescriptions);
				startJobTasks(job);
			}
			else if (f2fMessage.getType() == F2FMessage.Type.MESSAGE)
			{
				F2FDebug.println("\tMESSAGE received " + f2fMessage);
				Task recepientTask = getJob(f2fMessage.getJobID()).getTask(
						f2fMessage.getReceiverTaskID());
				if (recepientTask == null)
				{
					F2FDebug.println("\tGot MESSAGE for unknown task wiht ID: "
							+ f2fMessage.getReceiverTaskID());
					return;
				}
				recepientTask.getTaskProxy(f2fMessage.getSenderTaskID())
						.saveMessage(f2fMessage.getData());
			}
			else if (f2fMessage.getType() == F2FMessage.Type.ROUTE)
			{
				F2FDebug.println("\tReceived ROUTE: " + f2fMessage);
				f2fMessage.setType(F2FMessage.Type.MESSAGE);
				Job job = getJob(f2fMessage.getJobID());
				if (job == null)
				{
					F2FDebug.println("\tERROR!!! didn't find the job");
					return;
				}
				TaskDescription receiverTaskDesc = job
						.getTaskDescription(f2fMessage.getReceiverTaskID());
				if (receiverTaskDesc == null)
				{
					F2FDebug.println("\tERROR!!! didn't find the receiver task description");
					return;
				}
				// try to send message the receiver
				for (CommunicationLayer commLayer : getCommunicationLayers())
				{
					if (receiverTaskDesc.mapComm2Peer.containsKey(commLayer
							.getID()))
					{
						try
						{
							Peer peer = commLayer
									.findPeerByID(receiverTaskDesc.mapComm2Peer
											.get(commLayer.getID()));
							if (peer == null)
								continue;
							peer.sendMessage(f2fMessage);
							return;
						}
						catch (CommunicationFailedException e)
						{
							F2FDebug.println("\tRouting message ("
									+ message
									+ ") to the peer ("
									+ receiverTaskDesc.mapComm2Peer
											.get(commLayer.getID())
									+ ") failed.");
							e.printStackTrace();
						}
					}
				}
				F2FDebug.println("\tERROR!!! master node: COULD NOT ROUTE MESSAGE TO RECEIVER NODE!!!");
			}
		}

		public void communicationOffline() {}

		public void communicationOnline() {}

		public void messageReceiveProgress(String id, int packetNumber,
				Peer fromPeer) {}

		public void messageReceiveStarted(String id, int packetCount,
				Peer fromPeer) {}

		public void peerOffline(Peer peer) {}

		public void peerOnline(Peer peer) {}
	}

	@SuppressWarnings("serial")
	static class F2FMessage implements Serializable
	{
		// private static final Logger LOG =
		// LogManager.getLogger(F2FMessage.class);

		/**
		 * Defines the nature of a message.
		 */
		enum Type
		{
			/**
			 * Master node asks slaves for cpu.
			 */
			REQUEST_FOR_CPU,
			/**
			 * Slave answers to REQUEST_FOR_CPU.
			 */
			RESPONSE_FOR_CPU,
			/**
			 * Master sends job to slave nodes.
			 */
			JOB,
			/**
			 * Master sends tasks to slave nodes.
			 */
			TASKS,
			/**
			 * A message from one task to another.
			 */
			MESSAGE,
			/**
			 * A message from one task to another that has to be routed. Only
			 * master tasks receive such messages and forward them to final
			 * destination nodes.
			 */
			ROUTE
		}

		F2FMessage(Type type, String jobID, String receiverTaskID,
				String senderTaskID, Object data)
		{
			this.type = type;
			this.jobID = jobID;
			this.receiverTaskID = receiverTaskID;
			this.senderTaskID = senderTaskID;
			this.data = data;
		}

		/**
		 * This is the nature of the message object.
		 */
		private Type type;
		Type getType() { return type; }
		void setType(Type type) { this.type = type; }

		/**
		 * The ID of the job which tasks are communicating using the message.
		 */
		private String jobID;
		String getJobID() { return jobID; }

		/**
		 * The ID of the receiver task.
		 */
		private String receiverTaskID;
		String getReceiverTaskID() { return receiverTaskID; }

		/**
		 * The ID of the sender task.
		 */
		private String senderTaskID;
		String getSenderTaskID() { return senderTaskID; }

		/**
		 * The data object that is being sent.
		 */
		private Object data;
		Object getData() { return data; }

		public String toString()
		{
			return "[F2FMessage: type=" + type + ", jobID=" + jobID
					+ ", receiverTaskId=" + receiverTaskID + ", senderTaskId="
					+ senderTaskID + ", data=" + data + "]";
		}

		/**
		 * Called by the JVM when the class is being serialized.
		 * 
		 * @param stream
		 * @throws IOException
		 */
		private void writeObject(java.io.ObjectOutputStream stream)
				throws IOException
		{
			stream.writeObject(type);
			stream.writeObject(jobID);
			stream.writeObject(receiverTaskID);
			stream.writeObject(senderTaskID);
			stream.writeObject(data);
		}

		/**
		 * Called by the JVM when the class is being deserialized.
		 */
		private void readObject(java.io.ObjectInputStream stream)
				throws IOException, ClassNotFoundException
		{
			// Do we use custom object input stream?
			boolean customOIPresent = stream instanceof CustomObjectInputStream;
			if (!customOIPresent)
				F2FDebug.println("\t!!! CustomObjectInputStream IS NOT BEING USED! Cannot cast custom objects!!!");

			type = (Type) stream.readObject();
			jobID = (String) stream.readObject();
			receiverTaskID = (String) stream.readObject();
			senderTaskID = (String) stream.readObject();
			try
			{
				// Read the data with custom loader only when we know the jobID
				// and the custom loader is present.
				data = (jobID != null && customOIPresent) ?
						// Pass the jobID to the custom loader in order
						// to resolve the class in it.
						((CustomObjectInputStream) stream).readObject(jobID)
						: stream.readObject();
				//F2FDebug.println("\tDezerialised F2FMessage data is: " + data);
			}
			catch (ClassNotFoundException e)
			{
				F2FDebug.println("\tERROR!!! deserializing F2FMessage data" + e);
			}
		}
	}
}
