package ee.ut.f2f.core;

import java.util.Collection;

import ee.ut.f2f.util.F2FDebug;

/**
 * This is a proxy to/from multiple tasks. 
 * It can be used to ease sending/reading a message to/from multiple tasks.
 */
public class MultiTaskProxy //extends TaskProxy
{
	//private static final Logger LOG = LogManager.getLogger(MultiTaskProxy.class);
	
	/**
	 * The default polling timout.
	 */
	private static final int DEFAULT_POLLING_TIMEOUT = 100;
	
	/**
	 * Actual proxies of tasks.
	 */
	private Collection<TaskProxy> taskProxies;
	
	/**
	 * The time in millis after what another task proxy is checked against the
	 * message.
	 */
	private long pollingTimeInMillis = DEFAULT_POLLING_TIMEOUT; // Default polling time is 0.1 sek 
	
	/**
	 * Timout will be default {@link #DEFAULT_POLLING_TIMEOUT}.
	 * 
	 * @param taskProxies the proxies to monitor.
	 * @param pollingTimeInMillis time to wait while another task proxy is checked against the messages.
	 */
	public MultiTaskProxy(Collection<TaskProxy> taskProxies)
	{
		this(taskProxies, 0);
	}
	
	/**
	 * @param taskProxies The proxies to monitor.
	 * @param pollingTimeInMillis Time to wait while another task proxy is checked against the messages.
	 */
	MultiTaskProxy(Collection<TaskProxy> taskProxies, long pollingTimeInMillis)
	{
		//super(null, null);
		if(taskProxies != null) this.taskProxies = taskProxies; 
		else F2FDebug.println("\tWARNING: no TaskProxies given to MultiTaskProxy constructor!");
		
		if (pollingTimeInMillis > 0) this.pollingTimeInMillis = pollingTimeInMillis;
	}

	/**
	 * Sends a message through the tisted proxies.
	 * 
	 * @param message The message to be sent.
	 */
	public void sendMessage(Object message)
	{
		for(TaskProxy taskProxy : taskProxies)
		{
			taskProxy.sendMessage(message);
		}
	}
	
	/**
	 * Polls all the task proxies in this multiproxy for a message until first 
	 * message is found, which then is returned. The proxy will not be removed
	 * from the polling list. 
	 * Blocks until at least one message is returned from the listed proxies.  
	 * 
	 * @return <code>null</code> if no proxies in the list
	 */
	public Object receiveMessage()
	{
		return receiveMessage((TaskProxy)null);
	}

	/**
	 * Polls all the task proxies in this multiproxy for a message until first 
	 * message is found, which then is returned. The proxy will not be removed
	 * from the polling list. 
	 * Blocks until at least one message is returned from the listed proxies.  
	 * 
	 * @param sender Will be set to source proxy which received the message.
	 * @return message First message sent to any listed task proxy.
	 */
	public Object receiveMessage(TaskProxy sender)
	{
		return receiveMessage(sender, false);
	}
	
	/**
	 * Polls all the task proxies in this multiproxy for a message until first 
	 * message is found, which then is returned. 
	 * Blocks until at least one message is returned from the listed proxies.  
	 * 
	 * @param sender Will be set to the proxy which received the message.
	 * @param removeProxyAfterMessage if <code>true</code> the proxy that recieved
	 * the message will be removed.
	 * @return message first message sent to any task proxy.
	 */
	public synchronized Object receiveMessage(TaskProxy sender, boolean removeProxyAfterMessage)
	{
		if (taskProxies==null || taskProxies.size()==0) return null;
		
		while (true)
		{
			for (TaskProxy proxy : taskProxies )
			{
				// Get the message with timeout.
				if (proxy.hasMessage())
				{
					if (removeProxyAfterMessage) taskProxies.remove(proxy);
					sender = proxy;
					return proxy.receiveMessage();
				}
			}
			// Wait before polling again.
			try { wait(pollingTimeInMillis); }
			catch (InterruptedException e) { /*continue*/ }
		}
	}
	
	/**
	 * @return <code>true</code> if there is message from the task;
	 *         <code>false</code> otherwise.
	 */
	public boolean hasMessage()
	{
		for (TaskProxy tp : taskProxies)
		{
			if (tp.hasMessage()) return true;
		}
		return false;
	}
}
