package ee.ut.f2f.core.mpi.common;

import java.io.Serializable;
import java.util.ArrayList;

public class RankTable implements Serializable {
	private static final long serialVersionUID = 1000004L;

	ArrayList<TableInfo> table;

	public RankTable() {
		table = new ArrayList<TableInfo>();
	}

	public void addProcess(int rank, String taskID) {
		TableInfo tableInfo = new TableInfo(taskID, rank, true);
		table.add(tableInfo);
	}

	public int size() {
		return table.size();
	}

	public int getRank(int index) {
		TableInfo tmp = table.get(index);
		return tmp.getRank();
	}

	public boolean isAlive(int index) {
		TableInfo tmp = table.get(index);
		return tmp.isAlive();
	}

	public String getTaskID(int index) {
		TableInfo tmp = table.get(index);
		return tmp.getTaskID();
	}

	public void setAlive(int index, boolean alive) {
		TableInfo tmp = table.get(index);
		tmp.setAlive(alive);
	}

	// Return only alive process
	public String[] getTaskIDs() {
		int size = size();
		String[] result = new String[size];
		TableInfo tmp;
		for (int i = 0; i < size; i++) {
			tmp = table.get(i);
			try {
				result[i] = tmp.getTaskID();
			} catch (Exception e) {
			}
		}
		return result;
	}

	public ArrayList<Integer> getRankInListByRank(int rank) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		TableInfo tmp;
		for (int i = 0; i < size(); i++) {
			tmp = table.get(i);
			if (tmp.getRank() == rank) {
				if (tmp.isAlive()) {
					Integer element = new Integer(i);
					result.add(element);
				}
			}
		}
		return result;
	}

	public ArrayList<String> getTaskIDsByRank(int rank) {
		ArrayList<String> result = new ArrayList<String>();
		TableInfo tmp;
		for (int i = 0; i < size(); i++) {
			tmp = table.get(i);
			if (tmp.getRank() == rank) {
				if (tmp.isAlive()) {
					result.add(tmp.getTaskID());
				}
			}
		}
		return result;
	}

	public int getIndexByTaskID(String taskID) {
		for (int i = 0; i < table.size(); i++) {
			if (getTaskID(i).equals(taskID)) {
				return i;
			}
		}
		return -1;
	}

	public class TableInfo implements Serializable {
		private static final long serialVersionUID = 1000006L;
		String taskID;
		int rank;
		boolean alive;

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
}
