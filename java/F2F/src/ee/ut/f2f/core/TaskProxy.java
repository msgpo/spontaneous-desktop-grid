package ee.ut.f2f.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.F2FMessage;

/**
 * TaskProxy represents link to a remote task.
 * It is used to send/receive message to/from the 
 * remote task.
 */
public class TaskProxy
{	
	/** Recieved messages in the queue. */
	private Queue<Object> messages = new ConcurrentLinkedQueue<Object>();

	private Task task;
	/** Whose proxy is this. */
	private TaskDescription remoteTaskDescription;
	
	TaskProxy(Task task, TaskDescription remoteTaskDescription)
	{
		this.task = task;
		this.remoteTaskDescription = remoteTaskDescription;
		F2FDebug.println("\tCreated new TaskProxy of task " + remoteTaskDescription.getTaskID());
	}

	/** Sends a message to the corresponding task. 
	 * @throws CommunicationFailedException */
	public void sendMessage(Object message) throws CommunicationFailedException
	{
		if (remoteTaskDescription == null)
		{
			F2FDebug.println("\tERRRRROORRR: remoteTaskDescription == null");
			return;
		}
		F2FMessage f2fMessage = 
			new F2FMessage(
					F2FMessage.Type.MESSAGE,
					task.getJob().getJobID(),
					remoteTaskDescription.getTaskID(),
					task.getTaskID(),
					message);
		// try to send message directly to the receiver
		F2FPeer receiver = F2FComputing.peers.get(remoteTaskDescription.peerID);
		if (receiver != null)
		{
			try
			{
				receiver.sendMessage(f2fMessage);
				return;
			}
			catch (CommunicationFailedException e)
			{
				F2FDebug.println("\tcould not send a message to a tast directly, try to route via master");
			}
		}
		// could not find receiver directly -> try routing through master node
		f2fMessage.setType(F2FMessage.Type.ROUTE);
		TaskDescription masterTaskDesc = task.getTaskProxy(F2FComputing.getJob(task.getJob().getJobID()).getMasterTaskID()).getRemoteTaskDescription();
		F2FPeer master = F2FComputing.peers.get(masterTaskDesc.peerID);
		try
		{
			master.sendMessage(f2fMessage);
			return;
		}
		catch (CommunicationFailedException e)
		{
			F2FDebug.println("\tERRRORRRR!!! COULD NOT ROUTE A MESSAGE TO THE MASTER NODE!!!");
			throw e;
		}
	}

	/**
	 * Message will be saved in this proxy's message queue. The message can be
	 * aqcuired from {@link #receiveMessage}.
	 */
	void saveMessage(Object message)
	{
		F2FDebug.println("\tPROXY of " + remoteTaskDescription.getTaskID() + ": want to add a message ...");
		synchronized (messages)
		{
			this.messages.add(message);
			F2FDebug.println("\tPROXY of " + remoteTaskDescription.getTaskID() + ": added message " + message);
			messages.notify();
		}
	}
	
	/**
	 * @param timeoutInMillis custom timeout when the method will return
	 * if it has not found the message. 0 or negative value will wait until
	 * message is received.
	 * 
	 * @return message recieved from that (Proxy) task; <code>null</code> if
	 *         there is no messages after the timeout.
	 */
	public Object receiveMessage(long timeoutInMillis)
	{
		F2FDebug.println("\tPROXY of " + remoteTaskDescription.getTaskID() + ": want to read a message ...");
		synchronized (messages)
		{
			if (messages.isEmpty())
			{
				try
				{
					if (timeoutInMillis < 0) timeoutInMillis = 0;
					messages.wait(timeoutInMillis);
				}
				catch (InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
			Object message = messages.poll(); 
			F2FDebug.println("\tPROXY of " + remoteTaskDescription.getTaskID() + ": read/removed message " + message);
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
	 * @return <code>true</code> if there is message from the task;
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
	 * @return The description of the task to which this proxy belongs to.
	 */
	private TaskDescription getRemoteTaskDescription()
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
