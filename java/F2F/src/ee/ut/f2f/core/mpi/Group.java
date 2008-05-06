package ee.ut.f2f.core.mpi;

import java.util.ArrayList;

import ee.ut.f2f.core.mpi.common.MapRankTable;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.internal.MessageHandler;
import ee.ut.f2f.core.mpi.internal.MessageIDLog;
import ee.ut.f2f.core.mpi.internal.SendBufferInformation;

/**
 * Group class
 */
public class Group {
	MessageHandler msgHandle;
	RankTable rankTable;
	MapRankTable mapRankTable;
	int myRank;
	int myRankInList;
	int groupSize;

	ArrayList<SendBufferInformation> backupMessage;
	MessageIDLog log;

	// Create by MPI_INIT (COMM_WORLD)
	/**
	 * Internal constructor
	 * 
	 * @param msgHandle
	 *            message handle
	 * @param rankTable
	 *            list of nodes
	 * @param myRank
	 *            MPI rank
	 * @param myRankInList
	 *            Rank in list
	 * @param groupSize
	 *            number of MPI ranks
	 * @param backupMessage
	 *            a backup message
	 * @param log
	 *            messageID log
	 */

	public Group(MessageHandler msgHandle, RankTable rankTable, int myRank, int myRankInList, int groupSize, ArrayList<SendBufferInformation> backupMessage, MessageIDLog log, MapRankTable mapRankTable) {
		this.msgHandle = msgHandle;
		this.rankTable = rankTable;
		this.myRank = myRank;
		this.myRankInList = myRankInList;
		this.groupSize = groupSize;
		this.backupMessage = backupMessage;
		this.log = log;
		this.mapRankTable = mapRankTable;
	}

	// //////////////////
	// Public call
	// ///////////////////
	/**
	 * Returns rank of process in group
	 * 
	 * @return process rank in group
	 */
	public int Rank() {
		return myRank;
	}

	/**
	 * Returns rank of process in list table (Internal used)
	 * 
	 * @return process rank in rank table
	 */
	public int RankInList() {
		return myRankInList;
	}

	// Group size
	/**
	 * Returns size of group
	 * 
	 * @return group size
	 */
	public int Size() {
		return groupSize;
	}

	/**
	 * Internal use
	 */
	public int __sizetotal() {
		int size = 0;
		int numPeer = __getNumPeers();
		synchronized (mapRankTable) {
			for (int i = 0; i < numPeer; i++) {
				if (rankTable.isAlive(mapRankTable.getRank(i)))
					size++;
			}
		}
		return size;
	}

	// Create a new group which exclude some ranks from original group
	/**
	 * Create a new group which exclude some ranks from original group
	 * 
	 * @param rank
	 *            list of excluded rank
	 * @return a new group
	 */
	// TODO: No error checking if rank is redundant
	public Group Excl(int[] rank) {
		int newGroupSize = groupSize;
		int numPeers = __getNumPeers();
		MapRankTable newMapRankTable = new MapRankTable();

		int counter;
		boolean isExclude;
		int myNewRank = MPI.UNDEFINED;
		int myNewRankInList = MPI.UNDEFINED;
		int numElement = 0;
		for (int i = 0; i < numPeers; i++) {
			int r = mapRankTable.getRank(i);
			counter = 0;
			isExclude = false;
			for (int j = 0; j < rank.length; j++) {
				if (rank[j] < r) {
					// Counter for assigning a new rank
					counter++;
				} else if (rank[j] == r) {
					// This rank is excluded
					isExclude = true;
					break;
				}
			}
			if (!isExclude) {
				// Create new rank mapping but rank in list is still the same
				newMapRankTable.addMap(r - counter, mapRankTable.getRankInList(i));
				// Calcalate new rank and rankInList value
				if (i == myRankInList) {
					myNewRank = r - counter;
					myNewRankInList = numElement;
				}
				numElement++;
			}
		}
		newGroupSize -= rank.length;
		Group newGroup = new Group(msgHandle, rankTable, myNewRank, myNewRankInList, newGroupSize, backupMessage, log, newMapRankTable);
		return newGroup;
	}

