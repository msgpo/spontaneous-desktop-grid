package ee.ut.f2f.examples.pi;

import java.util.ArrayList;
import java.util.Collection;

import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.util.F2FDebug;

public class PiMaster2 extends Task
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
		
		// prepare the slave tasks
		Collection<Task> slaveTasks = new ArrayList<Task>();
		for (int i = 0; i < this.getJob().getPeers().size(); i++)
		{
			PiSlave2 task = new PiSlave2();
			task.setInterval(2000);
			slaveTasks.add(task);
		}
		
		// submit slave tasks
		try {
			// HACK for running PiTest
			// start 2 slaves in localhost if job is created without specifying friends
			if (this.getJob().getPeers() == null || this.getJob().getPeers().size() == 0)
			{
				Collection<F2FPeer> peer = new ArrayList<F2FPeer>();
				peer.add(F2FComputing.getLocalPeer());
				Collection<Task> task = new ArrayList<Task>();
				task.add(slaveTasks.iterator().next());
				for (int i = 0; i < 2; i++)
					this.getJob().submitTasks(
							task,
							peer);
			}
			else
			{
				this.getJob().submitTasks(
					slaveTasks,
					this.getJob().getPeers());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
				
		// show current result after each 10 seconds
		// the loop is exited after enough results have been received from slaves
		while (!bStopFlag && received.getUnSyncTotal() < maxpoints)
		{
			F2FDebug.println("processed " + (int)(((float)received.getUnSyncTotal() / maxpoints)*100) + "%" );
			F2FDebug.println("Pi is " + received.getUnSyncPositive() * 4.0 / received.getUnSyncTotal() );
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
		}
		
		// send stop message to slave tasks
		for (String taskID: this.getJob().getTaskIDs())
		{
			if (taskID == this.getTaskID()) continue;
			TaskProxy proxy = this.getTaskProxy(taskID);
			if (proxy == null) continue;
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
	public void messageReceivedEvent(String remoteTaskID)
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
			
			setProgress((int)(((float)received.getUnSyncTotal() / maxpoints)*100));
			// check if the job is finished
			if (received.getUnSyncTotal() >= maxpoints)
				this.interrupt();
		}
	}
	
	// kill the job
	protected void taskStoppedEvent()
	{
		this.interrupt();
	}
}
