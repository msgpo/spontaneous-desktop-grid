package ee.ut.f2f.examples.pi;

import java.util.ArrayList;
import java.util.Collection;

import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.util.F2FDebug;

public class PiMasterTask extends Task
{
	// number of milliseconds to the slaves, how long
	// they should compute points
	Long intervalms = 2000L;
	// received points
	AtomicLongVector received = new AtomicLongVector( 0, 0 );
	// How many points to compute in total
	long maxpoints = 1000000000L;
	
	public void runTask()
	{
		long start = System.currentTimeMillis();
		// submit slave tasks
		try {
			// HACK for running PiTest
			// start 2 slaves in localhost if job is created without specifying friends
			if (this.getJob().getPeers() == null || this.getJob().getPeers().size() == 0)
			{
				Collection<F2FPeer> peer = new ArrayList<F2FPeer>();
				peer.add(F2FComputing.getLocalPeer());
				for (int i = 0; i < 2; i++)
					this.getJob().submitTasks(
							"ee.ut.f2f.examples.pi.PiSlaveTask",
							1,
							peer);
			}
			else
			{
				this.getJob().submitTasks(
					"ee.ut.f2f.examples.pi.PiSlaveTask",
					this.getJob().getPeers().size(),
					this.getJob().getPeers());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		// get IDs of all the tasks that have been created
		Collection<String> taskIDs = this.getJob().getTaskIDs();
		
		// get proxies of slave tasks
		Collection<TaskProxy> slaveProxies = new ArrayList<TaskProxy>();
		for (String taskID: taskIDs)
		{
			// do not get proxy of master task 
			if (taskID == this.getTaskID()) continue;
			TaskProxy proxy = this.getTaskProxy(taskID);
			if (proxy != null) slaveProxies.add(proxy);
		}

		// Send the interval time to all slaves
		for (TaskProxy proxy: slaveProxies)
		{
			try {
				proxy.sendMessage(intervalms);
			} catch (CommunicationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// show current result after each 10 seconds
		// the loop is exited after enough results have been received from slaves
		while (received.getUnSyncTotal() < maxpoints)
		{
			F2FDebug.println("processed " + (int)(((float)received.getUnSyncTotal() / maxpoints)*100) + "%" );
			F2FDebug.println("Pi is " + received.getUnSyncPositive() * 4.0 / received.getUnSyncTotal() );
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
		}
		
		// send stop message to slave tasks
		for (TaskProxy proxy: slaveProxies)
		{
			try {
				proxy.sendMessage(new Long(0L));
			} catch (CommunicationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		long end = System.currentTimeMillis();

		// show the result
		F2FDebug.println("The computed Pi is : " + received.getUnSyncPositive() * 4.0 / received.getUnSyncTotal());
		F2FDebug.println("Took " + (end - start) / 1000 + "s");
		System.out.println("The computed Pi is : " + 
				received.getUnSyncPositive() * 4.0 / received.getUnSyncTotal());
		System.out.println("Took " + (end - start) / 1000 + "s");
	}
	
	// collect results
	public void messageReceived(String remoteTaskID)
	{
		// do not process the message if the required amount of 
		// points have been calculated already
		if (received.getUnSyncTotal() >= maxpoints) return;
		
		// process the numbers from the slave task
		TaskProxy proxy = this.getTaskProxy(remoteTaskID);
		if (!proxy.hasMessage()) return;
		AtomicLongVector receivedvector = (AtomicLongVector) proxy.receiveMessage();
		synchronized (this)
		{
			if (received.getUnSyncTotal() >= maxpoints) return;
			received.add( receivedvector );
			
			F2FDebug.println("Received from task "+remoteTaskID+": total " 
					+ receivedvector.getUnSyncTotal()
					+ " positives " 
					+ receivedvector.getUnSyncPositive());
			F2FDebug.println("Sum is now: total " 
					+ received.getUnSyncTotal()
					+ " positives " 
					+ received.getUnSyncPositive());
			
			// check if the job is finished
			if (received.getUnSyncTotal() >= maxpoints)
				this.interrupt();
		}
	}
}
