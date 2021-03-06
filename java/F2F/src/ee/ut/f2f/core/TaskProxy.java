package ee.ut.f2f.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.logging.Logger;

/**
 * TaskProxy represents link to a remote task.
 * It is used to send/receive message to/from the 
 * remote task.
 */
public class TaskProxy
{	
	private static final Logger logger = Logger.getLogger(TaskProxy.class);
	
	/** Recieved messages in the queue. */
	private Queue<Object> messages = new ConcurrentLinkedQueue<Object>();

	private Task task;
	/** Whose proxy is this. */
	private TaskDescription remoteTaskDescription;
	
	protected TaskProxy(Task task, TaskDescription remoteTaskDescription)
	{
		this.task = task;
		this.remoteTaskDescription = remoteTaskDescription;
		logger.info("Created new TaskProxy of task " + remoteTaskDescription.getTaskID());
	}

	/** 
	 * Sends a message to the corresponding task.
	 *  
	 * @throws CommunicationFailedException 
	 */
	public void sendMessage(Object message) throws CommunicationFailedException
	{
		if (remoteTaskDescription == null)
		{
			logger.error("remoteTaskDescription == null");
			return;
		}
		// try to send message to the receiver
		F2FPeer receiver = F2FComputing.getPeer(remoteTaskDescription.getPeerID());
		if (receiver != null)
		{
			F2FMessage f2fMessage = 
				new F2FMessage(
					F2FMessage.Type.MESSAGE,
					task.getJob().getJobID(),
					remoteTaskDescription.getTaskID(),
					task.getTaskID(),
					message);
			receiver.sendMessage(f2fMessage);
			return;
		}
		else throw new CommunicationFailedException("Task " + getRemoteTaskID() + " is not reachable!");
	}

	/** 
	 * Sends a message to the corresponding task and blocks until the 
	 * receiver has got it.
	 *  
	 * @throws CommunicationFailedException 
	 * @throws InterruptedException 
	 */
	public void sendMessageBlocking(Object message) throws CommunicationFailedException, InterruptedException
	{
		if (remoteTaskDescription == null)
		{
			logger.error("remoteTaskDescription == null");
			return;
		}
		// try to send message to the receiver
		F2FPeer receiver = F2FComputing.getPeer(remoteTaskDescription.getPeerID());
		if (receiver != null)
		{
			F2FMessage f2fMessage = 
				new F2FMessage(
						F2FMessage.Type.MESSAGE,
						task.getJob().getJobID(),
						remoteTaskDescription.getTaskID(),
						task.getTaskID(),
						message);
			try
			{
				receiver.sendMessageBlocking(f2fMessage);
				return;
			}
			catch (CommunicationFailedException e)
			{
				logger.warn("could not send a message to a tast directly, try to route via master");
			}
		}
		else logger.warn("could not find receiver peer!");
	}

	/**
	 * Message will be saved in this proxy's message queue. The message can be
	 * aqcuired from {@link #receiveMessage}.
	 */
	void saveMessage(Object message)
	{
		if(logger.isTraceEnabled()) {		
			logger.trace("PROXY of " + remoteTaskDescription.getTaskID()
					+ ": want to add a message ...");
		}
		synchronized (messages)
		{
			this.messages.add(message);
			if(logger.isTraceEnabled()) {
				logger.trace("PROXY of " + remoteTaskDescription.getTaskID()
						+ ": added message " + message);
			}
			messages.notify();
		}
		// notify the task that a message from a remote task was received
		new Thread()
		{
			public void run()
			{
				task.messageReceivedEvent(remoteTaskDescription.getTaskID());
			}
		}.start();
	}
	
	/**
	 * @param timeoutInMillis Custom timeout when the method will return
	 * if it has not found the message. 0 or negative value will wait until
	 * message is received.
	 * 
	 * @return message recieved from that (Proxy) task; <code>null</code> if
	 *         there is no messages after the timeout.
	 */
	public Object receiveMessage(long timeoutInMillis)
	{
		if(logger.isTraceEnabled()) {
			logger.trace("PROXY of " + remoteTaskDescription.getTaskID()
					+ ": want to read a message ...");
		}
		synchronized (messages)
		{
			if (messages.isEmpty())
			{
				while (true)
				{
					try
					{
						if (timeoutInMillis < 0) timeoutInMillis = 0;
						messages.wait(timeoutInMillis);
						break;
					}
					catch (InterruptedException e){}
				}
			}
			Object message = messages.poll(); 
			logger.trace("PROXY of " + remoteTaskDescription.getTaskID()
					+ ": read/removed message " + message);
			return message;
		}
	}

	/**
	 * @return message recieved from that (Proxy) task; blocks until there is 
	 * 	message to return.
	 */
	public Object receiveMessage()
	{
		return receiveMessage(0);
	}

	/**
	 * @return <code>true</code> if there is message from the task,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasMessage()
	{
		return !messages.isEmpty();
	}

	/**
	 * @return The ID of the task to which this proxy belongs to.
	 */
	public String getRemoteTaskID()
	{
		return remoteTaskDescription.getTaskID();
	}
	
	/**
	 * @return The description of the task to which this proxy links to.
	 */
	public TaskDescription getRemoteTaskDescription()
	{
		return remoteTaskDescription;
	}
	
	/**
	 * @return The size of incoming message queue.
	 */
	public int getMessageCount()
	{
		return messages.size();
	}

}
