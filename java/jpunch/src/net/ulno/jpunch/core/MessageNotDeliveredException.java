package net.ulno.jpunch.core;
/**
 * This exception is thrown if the sendMessageBlocking() does not 
 * receive the acknowledgement from the receiver in the given time.
 * This doesn't definitely mean that the message was not received.  
 */
@SuppressWarnings("serial")
public class MessageNotDeliveredException extends CommunicationFailedException
{
	public MessageNotDeliveredException(Object message)
	{
		super("A message was not delivered to the destination in time: " + message.toString());
	}
}