	/**
	 * Create a new group which include some ranks from original group
	 * 
	 * @param rank
	 *            list of included rank
	 * @return a new group
	 */
	public Group Incl(int[] rank) {
		int counter = 0;
		int listcounter = 0;
		// int numPeers = __getNumPeers();
		MapRankTable newMapRankTable = new MapRankTable();
		int myNewRank = MPI.UNDEFINED;
		int myNewRankInList = MPI.UNDEFINED;

		int numPeers = mapRankTable.size();
		boolean rankAdded;
		for (int i = 0; i < rank.length; i++) {
			rankAdded = false;
			for (int j = 0; j < numPeers; j++) {
				if (mapRankTable.getRank(j) == rank[i]) {
					newMapRankTable.addMap(counter, mapRankTable.getRankInList(j));

					if (j == myRankInList) {
						myNewRank = counter;
						myNewRankInList = listcounter;
					}
					rankAdded = true;
					listcounter++;
				}
			}
			if (rankAdded) {
				counter++;
			}
		}
		int newGroupSize = counter;
		/*
		 * for(int i = 0; i < numPeers; i++) { int r = mapRankTable.getRank(i);
		 * counter = 0; isInclude = false; for(int j = 0; j < rank.length; j++) {
		 * if(rank[j] < r) { counter++; } else if(rank[j] == r) { isInclude =
		 * true; } } if(isInclude) { newMapRankTable.addMap(counter,
		 * mapRankTable.getRankInList(i)); if(i == myRankInList) { myNewRank =
		 * counter; myNewRankInList = numElement; } numElement++; } }
		 * newGroupSize = rank.length;
		 */
		Group newGroup = new Group(msgHandle, rankTable, myNewRank, myNewRankInList, newGroupSize, backupMessage, log, newMapRankTable);
		return newGroup;

	}

	/**
	 * Create a new group which include some ranks from original group
	 * 
	 * @param group1
	 *            group1
	 * @param group2
	 *            group2
	 * @return MPI.IDENT, MPI.SIMILAR, MPI.UNEQUAL
	 */
	public static int Compare(Group group1, Group group2) {
		MapRankTable rank1 = group1.__getMapCommTable();
		MapRankTable rank2 = group2.__getMapCommTable();

		if (rank1.size() != rank2.size()) {
			return MPI.UNEQUAL;
		}

		int numMap = rank1.size();

		// Search if groups are identique
		boolean ident = true;
		for (int i = 0; i < numMap; i++) {
			if ((rank1.getRank(i) != rank2.getRank(i)) || (rank1.getRankInList(i) != rank2.getRankInList(i))) {
				ident = false;
				break;
			}
		}
		// Search if group are similar
		if (!ident) {
			// Assign rank in list
			boolean pass = false;
			for (int i = 0; i < numMap; i++) {
				for (int j = 0; j < numMap; j++) {
					if (rank1.getRankInList(i) == rank2.getRankInList(j)) {
						pass = true;
						break;
					}
				}
				if (!pass) {
					return MPI.UNEQUAL;
				}
			}
			return MPI.SIMILAR;
		}

		return MPI.IDENT;
	}

