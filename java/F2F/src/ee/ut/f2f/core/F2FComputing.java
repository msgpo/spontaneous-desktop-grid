package ee.ut.f2f.core;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.swing.JOptionPane;

import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFactory;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.util.logging.Logger;

/**
 * This is the core class of F2F framework. It provides methods to create new
 * jobs and tasks.
 */
public class F2FComputing
{
	private static final Logger logger = Logger.getLogger(F2FComputing.class);
	/**
	 * Map jobID->Job, contains all the jobs that the F2F framework is aware
	 * at. New jobs can be added by user by GUI or received from other nodes.
	 */
	private static Map<String, Job> jobs = null;
	/**
	 * Returns all the jobs that are currently known.
	 */
	public static Collection<Job> getJobs()
	{
		if (!isInitialized()) return null;
		return jobs.values();
	}
	/**
	 * Returns the job with the given ID or null if such a job is not known.
	 */
	public static Job getJob(String jobID)
	{
		if (!isInitialized()) return null;
		return jobs.get(jobID);
	}
	
	/**
	 * The parent directory for all job directories.
	 */
	private static java.io.File rootDirectory = null;
	
	private static F2FPeer localPeer = null;
	/**
	 * Returns a peer in F2F frameork that represents the local machine.
	 */
	public static F2FPeer getLocalPeer() {return localPeer;}
	/**
	 * Collection of remote peers that are known.
	 */
	private static Map<UUID, F2FPeer> peers = new HashMap<UUID, F2FPeer>();
	
	private static boolean isInitialized() { return localPeer != null; }
	/**
	 * Private constructor for singleton implementation.
	 */
	private F2FComputing(java.io.File rootDir)
	{
		rootDirectory = rootDir;
		rootDirectory.mkdir();
		jobs = new HashMap<String, Job>();
		localPeer = new F2FPeer("me (localhost)");
		logger.debug("local F2FPeer ID is " + localPeer.getID());
		peers.put(localPeer.getID(), localPeer);
		// init comm providers
		CommunicationFactory.getInitializedCommunicationProviders();
	}

	/**
	 * Initiates F2FComputing in rootDirectory which will be parent 
	 * directory for all job directories.
	 * 
	 * @param rootDirectory Parent directory of all job directories
	 */
	public static void initiateF2FComputing(java.io.File rootDirectory)
	{
		if (isInitialized()) return;
		new F2FComputing(rootDirectory);
	}

	/**
	 * Initiates F2FComputing in ./__F2F_ROOTDIRECTORY which will be parent
	 * directory for all job directories.
	 */
	public static void initiateF2FComputing()
	{
		if (isInitialized()) return;
		String tempName = "__F2F_ROOTDIRECTORY";
		tempName = tempName.replaceAll("\\W", "_");
		initiateF2FComputing(new java.io.File(tempName));
	}

	/**
	 * Creates new job and executes its master task. This method is ment to be
	 * used from GUI (for example, an user selects jar files that contain needed
	 * algorithm and selects peers whose computational power he/she wants
	 * to use, specifies the name of master task class and initiates start of
	 * the job by pressing "Start" button).
	 * The method asks if the given peers allow to use their PC.
	 * 
	 * @param jarFiles Jar files that contain an algorithm that has to be executed.
	 * @param masterTaskClassName The name of class that contains the implementation of master task.
	 * @param peers The peers that have been selected to be involved in the execution of the job.
	 */
	public static Job createJob(Collection<String> jarFilesNames, String masterTaskClassName, Collection<F2FPeer> peers) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		if (!isInitialized()) return null;
		// create a job
		String jobID = newJobID();
		Job job = new Job(rootDirectory, jobID, jarFilesNames, peers);
		logger.info("Created new job with ID: " + jobID);
		// add job to jobs map
		jobs.put(jobID, job);
		
