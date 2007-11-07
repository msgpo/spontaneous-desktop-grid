package ee.ut.f2f.util;

import java.io.IOException;
import java.io.Serializable;


@SuppressWarnings("serial")
public class F2FMessage implements Serializable
{
	/**
	 * Defines the nature of a message.
	 */
	public enum Type
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
		ROUTE,
		/**
		 * A chat message from one user to another.
		 */
		CHAT,
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