package ee.ut.f2f.examples.pi;

import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.util.F2FDebug;

import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;

public class PiSlaveTask extends Task
{
	AtomicLongVector computedPoints = new AtomicLongVector (0,0);
	// bad random Random random = new Random(System.currentTimeMillis());
	MersenneTwisterRNG random = new MersenneTwisterRNG();
	
	public void runTask()
	{
		// get proxy of master task
		TaskProxy masterProxy = this.getTaskProxy(this.getJob().getMasterTaskID());
		if (masterProxy == null) throw new RuntimeException("Proxy of master task was not found!");
		
		// get interval time
		long intervalms = ( (Long)masterProxy.receiveMessage() ).longValue();
		
		F2FDebug.println("Received intervalms.");

		// Start a thread which computes the points
		ComputePoints compTask = new ComputePoints ();
		compTask.start();
		
		// Send the computed points back to the master
		while (true)
		{
			// Wait
			Long stopcondition = (Long) masterProxy.receiveMessage(intervalms);
			if (isStopped()) break;
			// Test if stopcondition was sent
			if ( stopcondition != null )
				if ( stopcondition.longValue() == 0 )
					break;
				
			F2FDebug.println("Stop-condition not met, sending back results. total: "
					+ computedPoints.getUnSyncTotal() + " positives: " 
					+ computedPoints.getUnSyncPositive() );
			// send results back to master
			try {
				masterProxy.sendMessage( computedPoints );
				// reset computiation thread
				computedPoints.set(0, 0);
			} catch (CommunicationFailedException e) {
				e.printStackTrace();
			}
		}
		
		// stop the thread
		stopTask();
	}

	private class ComputePoints extends Thread
	{
		public void run() {
			while(!isStopped())
			{
				double x = random.nextDouble();
				double y = random.nextDouble();
				// check if the point in the quarter-cicle with radius 1
				if( x*x + y*y < 1.0 )
					computedPoints.positiveHit();
				else
					computedPoints.negativeHit();
			}
		}
	}
}
