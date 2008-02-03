/**
 * 
 */
package ee.ut.f2f.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;

class CPURequests extends Thread implements Activity
{
	private final static Logger logger = Logger.getLogger(CPURequests.class);

	/**
	 * The time how long to wait for the answers of REQUEST_FOR_CPU before
	 * throwing error that there are not enough CPUs
	 */
	private static final long REQUEST_FOR_CPUS_TIMEOUT = 10000;	
	
	private Job job;
	private Collection<F2FPeer> requestedPeers;
	/**
	 * Peers who allow their CPU to be used for F2F.
	 */
	private Collection<F2FPeer> reservedPeers;
	
	CPURequests(Job job, Collection<F2FPeer> peers)
	{
		this.job = job;
		requestedPeers = peers;
		reservedPeers = new ArrayList<F2FPeer>();	
	}

	public void run()
	{
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED, 
				"requesting " + requestedPeers.size() + " peer(s)"));
		
		F2FMessage message = 
			new F2FMessage(F2FMessage.Type.REQUEST_FOR_CPU, job.getJobID(), null, null, null);
		for (F2FPeer peer : requestedPeers)
		{
			try {
				peer.sendMessage(message);
			} catch (Exception e) {
				logger.warn("Error sending CPU request to peer" + peer.getClass(), e);
			}
		}
	
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FINISHED, 
				"requesting " + requestedPeers.size() + " peer(s)"));
	}
	
	Iterator<F2FPeer> waitForResponses(int taskCount, Collection<F2FPeer> peers) throws F2FComputingException
	{
		Collection<F2FPeer> result = new ArrayList<F2FPeer>();
		while (true)
		{
			synchronized(reservedPeers)
			{
				for (F2FPeer reservedPeer: reservedPeers)
					if (peers.contains(reservedPeer) && !result.contains(reservedPeer))
						result.add(reservedPeer);
			}
			
			if (result.size() >= taskCount) break;
			
			try
			{
				reservedPeers.wait(REQUEST_FOR_CPUS_TIMEOUT);
			}
			catch (InterruptedException e)
			{// timeout
				throw new F2FComputingException("Not enough available CPUs!");
			}
		}
		return result.iterator();
	}

	void responseReceived(F2FMessage f2fMessage, F2FPeer sender)
	{
		if((Boolean) f2fMessage.getData())
		{
			synchronized(reservedPeers)
			{
				reservedPeers.add(sender);
				reservedPeers.notifyAll();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see ee.ut.f2f.activity.Activity#getActivityName()
	 */
	public String getActivityName()
	{
		return "CPU requests for " + job.getJobID();
	}

	public Activity getParentActivity()
	{
		return job;
	}
}
