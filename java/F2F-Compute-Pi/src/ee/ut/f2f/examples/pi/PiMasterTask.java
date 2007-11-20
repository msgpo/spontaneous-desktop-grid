package ee.ut.f2f.examples.pi;

import java.util.ArrayList;
import java.util.Collection;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;

public class PiMasterTask extends Task
{
	public void runTask()
	{
		long start = System.currentTimeMillis();
		// submit slave tasks
		try {
			this.getJob().submitTasks(
					"ee.ut.f2f.examples.pi.PiSlaveTask",
					this.getJob().getPeers().size(),
					this.getJob().getPeers());
		} catch (F2FComputingException e) {
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
		
		// number of milliseconds to the slaves, how long
		// they should compute points
		long intervalms = 2000L;
		// received points
		AtomicLongVector received = new AtomicLongVector( 0, 0 );
		// How many points to compute in total
		long maxpoints = 1000000000L;

		while (received.getUnSyncTotal() < maxpoints)
		{
			// Send the interval time to all slaves
			for (TaskProxy proxy: slaveProxies)
			{
				try {
						proxy.sendMessage(new Long(intervalms));
					} catch (CommunicationFailedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			// check if one of the slave tasks has found a prime
			for (TaskProxy proxy: slaveProxies)
			{
				if (proxy.hasMessage())
				{
					AtomicLongVector receivedvector = (AtomicLongVector) proxy.receiveMessage();
					received.add( receivedvector );
				}
			}
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
		System.out.println("The computed Pi is : " + received.getUnSyncTotal() * 4 / 3.0);
		System.out.println("Took " + (end - start) / 1000 + "s");
	}
}
