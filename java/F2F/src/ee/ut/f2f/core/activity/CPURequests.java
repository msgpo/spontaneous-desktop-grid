/**
 * 
 */
package ee.ut.f2f.core.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.F2FMessage;

/**
 * @author olegus
 *
 */
public class CPURequests implements Activity {

	/**
	 * The time how long to wait for the answers of REQUEST_FOR_CPU before
	 * throwing error that there are not enough CPUs
	 */
	private static final long REQUEST_FOR_CPUS_TIMEOUT = 10000;	
	
	private String jobID;
	private Collection<F2FPeer> requestedPeers;
	/**
	 * Peers who allow their CPU to be used for F2F.
	 * JobID -> Peer
	 */
	private Map<String, Collection<F2FPeer>> reservedPeers;

	/**
	 * Desired number of new tasks.
	 */
	private int taskCount;
	
	public CPURequests(String jobID, Collection<F2FPeer> peers, int taskCount) {
		this.jobID = jobID;
		this.taskCount = taskCount;
		requestedPeers = peers;
		reservedPeers = new HashMap<String, Collection<F2FPeer>>();		
	}
	
	/* (non-Javadoc)
	 * @see ee.ut.f2f.activity.Activity#getActivityName()
	 */
	public String getActivityName() {
		return jobID+" CPU requests for "+taskCount+" task(s)";
	}

	public void makeRequests() {
		reservedPeers.put(jobID, new ArrayList<F2FPeer>());
		F2FDebug.println("\tSending REQUEST_FOR_CPU to: " + requestedPeers + ".");
		
		ActivityEvent event = new ActivityEvent(this, ActivityEvent.Type.STARTED);
		event.setDescription("Making requests to " + requestedPeers.size() + " peer(s)");
		ActivityManager.getDefaultActivityManager().emitEvent(event);
		
		F2FMessage message = 
			new F2FMessage(F2FMessage.Type.REQUEST_FOR_CPU, jobID, null, null, null);
		for (F2FPeer peer : requestedPeers)
		{
			try {
				peer.sendMessage(message);
			} catch (Exception e) {
				F2FDebug.println("" + e);
			}
		}
	}
	
	public void waitForResponses() throws F2FComputingException {
		
		ActivityEvent event = new ActivityEvent(this, ActivityEvent.Type.CHANGED);
		event.setDescription("Waiting for responses");
		ActivityManager.getDefaultActivityManager().emitEvent(event);		
		
		while (true)
		{
			if (reservedPeers.get(jobID).size() >= taskCount) break;
			try
			{
				synchronized(reservedPeers.get(jobID))
				{
					reservedPeers.get(jobID).wait(REQUEST_FOR_CPUS_TIMEOUT);
				}
			}
			catch (InterruptedException e)
			{// timeout
				reservedPeers.remove(jobID);
				throw new F2FComputingException("Not enough available CPUs!");
			}
		}
	}

	public Map<String, Collection<F2FPeer>> getReservedPeers() {
		return reservedPeers;
	}

	public Collection<F2FPeer> getRequestedPeers() {
		return requestedPeers;
	}

	public void responseReceived(F2FMessage f2fMessage, F2FPeer sender) {
		if ((Boolean)f2fMessage.getData() &&
				reservedPeers.get(f2fMessage.getJobID()) != null)
			{
				synchronized(reservedPeers.get(f2fMessage.getJobID()))
				{
					reservedPeers.get(f2fMessage.getJobID()).add(sender);
					reservedPeers.get(f2fMessage.getJobID()).notifyAll();
				}
			}		
	}
}
