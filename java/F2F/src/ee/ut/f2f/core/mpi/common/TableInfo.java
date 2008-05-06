package ee.ut.f2f.core.mpi.common;

import java.io.Serializable;

public class TableInfo implements Serializable {
	private static final long serialVersionUID = 4002006L;
	private int rank;
	private boolean alive;
	private String taskID = null;

	public TableInfo(String taskID, int rank, boolean alive) {
		this.taskID = taskID;
		this.rank = rank;
		this.alive = alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public boolean isAlive() {
		return alive;
	}

	public int getRank() {
		return rank;
	}

	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

}
