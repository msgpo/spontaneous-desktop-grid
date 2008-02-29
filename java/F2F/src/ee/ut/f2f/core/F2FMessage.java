package ee.ut.f2f.core;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import ee.ut.f2f.util.logging.Logger;

class F2FMessage implements Serializable
{
	private static final long serialVersionUID = 3696207524297521656L;
	private static final Logger logger = Logger.getLogger(F2FMessage.class);
	
	/**
	 * Defines the nature of a message.
	 */
	public enum Type
	{
		/**
		 * Master peer asks a peer for CPU.
		 */
		REQUEST_FOR_CPU,
		/**
		 * Peer answers to REQUEST_FOR_CPU.
		 */
		RESPONSE_FOR_CPU,
		/**
		 * Master sends job with task descriptions to a peer.
		 */
		JOB,
		/**
		 * Master sends job with task descriptions a peer.
		 * The TASK message without task descriptions follows it.
		 * The job has to be sent first because otherwise
		 * custom classes in the task can not be deserialized.
		 */
		JOB_TASK,
		/**
		 * Master sends task descriptions and a prepared task to a peer.
		 */
		TASK,
		/**
		 * Master sends task descriptios to a peer.
		 */
		TASK_DESCRIPTIONS,
		/**
		 * A message from one task to another.
		 */
		MESSAGE,
		/**
		 * A message from one task to another that has to be routed. Only
		 * master peers receive such messages and forward them to final
		 * destination nodes.
		 */
		ROUTE,
		ROUTE_BLOCKING,
		ROUTE_REPORT
	}

	public F2FMessage(Type type, String jobID, String receiverTaskID,
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
	public Type getType() { return type; }
	public void setType(Type type) { this.type = type; }

	/**
	 * The ID of the job which tasks are communicating using the message.
	 */
	private String jobID;
	public String getJobID() { return jobID; }

	/**
	 * The ID of the receiver task.
	 */
	private String receiverTaskID;
	public String getReceiverTaskID() { return receiverTaskID; }

	/**
	 * The ID of the sender task.
	 */
	private String senderTaskID;
	public String getSenderTaskID() { return senderTaskID; }

	/**
	 * The data object that is being sent.
	 */
	private Object data;
	public Object getData() { return data; }
	public void setData(Object data) { this.data = data; }

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
		boolean customOIPresent = stream instanceof JobCustomObjectInputStream;
		if (!customOIPresent)
			logger.warn("CustomObjectInputStream IS NOT BEING USED! Cannot cast custom objects!!!");

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
					((JobCustomObjectInputStream) stream).readObject(jobID)
					: stream.readObject();
			//F2FDebug.println("\tDezerialised F2FMessage data is: " + data);
		}
		catch (ClassNotFoundException e)
		{
			logger.error("Error deserializing F2FMessage data", e);
		}
	}
}

class F2FTaskMessage implements Serializable
{
	private static final long serialVersionUID = -1956956326946440671L;

	private Collection<TaskDescription> newTaskDescriptions;
	Collection<TaskDescription> getTaskDescriptions() { return newTaskDescriptions; }
	
	private Task task;
	Task getTask() { return task; }
	
	F2FTaskMessage(Collection<TaskDescription> newTaskDescriptions, Task task)
	{
		this.newTaskDescriptions = newTaskDescriptions;
		this.task = task;
	}
}