		// set the master task ID of the job
		String masterTaskID = job.newTaskId();
		job.setMasterTaskID(masterTaskID);
		// create the description of master task and add it to the job
		TaskDescription masterTaskDescription = 
			new TaskDescription( 
					jobID, 
					masterTaskID, 
					localPeer.getID(), 
					masterTaskClassName
			);
		job.addTaskDescription(masterTaskDescription);
		job.addWorkingPeer(localPeer);
		// create a task based on master task description and execute it	
		Task task = newTask(masterTaskDescription);
		task.start();
		
		return job;
	}

	/**
	 * Generate unique ID for a job.
	 * ID of a job consists of "Job" + 'a random integer that is generated in the local peer' + 'the ID of the local peer'
	 */
	private static String newJobID() { return "Job"+(++lastJobID)+localPeer.getID(); }
	private static int lastJobID = new Random(UUID.randomUUID().getLeastSignificantBits()).nextInt();
		
	private static boolean checkSubmitTasks(Job job,
			int taskCount, Collection<F2FPeer> peers) throws F2FComputingException
	{
		if (taskCount < 1)
		{
			logger.debug("no tasks to submit (taskCount < 1)");
			return false;
		}
		if (job.getTask(job.getMasterTaskID()) == null)
			throw new NotMasterException();
		if (peers == null || peers.size() < taskCount)
			throw new NotEnoughPeersException(taskCount, peers == null ? 0: peers.size());
		for (F2FPeer peer: peers)
			if (!job.getPeers().contains(peer))
				throw new NotJobPeerException(peer, job);
		return true;
	}
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
	 * @param taskCount The number of tasks to make. It should be more than 0.
	 * @param peers The collection of peers to where new tasks should be sent.
	 * 	This collection should hold at least taskCount peers.
	 */
	static void submitTasks(Job job, String className,
			int taskCount, Collection<F2FPeer> peers) throws F2FComputingException, ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		if (!isInitialized()) return;
		logger.debug("Submitting " + taskCount + " tasks of " + className);
		
		if (!checkSubmitTasks(job, taskCount, peers)) return;
		
		// try to load the class
		ClassLoader loader = job.getClassLoader();
		Class clazz = loader.loadClass(className);
		@SuppressWarnings("unused")
		Task task = (Task) clazz.newInstance();
				
		ActivityManager.getDefault().emitEvent(new ActivityEvent(job, ActivityEvent.Type.CHANGED, 
				"submitting " + taskCount + " tasks of " + className));
	
		// wait for answers
		Iterator<F2FPeer> reservedPeersIterator = job.getCPURequests().waitForResponses(taskCount, peers);
		
		// now we know in which peers new tasks should be started
		F2FPeer[] peersToBeUsed = new F2FPeer[taskCount];
		for (int i = 0; i < taskCount; i++) peersToBeUsed[i] = reservedPeersIterator.next();
		
		// create descriptions of new tasks and add them to the job
		Collection<TaskDescription> newTaskDescriptions = new ArrayList<TaskDescription>();
		for (F2FPeer peer: peersToBeUsed)
		{
			TaskDescription newTaskDescription = 
				new TaskDescription(
					job.getJobID(), job.newTaskId(),
					peer.getID(), className);
			job.addTaskDescription(newTaskDescription);
			newTaskDescriptions.add(newTaskDescription);
			logger.debug("Added new taskdescription to the job: "
					+ newTaskDescription);
		}
		
		// send Job to those peers who are new for this job and ...
		Collection<F2FPeer> newPeers = new ArrayList<F2FPeer>();
		for (F2FPeer peer: peersToBeUsed)
		{
			if (job.getWorkingPeers() == null || !job.getWorkingPeers().contains(peer))
			{
				newPeers.add(peer);
				job.addWorkingPeer(peer);
			}
		}
		
		//TODO: if sendig fails nothing is done at the moment
		// possible solutions
		// 1) throw exception - in this case some of the tasks start running,
		//	  maybe master should be able to determine which are running and
		//    submit some new ones (but this is possible already now (exchange
		//    some test messages));
		// 2) try to submit to a new peer - this means that according task 
		//    description has to be updated (in all peers)
		// NB! do not return from this method before the submit has succeeded
		//     or failed (tasks have started in remote peers)!
		
		Collection<Thread> threads = new ArrayList<Thread>();
		final F2FMessage messageJob = 
			new F2FMessage(F2FMessage.Type.JOB, null, null, null, job);
		for (final F2FPeer peer: newPeers)
		{
			Thread thread = new Thread()
				{
					public void run()
					{
						try {
							peer.sendMessage(messageJob);
						} catch (CommunicationFailedException e) {
							logger.error("Error sending a job to a peer. " + e, e);
						}
					}
				};
			threads.add(thread);
			thread.start();
		}
		// ... notify other peers about additional tasks
		final F2FMessage messageTasks = 
			new F2FMessage(F2FMessage.Type.TASK_DESCRIPTIONS, job.getJobID(), null, null, newTaskDescriptions);
		for (final F2FPeer peer: job.getWorkingPeers())
		{
			if (newPeers.contains(peer)) continue;
			
			Thread thread = new Thread()
				{
					public void run()
					{
						try {
							peer.sendMessage(messageTasks);
						} catch (CommunicationFailedException e) {
							logger.error("Error sending new tasks to a peer. " + e, e);
						}
					}
				};
			threads.add(thread);
			thread.start();
		}
		for (Thread thread: threads)
		{
			while (true)
			{
				try
				{
					thread.join();
					break;
				} catch (InterruptedException e) {}
			}
		}
		
		ActivityManager.getDefault().emitEvent(new ActivityEvent(job, ActivityEvent.Type.CHANGED, 
				"tasks submitted"));
	}
	
	static void submitTasks(Job job, Collection<Task> tasks,
			Collection<F2FPeer> peers) throws F2FComputingException, ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		if (!isInitialized()) return;
		int taskCount = (tasks != null? tasks.size(): 0);
		logger.debug("Submitting " + taskCount + " tasks");
		
		if (!checkSubmitTasks(job, taskCount, peers)) return;
		
		for (Task task: tasks)
			if (!(task instanceof Serializable))
				throw new NotSerializableTaskException(task);
						
		ActivityManager.getDefault().emitEvent(new ActivityEvent(job, ActivityEvent.Type.CHANGED, 
				"submitting " + taskCount + " tasks"));
	
		// wait for answers
		Iterator<F2FPeer> reservedPeersIterator = job.getCPURequests().waitForResponses(taskCount, peers);
		
		// now we know in which peers new tasks should be started
		Collection<F2FPeer> peersToBeUsed = new ArrayList<F2FPeer>();
		for (int i = 0; i < taskCount; i++) peersToBeUsed.add(reservedPeersIterator.next());
		
		// create descriptions of new tasks and add them to the job
		Iterator<Task> tasksIter = tasks.iterator();
		Hashtable<F2FPeer, Task> peerTask = new Hashtable<F2FPeer, Task>();
		Collection<TaskDescription> newTaskDescriptions = new ArrayList<TaskDescription>();
		for (F2FPeer peer: peersToBeUsed)
		{
			Task task = tasksIter.next();
			TaskDescription newTaskDescription = 
				new TaskDescription(
					job.getJobID(), job.newTaskId(),
					peer.getID(), task.getClass().toString());
			job.addTaskDescription(newTaskDescription);
			newTaskDescriptions.add(newTaskDescription);
			peerTask.put(peer, task);
			logger.debug("Added new taskdescription to the job: "
					+ newTaskDescription);
		}
		
		// send Job to those peers who are new for this job and ...
		Collection<F2FPeer> newPeers = new ArrayList<F2FPeer>();
		for (F2FPeer peer: peersToBeUsed)
		{
			if (job.getWorkingPeers() == null || !job.getWorkingPeers().contains(peer))
			{
				newPeers.add(peer);
				job.addWorkingPeer(peer);
			}
		}
		
		//TODO: if sendig fails nothing is done at the moment
		// possible solutions
		// 1) throw exception - in this case some of the tasks start running,
		//	  maybe master should be able to determine which are running and
		//    submit some new ones (but this is possible already now (exchange
		//    some test messages));
		// 2) try to submit to a new peer - this means that according task 
		//    description has to be updated (in all peers)
		// NB! do not return from this method before the submit has succeeded
		//     or failed (tasks have started in remote peers)!
		
		tasksIter = tasks.iterator();
		Collection<Thread> threads = new ArrayList<Thread>();
		final F2FMessage messageJobTask = 
			new F2FMessage(F2FMessage.Type.JOB_TASK, null, null, null, job);
		for (final F2FPeer peer: newPeers)
		{
			final F2FMessage messageTask = 
				new F2FMessage(F2FMessage.Type.TASK, job.getJobID(), null, null, new F2FTaskMessage(null, peerTask.get(peer)));
			Thread thread = new Thread()
				{
					public void run()
					{
						try {
							peer.sendMessage(messageJobTask);
							peer.sendMessage(messageTask);
						} catch (CommunicationFailedException e) {
							logger.error("Error sending a job to a peer. " + e, e);
						}
					}
				};
			threads.add(thread);
			thread.start();
		}
		// ... send tasks to the peers where they are executed and ...
		for (final F2FPeer peer: peersToBeUsed)
		{
			if (newPeers.contains(peer)) continue;
			
			final F2FMessage messageTask = 
				new F2FMessage(F2FMessage.Type.TASK, job.getJobID(), null, null, new F2FTaskMessage(newTaskDescriptions, peerTask.get(peer)));
			Thread thread = new Thread()
				{
					public void run()
					{
						try {
							peer.sendMessage(messageTask);
						} catch (CommunicationFailedException e) {
							logger.error("Error sending a job to a peer. " + e, e);
						}
					}
				};
			threads.add(thread);
			thread.start();
		}
		// ... notify other peers about additional tasks
		final F2FMessage messageTasks = 
			new F2FMessage(F2FMessage.Type.TASK_DESCRIPTIONS, job.getJobID(), null, null, newTaskDescriptions);
		for (final F2FPeer peer: job.getWorkingPeers())
		{
			if (peersToBeUsed.contains(peer)) continue;
			
			Thread thread = new Thread()
				{
					public void run()
					{
						try {
							peer.sendMessage(messageTasks);
						} catch (CommunicationFailedException e) {
							logger.error("Error sending new tasks to a peer. " + e, e);
						}
					}
				};
			threads.add(thread);
			thread.start();
		}
		for (Thread thread: threads)
		{
			while (true)
			{
				try
				{
					thread.join();
					break;
				} catch (InterruptedException e) {}
			}
		}
		
		ActivityManager.getDefault().emitEvent(new ActivityEvent(job, ActivityEvent.Type.CHANGED, 
				"tasks submitted"));
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
	 */
	private static Task newTask(TaskDescription taskDescription)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException
	{
		// this job must definetely exist!
		Job job = jobs.get(taskDescription.getJobID());
		ClassLoader loader = job.getClassLoader();
		logger.debug("Loading class: " + taskDescription.getClassName());
		Class clazz = loader.loadClass(taskDescription.getClassName());
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
	static ClassLoader getJobClassLoader(String jobID)
	{
		// If there is no such job
		if (!isInitialized() || !jobs.containsKey(jobID))
			return null;
		// Return the classloader.
		return jobs.get(jobID).getClassLoader();
	}

	/**
	 * This method is mainly ment for GUI to show available peers.
	 * But this collection of peers can also be used to create a new job.
	 * 
	 * @return All peers that are known through communication providers.
	 */
	public static Collection<F2FPeer> getPeers()
	{
		return peers.values();
	}

	/**
	 * This method can be used to get reference to a peer if its ID is known.
	 * 
	 * @param id the ID of a peer
	 * @return The peer which ID is id, or null if such a peer is not known.
	 */
	public static F2FPeer getPeer(UUID id)
	{
		if (!peers.containsKey(id)) return null;
		return peers.get(id);
	}
	
	private static ArrayList<PeerPresenceListener> peerListeners = new ArrayList<PeerPresenceListener>();
	public static void addPeerPresenceListener(PeerPresenceListener listener)
	{
		synchronized (peerListeners)
		{
			if (!peerListeners.contains(listener))
				peerListeners.add(listener);
		}
	}	
	public static void removePeerPresenceListener(PeerPresenceListener listener)
	{
		synchronized (peerListeners)
		{
			if (peerListeners.contains(listener))
				peerListeners.remove(listener);
		}
	}
	
	public static void peerContacted(UUID peerID, String displayName, CommunicationProvider comm)
	{
		if (!isInitialized()) return;
		F2FPeer peer = null;
		synchronized (peers)
		{
			peer = peers.get(peerID);
			if (peer == null)
			{
				peer = new F2FPeer(peerID, displayName, comm);
				peers.put(peerID, peer);
				synchronized (peerListeners)
				{
					for (final PeerPresenceListener listener: peerListeners)
					{
						final F2FPeer fPeer = peer;
						new Thread() {
							public void run()
							{
								listener.peerContacted(fPeer);
							}
						}.start();
					}
				}
				return;
			}
			peer.addCommProvider(comm);
		}
	}

	public static void peerUnContacted(UUID peerID, CommunicationProvider comm)
	{
		if (!isInitialized()) return;
		F2FPeer peer = peers.get(peerID);
		if (peer == null) return;
		peer.removeCommProvider(comm);
		if (peer.isContactable()) return;
		synchronized (peers)
		{
			peers.remove(peerID);
			synchronized (peerListeners)
			{
				for (final PeerPresenceListener listener: peerListeners)
				{
					final F2FPeer fPeer = peer;
					new Thread() {
						public void run()
						{
							listener.peerUnContacted(fPeer);
						}
					}.start();
				}
			}
		}
	}
	
	private static void startJobTasks(Job job)
	{
		// find the tasks, that are not created yet and are
		// ment for execution in this peer, and start them
		for (TaskDescription taskDesc : job.getTaskDescriptions())
		{
			if (job.getTask(taskDesc.getTaskID()) == null &&
				taskDesc.getPeerID().equals(localPeer.getID()))
			{
				try
				{
					logger.debug("Starting a task: " + taskDesc);
					Task task = newTask(taskDesc);
					task.start();
				}
				catch (Exception e)
				{
					logger.error("Error starting a task: "
							+ taskDesc
							+ e, e);
				}
			}
		}
	}
	private static void startJobTask(Job job, Task task)
	{
		// find the tasks, that are not created yet and are
		// ment for execution in this peer, and start them
		for (TaskDescription taskDesc : job.getTaskDescriptions())
		{
			if (job.getTask(taskDesc.getTaskID()) == null &&
				taskDesc.getPeerID().equals(localPeer.getID()) &&
				taskDesc.getClassName().equals(task.getClass().toString()))
			{
				try
				{
					logger.debug("Starting a task: " + taskDesc);
					task.setTaskDescription(taskDesc);
					job.addTask(task);
					task.start();
				}
				catch (Exception e)
				{
					logger.error("Error starting a task: "
							+ taskDesc
							+ e, e);
				}
			}
		}
	}

	/**
	 * MessageType -> Listeners
	 */
	private static HashMap<Class, Collection<F2FMessageListener>> messageListeners = new HashMap<Class, Collection<F2FMessageListener>>();
	public static void addMessageListener(Class messageType, F2FMessageListener listener)
	{
		synchronized (messageListeners)
		{
			if (!messageListeners.containsKey(messageType))
				messageListeners.put(messageType, new ArrayList<F2FMessageListener>());
			if (!messageListeners.get(messageType).contains(listener))
				messageListeners.get(messageType).add(listener);
		}
	}
	public static void removeMessageListener(Class messageType, F2FMessageListener listener)
	{
		synchronized (messageListeners)
		{
			if (!messageListeners.containsKey(messageType)) return;
			if (!messageListeners.get(messageType).contains(listener)) return;
			messageListeners.get(messageType).remove(listener);
		}
	}
	
	/**
	 * This method is called by a communication provider (IM, TCP, UDP etc) if
	 * a message from a remote peer has been received.
	 * This method directs the message to the right place for processing.
	 * 
	 * Messages of type F2FMessage (framework's internal messages and messages
	 * that are exchanged between Tasks) are handled in this method.
	 * Messages of other types are handled only if some listener is interested
	 * in them (has registered itself to be a F2FMessageListener that wants to
	 * be notified if a message of a specific type is received). 
	 */
	public static void messageReceived(final Object message, UUID senderID)
	{
		if (!isInitialized()) return;
		final F2FPeer sender = peers.get(senderID);
		// throw away messages from unknown peers
		if (sender == null)
		{
			logger.warn("the framework received a message from an unknown peer " + senderID);
			return;
		}
		// F2FMessages are handeled in this method
		if (!(message instanceof F2FMessage))
		{ // other types of messages should have a listener
			HashMap<Class, Collection<F2FMessageListener>> messageListenersCopy = null;
			synchronized (messageListeners)
			{
				messageListenersCopy = new HashMap<Class, Collection<F2FMessageListener>>(messageListeners);
			}
			
			if (!messageListenersCopy.containsKey(message.getClass()))
			{
				logger.warn("the framework does not know the handler of messages of " + message.getClass());
				return;
			}
			
			for (final F2FMessageListener listener: messageListenersCopy.get(message.getClass()))
			{
				new Thread() {
					public void run()
					{
						listener.messageReceived(message, sender);
					}
				}.start();
			}
			return;
		}
		F2FMessage f2fMessage = (F2FMessage) message;
		// JOB/TASK START
		if (f2fMessage.getType() == F2FMessage.Type.REQUEST_FOR_CPU)
		{
			askForCPU(sender, f2fMessage.getJobID());
		}
		else if (f2fMessage.getType() == F2FMessage.Type.RESPONSE_FOR_CPU)
		{
			Job job = getJob(f2fMessage.getJobID());
			if (job == null)
			{
				logger.error("Received RESPONSE_FOR_CPU for unknown job");
				return;
			}
			CPURequests requests = job.getCPURequests(); 
			if (requests == null)
			{
				logger.error("Received RESPONSE_FOR_CPU but requester is null");
				return;
			}
			requests.responseReceived(f2fMessage, sender);
		}
		else if (f2fMessage.getType() == F2FMessage.Type.JOB)
		{
			logger.info("got JOB");
			Job job = (Job) f2fMessage.getData();
			// check if we know this job already
			if (jobs.containsKey(job.getJobID()))
			{
				logger.error("Received a job that is already known!");
				return;
			}
			try
			{
				job.initialize(rootDirectory);
				jobs.put(job.getJobID(), job);
				ActivityManager.getDefault().emitEvent(
						new ActivityEvent(job,
								ActivityEvent.Type.CHANGED, "Job received"));				
				startJobTasks(job);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				logger.error("" + e, e);
			}
		}
		else if (f2fMessage.getType() == F2FMessage.Type.JOB_TASK)
		{
			logger.info("got JOB_TASK");
			Job job = (Job) f2fMessage.getData();
			// check if we know this job already
			if (jobs.containsKey(job.getJobID()))
			{
				logger.error("Received a job that is already known!");
				return;
			}
			try
			{
				job.initialize(rootDirectory);
				jobs.put(job.getJobID(), job);
				ActivityManager.getDefault().emitEvent(
						new ActivityEvent(job,
								ActivityEvent.Type.CHANGED, "Job received"));				
			}
			catch (IOException e)
			{
				e.printStackTrace();
				logger.error("" + e, e);
			}
		}
		else if (f2fMessage.getType() == F2FMessage.Type.TASK_DESCRIPTIONS)
		{
			logger.info("got TASK_DESCRIPTIONS");
			Job job = getJob(f2fMessage.getJobID());
			if (job == null)
			{
				logger.error("Received tasks for unknown job");
				return;
			}
			@SuppressWarnings("unchecked")
			Collection<TaskDescription> taskDescriptions = (Collection<TaskDescription>) f2fMessage.getData();
			job.addTaskDescriptions(taskDescriptions);
			startJobTasks(job);
		}
		else if (f2fMessage.getType() == F2FMessage.Type.TASK)
		{
			logger.info("got TASK");
			Job job = getJob(f2fMessage.getJobID());
			if (job == null)
			{
				logger.error("Received tasks for unknown job");
				return;
			}
			F2FTaskMessage msgTask = (F2FTaskMessage) f2fMessage.getData();
			Collection<TaskDescription> taskDescriptions = msgTask.getTaskDescriptions();
			job.addTaskDescriptions(taskDescriptions);
			startJobTask(job, msgTask.getTask());
		}
		// MESSAGES TO TASKS
		else if (f2fMessage.getType() == F2FMessage.Type.MESSAGE)
		{
			if(logger.isTraceEnabled()) {
				logger.trace("MESSAGE received " + f2fMessage);
			}
			Job job = getJob(f2fMessage.getJobID());
			if (job == null)
			{
				logger.warn("Got MESSAGE for unknown job with ID: "
						+ f2fMessage.getJobID());
				return;
			}
			Task recepientTask = job.getTask(f2fMessage.getReceiverTaskID());
			if (recepientTask == null)
			{
				logger.warn("Got MESSAGE for unknown task with ID: "
						+ f2fMessage.getReceiverTaskID());
				return;
			}
			recepientTask.getTaskProxy(f2fMessage.getSenderTaskID())
					.saveMessage(f2fMessage.getData());
		}
		else if (f2fMessage.getType() == F2FMessage.Type.ROUTE)
		{
			if(logger.isTraceEnabled()) {
				logger.trace("Received ROUTE: " + f2fMessage);
			}
			f2fMessage.setType(F2FMessage.Type.MESSAGE);
			Job job = getJob(f2fMessage.getJobID());
			if (job == null)
			{
				logger.error("didn't find the job");
				return;
			}
			TaskDescription receiverTaskDesc = job
					.getTaskDescription(f2fMessage.getReceiverTaskID());
			if (receiverTaskDesc == null)
			{
				logger.error("didn't find the receiver task description");
				return;
			}
			F2FPeer receiver = getPeer(receiverTaskDesc.getPeerID());
			if (receiver == null)
			{
				logger.error("didn't find the receiver peer");
				return;
			}
			try
			{
				receiver.sendMessage(f2fMessage);
			}
			catch (CommunicationFailedException e)
			{
				logger.error("couldn't send the message to the route target", e);
			}
		}
	}
	
	private static boolean allowAllFriendsToUseMyPC = false;
	public static void allowAllFriendsToUseMyPC(boolean allow)
	{
		logger.info((allow ? "Allow" : "Do not allow") + " all my friends to use my PC by default");
		allowAllFriendsToUseMyPC = allow;
	}
	
	private static void askForCPU(final F2FPeer peer, final String jobID)
	{
		new Thread()
		{
			public void run()
			{
				logger.debug("got REQUEST_FOR_CPU");
				Boolean response = null;
				// do not ask the permission from ourselves
				if (peer.equals(localPeer)) response = true;
				
				// check if all friends are allowed to use this PC
				else if (allowAllFriendsToUseMyPC) response = true;
			
				// ask the owner
				else
				{
					int n = JOptionPane.showConfirmDialog(
			                null, "Do you allow " + peer.getDisplayName() + " to use your PC?",
			                "F2FComputing", JOptionPane.YES_NO_OPTION);
					if (n == JOptionPane.YES_OPTION) response = true;
					else response = false;
					// TODO: 
					//?   1) add a checkbox to the dialog that user would not be asked again later at all (allow all)
					//?   2) add a checkbox that the specified friend is always trusted
				}

				F2FMessage responseMessage = 
					new F2FMessage(
						F2FMessage.Type.RESPONSE_FOR_CPU, 
						jobID, null, null,
						response);
				try
				{
					peer.sendMessage(responseMessage);
				}
				catch (CommunicationFailedException e)
				{
					e.printStackTrace();
				}
			}
		}.start();
	}
}
