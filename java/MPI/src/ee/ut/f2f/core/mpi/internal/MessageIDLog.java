package ee.ut.f2f.core.mpi.internal;

import java.util.*;

public class MessageIDLog {
	ArrayList<String> log;

	public MessageIDLog() {
		log = new ArrayList<String>();
	}

	public void add(String mid) {
		log.add(mid);
	}

	// return -1 is not exist
	// if not return index to mid
	public int isExist(String mid) {
		int numLog = log.size();
		String tmpID;

		for (int i = 0; i < numLog; i++) {
			tmpID = log.get(i);
			if (tmpID.equals(mid)) {
				return i;
			}
		}
		return -1;
	}

	public void remove(String mid) {
		int numLog = log.size();
		String tmpID;

		for (int i = 0; i < numLog; i++) {
			tmpID = log.get(i);
			if (tmpID.equals(mid)) {
				log.remove(i);
				break;
			}
		}
	}

	public void printInfo() {
		int numLog = log.size();
		for (int i = 0; i < numLog; i++)
			System.out.println("MID = " + log.get(i));
	}
}
