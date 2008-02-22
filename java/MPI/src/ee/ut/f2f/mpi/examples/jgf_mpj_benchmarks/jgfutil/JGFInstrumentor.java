/**************************************************************************
 *                                                                         *
 *         Java Grande Forum Benchmark Suite - MPJ Version 1.0             *
 *                                                                         *
 *                            produced by                                  *
 *                                                                         *
 *                  Java Grande Benchmarking Project                       *
 *                                                                         *
 *                                at                                       *
 *                                                                         *
 *                Edinburgh Parallel Computing Centre                      *
 *                                                                         * 
 *                email: epcc-javagrande@epcc.ed.ac.uk                     *
 *                                                                         *
 *                                                                         *
 *      This version copyright (c) The University of Edinburgh, 2001.      *
 *                         All rights reserved.                            *
 *                                                                         *
 **************************************************************************/

package ee.ut.f2f.mpi.examples.jgf_mpj_benchmarks.jgfutil;

import java.util.Hashtable;

import ee.ut.f2f.core.mpi.MPITask;

public class JGFInstrumentor {
	MPITask task = null;
	private Hashtable<String, JGFTimer> timers = new Hashtable<String, JGFTimer>();
	private Hashtable<String, JGFTimer> data = new Hashtable<String, JGFTimer>();

	public JGFInstrumentor(MPITask task) {
		this.task = task;
	}

	public synchronized void addTimer(String name) {

		if (timers.containsKey(name)) {
			task.getMPIDebug().println("JGFInstrumentor.addTimer: warning -  timer " + name + " already exists");
		} else {
			timers.put(name, new JGFTimer(task, name));
		}
	}

	public synchronized void addTimer(String name, String opname) {

		if (timers.containsKey(name)) {
			task.getMPIDebug().println("JGFInstrumentor.addTimer: warning -  timer " + name + " already exists");
		} else {
			timers.put(name, new JGFTimer(task, name, opname));
		}

	}

	public synchronized void addTimer(String name, String opname, int size) {

		if (timers.containsKey(name)) {
			task.getMPIDebug().println("JGFInstrumentor.addTimer: warning -  timer " + name + " already exists");
		} else {
			timers.put(name, new JGFTimer(task, name, opname, size));
		}

	}

	public synchronized void startTimer(String name) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).start();
		} else {
			task.getMPIDebug().println("JGFInstrumentor.startTimer: failed -  timer " + name + " does not exist");
		}

	}

	public synchronized void stopTimer(String name) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).stop();
		} else {
			task.getMPIDebug().println("JGFInstrumentor.stopTimer: failed -  timer " + name + " does not exist");
		}
	}

	public synchronized void addOpsToTimer(String name, double count) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).addops(count);
		} else {
			task.getMPIDebug().println("JGFInstrumentor.addOpsToTimer: failed -  timer " + name + " does not exist");
		}
	}

	public synchronized void addTimeToTimer(String name, double added_time) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).addtime(added_time);
		} else {
			task.getMPIDebug().println("JGFInstrumentor.addTimeToTimer: failed -  timer " + name + " does not exist");
		}

	}

	public synchronized double readTimer(String name) {
		double time;
		if (timers.containsKey(name)) {
			time = ((JGFTimer) timers.get(name)).time;
		} else {
			task.getMPIDebug().println("JGFInstrumentor.readTimer: failed -  timer " + name + " does not exist");
			time = 0.0;
		}
		return time;
	}

	public synchronized void resetTimer(String name) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).reset();
		} else {
			task.getMPIDebug().println("JGFInstrumentor.resetTimer: failed -  timer " + name + " does not exist");
		}
	}

	public synchronized void printTimer(String name) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).print();
		} else {
			task.getMPIDebug().println("JGFInstrumentor.printTimer: failed -  timer " + name + " does not exist");
		}
	}

	public synchronized void printperfTimer(String name) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).printperf();
		} else {
			task.getMPIDebug().println("JGFInstrumentor.printTimer: failed -  timer " + name + " does not exist");
		}
	}

	public synchronized void printperfTimer(String name, int arr_size) {
		if (timers.containsKey(name)) {
			((JGFTimer) timers.get(name)).printperf(arr_size);
		} else {
			task.getMPIDebug().println("JGFInstrumentor.printTimer: failed -  timer " + name + " does not exist");
		}
	}

	public synchronized void retrieveData(String name, Object obj) {
		obj = data.get(name);
	}

	public synchronized void printHeader(int section, int size, int nprocess) {

		String header, base;

		header = "";
		base = "Java Grande Forum MPJ Benchmark Suite - Version 1.0 - Section ";

		switch (section) {
		case 1:
			header = base + "1";
			break;
		case 2:
			switch (size) {
			case 0:
				header = base + "2 - Size A";
				break;
			case 1:
				header = base + "2 - Size B";
				break;
			case 2:
				header = base + "2 - Size C";
				break;
			}
			break;
		case 3:
			switch (size) {
			case 0:
				header = base + "3 - Size A";
				break;
			case 1:
				header = base + "3 - Size B";
				break;
			}
			break;
		}

		task.getMPIDebug().println(header);

		if (nprocess == 1) {
			task.getMPIDebug().println("Executing on " + nprocess + " process");
		} else {
			task.getMPIDebug().println("Executing on " + nprocess + " processes");
		}

		task.getMPIDebug().println("");

	}

}
