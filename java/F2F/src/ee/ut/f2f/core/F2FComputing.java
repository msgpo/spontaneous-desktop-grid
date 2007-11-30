package ee.ut.f2f.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.swing.JOptionPane;

import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFactory;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.core.activity.CPURequests;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.NatMessageProcessor;

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
	public static Collection<Job> getJobs()
	{
		if (!isInitialized()) return null;
		return jobs.values();
	}
	public static Job getJob(String jobID)
	{
		if (!isInitialized()) return null;
		return jobs.get(jobID);
	}
	
	/**
	 * The parent directory for all job directories.
	 */
	private static java.io.File rootDirectory = null;

	private static CPURequests cpuRequests;
	
	private static F2FPeer localPeer = null;
	public static F2FPeer getLocalPeer() {return localPeer;}
	/**
	 * Collection of remote peers that are known.
	 */
	static Map<UUID, F2FPeer> peers = null;
	
	private static boolean isInitialized() { return localPeer != null; } 
	/**
	 * Private constructor for singleton implementation.
	 * 
	 * @throws CommunicationInitException
	 */
	private F2FComputing(java.io.File rootDir)
	{
		localPeer = new F2FPeer("me (localhost)");
		logger.debug("\tlocal F2FPeer ID is " + localPeer.getID());
		peers = new HashMap<UUID, F2FPeer>();
		peers.put(localPeer.getID(), localPeer);
		jobs = new HashMap<String, Job>();
		CommunicationFactory.getInitializedCommunicationProviders();
		rootDirectory = rootDir;
		rootDirectory.mkdir();
	}

	/**
	 * Initiates F2FComputing in rootDirectory which will be parent directory for all
	 * job directories.
	 * 
	 * @param rootDirectory Parent directory of all job directories
	 * @throws F2FComputingException 
	 * @throws CommunicationInitException
	 */
	public static void initiateF2FComputing(java.io.File rootDirectory)
	{
		if (isInitialized()) return;
		new F2FComputing(rootDirectory);
	}

	/**
	 * Initiates F2FComputing in ./__F2F_ROOTDIRECTORY which will be parent directory for all
	 * job directories.
	 * 
	 * @throws CommunicationInitException
	 * @throws F2FComputingException 
	 */
	public static void initiateF2FComputing() throws F2FComputingException
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
	 * 
	 * @param jarFiles Jar files that contain an algorithm that has to be executed.
	 * @param masterTaskClassName The name of class that contains the implementation of master task.
	 * @param peers The peers that have been selected to be 'slaves' of the algorithm.
	 * @throws F2FComputingException 
	 */
	public static Job createJob(Collection<String> jarFilesNames, String masterTaskClassName, Collection<F2FPeer> peers) throws F2FComputingException
	{
		if (!isInitialized()) return null;
		// create a job
		String jobID = newJobID();
		Job job = new Job(rootDirectory, jobID, jarFilesNames, peers);
		logger.info("Created new job with ID: " + jobID);
		// add job to jobs map
		jobs.put(jobID, job);
		ActivityManager.getDefault().emitEvent(
				new ActivityEvent(job.getJobActivity(),
						ActivityEvent.Type.STARTED, "Job created"));
		
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
		try
		{
			Task task = newTask(masterTaskDescription);
			task.start();
		}
		catch (Exception e)
		{
			logger.error("Error starting a master task: "+masterTaskDescription + e, e);
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
			int taskCount, Collection<F2FPeer> peers) throws F2FComputingException
	{
		if (!isInitialized()) return;
		logger.debug("Submitting " + taskCount + " tasks of " + className);
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

		cpuRequests = new CPURequests(jobID, peers, taskCount);
		
		try {
			// ask peers for CPU power
			cpuRequests.makeRequests();
			
			// wait for answers
			cpuRequests.waitForResponses();
		
			ActivityEvent event = new ActivityEvent(cpuRequests, ActivityEvent.Type.FINISHED,
					"Reserved:"+cpuRequests.getReservedPeers().size()+" peer(s); " +
							"busy:"+cpuRequests.getBusyPeers().size()+ " peer(s)");
			ActivityManager.getDefault().emitEvent(event);
		} catch (RuntimeException e) {
			ActivityEvent event = new ActivityEvent(cpuRequests, ActivityEvent.Type.FAILED,
					e.toString());
			ActivityManager.getDefault().emitEvent(event);
			throw e;
		} catch (F2FComputingException e) {
			ActivityEvent event = new ActivityEvent(cpuRequests, ActivityEvent.Type.FAILED,
					e.toString());
			ActivityManager.getDefault().emitEvent(event);
			throw e;
		}
		
		// now we know in which peers new tasks should be started
		F2FPeer[] peersToBeUsed = new F2FPeer[taskCount];
		Iterator<F2FPeer> reservedPeersIterator = cpuRequests.getReservedPeers().iterator();
		for (int i = 0; i < taskCount; i++) peersToBeUsed[i] = reservedPeersIterator.next();
		
		// create descriptions of new tasks and add them to the job
		Collection<TaskDescription> newTaskDescriptions = new ArrayList<TaskDescription>();
		for (F2FPeer peer: peersToBeUsed)
		{
			TaskDescription newTaskDescription = 
				new TaskDescription(
					jobID, job.newTaskId(),
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
		F2FMessage messageJob = 
			new F2FMessage(F2FMessage.Type.JOB, null, null, null, job);
		for (F2FPeer peer: newPeers)
		{
			try {
				peer.sendMessage(messageJob);
			} catch (CommunicationFailedException e) {
				logger.error("Error sending the job to a peer. " + e, e);
			}
		}
		// ... notify other peers about additional tasks
		F2FMessage messageTasks = 
			new F2FMessage(F2FMessage.Type.TASKS, job.getJobID(), null, null, newTaskDescriptions);
		for (F2FPeer peer: job.getWorkingPeers())
		{
			if (newPeers.contains(peer)) continue;
			try {
				peer.sendMessage(messageTasks);
			} catch (CommunicationFailedException e) {
				logger.error("Error sending new tasks to a peer. " + e, e);
			}
		}
				
		cpuRequests.getReservedPeers().remove(jobID);
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
		logger.debug("Loading class: " + taskDescription.className);
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
	public static ClassLoader getJobClassLoader(String jobID)
	{
		// If there is no such job
		if (!isInitialized() || !jobs.containsKey(jobID))
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
	public static Collection<F2FPeer> getPeers()
			throws CommunicationFailedException
	{
		if (!isInitialized()) return null;
		return peers.values();
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
				peer = new F2FPeer(peerID, displayName);
				peers.put(peerID, peer);
			}
		}
		peer.addCommProvider(comm);
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
		}
	}
	
	private static void startJobTasks(Job job)
	{
		// find the tasks, that are not created yet and are
		// ment for execution in this peer, and start them
		for (TaskDescription taskDesc : job.getTaskDescriptions())
		{
			if (job.getTask(taskDesc.getTaskID()) == null &&
				taskDesc.peerID.equals(localPeer.getID()))
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

	/**
	 * Handles F2F framework messages and forwards messages sent between tasks.
	 */
	@SuppressWarnings("unchecked")
	public static void messageRecieved(Object message, UUID senderID)
	{
		if (!isInitialized()) return;
		F2FPeer sender = peers.get(senderID);
		// throw away messages from unknown peers
		if (sender == null) return;
		
		if (message instanceof F2FMessage);
		else 
		{
			logger.warn("messageRecieved() handles only F2FMessages!");
			return;
		}
		F2FMessage f2fMessage = (F2FMessage) message;
		if (f2fMessage.getType() == F2FMessage.Type.REQUEST_FOR_CPU)
		{
			try
			{
				logger.debug("got REQUEST_FOR_CPU. answer it with RESPONSE_FOR_CPU");
				F2FMessage responseMessage = 
					new F2FMessage(
						F2FMessage.Type.RESPONSE_FOR_CPU, 
						f2fMessage.getJobID(), null, null,
						askForCPU(sender));
				sender.sendMessage(responseMessage);
			}
			catch (CommunicationFailedException e)
			{
				e.printStackTrace();
			}
		}
		else if (f2fMessage.getType() == F2FMessage.Type.RESPONSE_FOR_CPU)
		{
			if (cpuRequests != null
					&& f2fMessage.getJobID().equals(cpuRequests.getJobID())) {
				cpuRequests.responseReceived(f2fMessage, sender);
			} else {
				logger.warn("Received unexpected 'CPU request' response");
			}
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
						new ActivityEvent(job.getJobActivity(),
								ActivityEvent.Type.STARTED, "Job received"));				
				
				startJobTasks(job);
			}
			catch (F2FComputingException e)
			{
				logger.error("" + e, e);
			}
		}
		else if (f2fMessage.getType() == F2FMessage.Type.TASKS)
		{
			logger.info("got TASKS");
			Job job = getJob(f2fMessage.getJobID());
			if (job == null)
			{
				logger.error("Received tasks for unknown job");
				return;
			}
			Collection<TaskDescription> taskDescriptions = (Collection<TaskDescription>) f2fMessage.getData();
			job.addTaskDescriptions(taskDescriptions);
			startJobTasks(job);
		}
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
			F2FPeer receiver = peers.get(receiverTaskDesc.peerID);
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
		else if (f2fMessage.getType() == F2FMessage.Type.CHAT)
		{	
			//NAT/Traversal filtering
			//Decapsulating message content
			String msg = (String) f2fMessage.getData();
			if( msg != null && msg.startsWith("/NAT>/")){
				//NAT Messages
				logger.debug("Received NAT message, size [" + msg.length() + "], forwarding to NatMessageProcessor");

					NatMessageProcessor.processIncomingNatMessage(msg);

			} else {
				//Others
				F2FComputingGUI.controller.writeMessage(sender.getDisplayName(), (String)f2fMessage.getData());
			}
		}
	}
	
	private static boolean allowAllFriendsToUseMyPC = false;
	public static void allowAllFriendsToUseMyPC(boolean allow)
	{
		logger.info((allow ? "Allow" : "Do not allow") + " all my friends to use my PC by default");
		allowAllFriendsToUseMyPC = allow;
	}
	private static Boolean askForCPU(F2FPeer peer)
	{
		// do not ask the permission from ourselves
		if (peer.equals(localPeer)) return Boolean.TRUE;
		
		// check if all friends are allowed to use this PC
		if (allowAllFriendsToUseMyPC) return Boolean.TRUE;
	
		// ask the owner
		int n = JOptionPane.showConfirmDialog(
                null, "Do you allow " + peer.getDisplayName() + " to use your PC?",
                "F2FComputing", JOptionPane.YES_NO_OPTION);
		if (n == JOptionPane.YES_OPTION) return Boolean.TRUE;
		
		// TODO: 
		//?   1) add a checkbox to the dialog that user would not be asked again later at all (allow all)
		//?   2) add a checkbox that the specified friend is always trusted
		return Boolean.FALSE;
	}
}
