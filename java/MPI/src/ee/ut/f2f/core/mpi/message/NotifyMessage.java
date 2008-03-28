package ee.ut.f2f.core.mpi.message;

import java.io.Serializable;

public class NotifyMessage extends BasicMessage implements Serializable{
	private static final long serialVersionUID = 2000022L;
	String deadTaskID;

	public NotifyMessage(String deadTaskID) {
		this.deadTaskID = deadTaskID;
	}

	public String getDeadTaskID() {
		return deadTaskID;
	}

	public void setDeadTaskID(String deadTaskID) {
		this.deadTaskID = deadTaskID;
	}
}
