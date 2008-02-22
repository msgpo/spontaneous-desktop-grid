package ee.ut.f2f.core.mpi.message;

import ee.ut.f2f.core.mpi.common.RankTable;

public class MPIMessage extends BasicMessage {
	private static final long serialVersionUID = 2000020L;
	private int cmd;
	private String taskID;
	private int rank;
	private int rankInList;
	private int size;
	private RankTable comm;
	private int t_gossip;
	private int t_margin;
	private int t_hang;

	public MPIMessage(int cmd) {
		this.cmd = cmd;
	}

	public void setTHang(int t_hang) {
		this.t_hang = t_hang;
	}

	public int getTHang() {
		return t_hang;
	}

	public void setTGossip(int t_gossip) {
		this.t_gossip = t_gossip;
	}

	public void setTMargin(int t_margin) {
		this.t_margin = t_margin;
	}

	public int getTGossip() {
		return t_gossip;
	}

	public int getTMargin() {
		return t_margin;
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
}
