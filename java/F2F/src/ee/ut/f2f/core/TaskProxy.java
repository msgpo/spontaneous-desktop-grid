package ee.ut.f2f.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.core.F2FComputing.F2FMessage;
import ee.ut.f2f.util.F2FDebug;

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

	/** Sends a message to the corresponding task. */
	public void sendMessage(Object message)
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
		for (CommunicationLayer commLayer: F2FComputing.getCommunicationLayers())
		{
			if (remoteTaskDescription.mapComm2Peer.containsKey(commLayer.getID()))
			{
				try
				{
					// if receiver task is in local peer send it directly
					if (remoteTaskDescription.mapComm2Peer.get(commLayer.getID())
						.equals(commLayer.getLocalPeer().getID()))
					{
						F2FComputing.workHandler.messageRecieved(f2fMessage, commLayer.getLocalPeer());
						return;
					}
					Peer peer = commLayer.findPeerByID(remoteTaskDescription.mapComm2Peer.get(commLayer.getID()));
					if (peer == null) continue;
					peer.sendMessage(f2fMessage);
					//F2FDebug.println("\t(last message was sent directly to the receiver node)");
					return;
				}
				catch (CommunicationFailedException e)
				{
					F2FDebug.println("\tSending message ("+message+") to the peer ("+remoteTaskDescription.mapComm2Peer.get(commLayer.getID())+") directly failed. Try to route through master. " + e);
				}
			}
		}
		// could not find receiver directly -> try routing through master node
		F2FDebug.println("\tmessage has to be ROUTED through MASTER");
		f2fMessage.setType(F2FMessage.Type.ROUTE);
		TaskDescription masterTaskDesc = task.getTaskProxy(F2FComputing.getJob(task.getJob().getJobID()).getMasterTaskID()).getRemoteTaskDescription();
		for (CommunicationLayer commLayer: F2FComputing.getCommunicationLayers())
		{
			if (masterTaskDesc.mapComm2Peer.containsKey(commLayer.getID()))
			{
				try
				{
					Peer peer = commLayer.findPeerByID(masterTaskDesc.mapComm2Peer.get(commLayer.getID()));
					if (peer == null) continue;
					peer.sendMessage(f2fMessage);
					F2FDebug.println("\tmessage was sent to MASTER for ROUTING");
					return;
				}
				catch (CommunicationFailedException e)
				{
					F2FDebug.println("\tSending message ("+message+") to the peer ("+remoteTaskDescription.mapComm2Peer.get(commLayer.getID())+") failed. " + e);
				}
			}
		}
		// todo: throw an exception
		F2FDebug.println("\tERRRORRRR!!! COULD NOT ROUTE MESSAGE TO MASTER NODE!!!");
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
