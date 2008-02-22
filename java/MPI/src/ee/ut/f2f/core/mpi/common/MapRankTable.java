package ee.ut.f2f.core.mpi.common;

import java.io.*;
import java.util.*;

public class MapRankTable implements Serializable {
	private static final long serialVersionUID = 1000000L;
	Vector<MapTableInfo> mapTable;

	public MapRankTable() {
		mapTable = new Vector<MapTableInfo>();
	}

	public void addMap(int rank, int rankInList) {
		MapTableInfo tableInfo = new MapTableInfo(rank, rankInList);
		mapTable.addElement(tableInfo);
	}

	public int size() {
		return mapTable.size();
	}

	public int getRank(int index) {
		MapTableInfo tmp = mapTable.elementAt(index);
		return tmp.getRank();
	}

	public int getRankInList(int index) {
		MapTableInfo tmp = mapTable.elementAt(index);
		return tmp.getRankInList();
	}

	public ArrayList<Integer> getRankInListByRank(int rank) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		MapTableInfo tmp;
		int size = size();

		for (int i = 0; i < size; i++) {
			tmp = mapTable.elementAt(i);
			if (tmp.getRank() == rank) {
				Integer element = new Integer(tmp.getRankInList());
				result.add(element);
			}
		}

		return result;
	}

	public class MapTableInfo implements Serializable {
		private static final long serialVersionUID = 1000001L;
		int rank; // Rank in Communicator
		int rankInList; // Rank in List of COMM_WORLD

		public MapTableInfo(int rank, int rankInList) {
			this.rank = rank;
			this.rankInList = rankInList;
		}

		public int getRank() {
			return rank;
		}

		public int getRankInList() {
			return rankInList;
		}
	}

}
