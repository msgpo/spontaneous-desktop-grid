package ee.ut.f2f.core.mpi.message;

import java.io.Serializable;
import java.lang.reflect.Method;

import ee.ut.f2f.core.mpi.common.RankTable;

public class MPIMessage implements Serializable {
	private static final long serialVersionUID = 2090120L;
	private RankTable comm = null;
	private String taskID = null;
	private int cmd;
	private int rank;
	private int rankInList;
	private int size;

	public MPIMessage(int cmd) {
		this.cmd = cmd;
	}

	public int getCmd() {
		return cmd;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public void setRankInList(int rankInList) {
		this.rankInList = rankInList;
	}

	public int getRankInList() {
		return rankInList;
	}

	public int getRank() {
		return rank;
	}

	public void setCommTable(RankTable comm) {
		this.comm = comm;
	}

	public RankTable getCommTable() {
		return comm;
	}

	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

	public String toString() {
		StringBuffer content = new StringBuffer(getClass().getName());
		Method[] metodo = this.getClass().getDeclaredMethods();
		for (int i = 0; i < metodo.length; i++) {
			if (!metodo[i].getName().equals("clone") && !metodo[i].getName().equals("toString") && metodo[i].getParameterTypes().length == 0) {
				try {
					content.append(" ").append(metodo[i].getName()).append("=").append(metodo[i].invoke(this, new Object[] {}));
				} catch (Exception e) {
				}
			}
		}
		return content.toString();
	}
}
