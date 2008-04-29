package ee.ut.f2f.mpi.examples.pi;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.util.F2FDebug;

public class F2FPi extends Task {

	public void runTask() {
		F2FDebug.println("Starting calculating Pi : " + getTaskID());
		Collection<F2FPeer> peers = getJob().getPeers();
		String master = getJob().getMasterTaskID();
		int rank = 0;
		int size = 0;
		double PI25DT = 3.141592653589793238462643;
		double h, sum, x;
		if (getTaskID().equals(master)) {// I am master
			size = peers.size() + 1;// working peers + master
			try {
				getJob().submitTasks(getClass().getName(), peers.size(), peers);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			int i = 0;
			Iterator<String> it = getJob().getTaskIDs().iterator();
			while (it.hasNext()) {
				String taskId = it.next();
				if (taskId.equals(getTaskID()))
					continue;
				TaskProxy proxy = getTaskProxy(taskId);
				try {
					proxy.sendMessage(Integer.valueOf(++i));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			size = getJob().getTaskIDs().size();
			Integer temp = (Integer) getTaskProxy(master).receiveMessage();
			rank = temp.intValue();
		}

		int n = 0;
		double mypi = 0;
		double pi = 0;

		if (getTaskID().equals(master)) {// I am master
			n = 1000000; // number of interval
			Iterator<String> it = getJob().getTaskIDs().iterator();
			while (it.hasNext()) {
				String taskId = it.next();
				if (taskId.equals(getTaskID()))
					continue;
				TaskProxy proxy = getTaskProxy(taskId);
				try {
					proxy.sendMessage(Integer.valueOf(n));// Send n to every peer
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			Integer temp = (Integer) getTaskProxy(master).receiveMessage();
			n = temp.intValue();
		}

		h = 1.0 / n;
		sum = 0.0;
		for (int i = rank + 1; i <= n; i += size) {
			x = h * (i - 0.5);
			sum += (4.0 / (1.0 + x * x));
		}
		mypi = h * sum;

		if (getTaskID().equals(master)) {// I am master
			pi = mypi;
			Iterator<String> it = getJob().getTaskIDs().iterator();
			while (it.hasNext()) {
				String taskId = it.next();
				if (taskId.equals(getTaskID()))
					continue;
				TaskProxy proxy = getTaskProxy(taskId);
				BigDecimal temp = (BigDecimal) proxy.receiveMessage();// Collect results
				pi += temp.doubleValue();
			}
		} else {
			TaskProxy proxy = getTaskProxy(master);
			try {
				proxy.sendMessage(BigDecimal.valueOf(mypi));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		F2FDebug.println("My part of Pi was " + mypi);
		if (getTaskID().equals(master)) {// I am master
			F2FDebug.println("Pi is approximately " + pi);
			F2FDebug.println("Error is " + (pi - PI25DT));
		}
		F2FDebug.println("Done calculating Pi");
	}
}