	/**
	 * Create a new group which is the union of 2 groups
	 * 
	 * @param group1
	 *            group1
	 * @param group2
	 *            group2
	 * @return Union of 2 groups
	 */
	public static Group Union(Group group1, Group group2) {
		MapRankTable rank1 = group1.__getMapCommTable();
		MapRankTable rank2 = group2.__getMapCommTable();

		MapRankTable newMapRankTable = new MapRankTable();
		int newGroupSize = 0;

		// Add all ranks of group1
		int numMap1 = rank1.size();
		int numMember1 = group1.Size();

		for (int i = 0; i < numMap1; i++) {
			newMapRankTable.addMap(rank1.getRank(i), rank1.getRankInList(i));
		}
		int myNewRank = group1.Rank();
		int myNewRankInList = group1.RankInList();

		// Add all ranks of group2 that don't exist in group2
		int numMap2 = rank2.size();
		int numMember2 = group2.Size();
		boolean alreadyAdded;
		boolean rankAdded;
		int numAdded = 0;
		int counter = 0;

		for (int i = 0; i < numMember2; i++) { // loop for all ranks
			rankAdded = false;
			for (int j = 0; j < numMap2; j++) { // loop for all tables
				if (rank2.getRank(j) == i) { // operation by rank 0 to n-1
					alreadyAdded = false;
					for (int k = 0; k < numMap1; k++) {
						if (rank2.getRankInList(j) == rank1.getRankInList(k)) {
							// already add this process in a new group
							alreadyAdded = true;
							break;
						}
					}
					if (!alreadyAdded) {
						newMapRankTable.addMap(numAdded + numMember1, rank2.getRankInList(j));
						if (j == group2.RankInList()) {
							myNewRank = numAdded + numMember1;
							myNewRankInList = numMap1 + counter;
						}
						counter++;
						rankAdded = true;
					}
				}
			}
			if (rankAdded) {
				numAdded++;
			}
		}

		newGroupSize = numAdded + numMember1;
		Group newGroup = new Group(group1.__getMessageHandler(), group1.__getRankTable(), myNewRank, myNewRankInList, newGroupSize, group1.__getBackupMessage(), group1.__getLog(), newMapRankTable);
		return newGroup;
	}

	/**
	 * Create a new group which is the intersection of 2 groups
	 * 
	 * @param group1
	 *            group1
	 * @param group2
	 *            group2
	 * @return Union of 2 groups
	 */
	public static Group Intersection(Group group1, Group group2) {
		MapRankTable rank1 = group1.__getMapCommTable();
		MapRankTable rank2 = group2.__getMapCommTable();

		int numMap1 = rank1.size();
		int numMap2 = rank2.size();

		int[] tmpRanks = new int[numMap1];
		int counter = 0;
		boolean rankDup;
		for (int i = 0; i < numMap1; i++) {
			for (int j = 0; j < numMap2; j++) {
				if (rank1.getRankInList(i) == rank2.getRankInList(j)) {
					// Check if it's exist in ranks
					rankDup = false;
					for (int k = 0; k < counter; k++) {
						if (tmpRanks[k] == rank1.getRank(i)) {
							rankDup = true;
							break;
						}
					}
					if (!rankDup) {
						tmpRanks[counter] = rank1.getRank(i);
						counter++;
					}
					break;
				}
			}
		}

		int[] ranks = new int[counter];
		for (int i = 0; i < counter; i++) {
			ranks[i] = tmpRanks[i];
		}

		return group1.Incl(ranks);
	}

	public static Group Difference(Group group1, Group group2) {
		MapRankTable rank1 = group1.__getMapCommTable();
		MapRankTable rank2 = group2.__getMapCommTable();

		int numMap1 = rank1.size();
		int numMap2 = rank2.size();

		int[] tmpRanks = new int[numMap1];
		int counter = 0;
		boolean rankDup;
		for (int i = 0; i < numMap1; i++) {
			for (int j = 0; j < numMap2; j++) {
				if (rank1.getRankInList(i) == rank2.getRankInList(j)) {
					// Check if it's exist in ranks
					rankDup = false;
					for (int k = 0; k < counter; k++) {
						if (tmpRanks[k] == rank1.getRank(i)) {
							rankDup = true;
							break;
						}
					}
					if (!rankDup) {
						tmpRanks[counter] = rank1.getRank(i);
						counter++;
					}
					break;
				}
			}
		}

		int[] ranks = new int[counter];
		for (int i = 0; i < counter; i++) {
			ranks[i] = tmpRanks[i];
		}

		return group1.Excl(ranks);
	}

	/**
	 * Internal use
	 */
	public ArrayList<SendBufferInformation> __getBackupMessage() {
		return backupMessage;
	}

	public MessageIDLog __getLog() {
		return log;
	}

	public MessageHandler __getMessageHandler() {
		return msgHandle;
	}

	public int __getNumPeers() {
		return mapRankTable.size();
	}

	public RankTable __getCommTable() {
		return rankTable;
	}

	public MapRankTable __getMapCommTable() {
		return mapRankTable;
	}

	public RankTable __getRankTable() {
		return rankTable;
	}

}
