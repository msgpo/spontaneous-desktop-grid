package ee.ut.f2f.core;

/**
 * This exception is thrown if new tasks are tried to submit to peers
 * that were not listed/selected when the job was created. This mean that
 * if method submitTasks() is called only the same collection (or its 
 * sub-collection) of peers may be given as argument that was used when
 * the job was created. 
 */
@SuppressWarnings("serial")
public class NotJobPeerException extends F2FComputingException
{
	NotJobPeerException(F2FPeer peer, Job job)
	{
		super("Peer "+peer.getDisplayName()+" was not listed/selected to take part in the job " + job.getJobID());
	}
}
