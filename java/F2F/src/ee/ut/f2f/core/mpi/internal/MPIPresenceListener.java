package ee.ut.f2f.core.mpi.internal;

import java.util.Collection;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.PeerPresenceListener;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.core.mpi.MPIDebug;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.message.NotifyMessage;

/**
 * Keeps an eye on dead peers, to remove them and replace with replicas.
 */
public class MPIPresenceListener implements PeerPresenceListener {
	private MPITask task;

	public MPIPresenceListener(MPITask task) {
		this.task = task;
	}

	public void peerContacted(F2FPeer peer) {
	}

	/**
	 * Unregister a dead peer
	 */
	public void peerUnContacted(F2FPeer peer) {
		task.getMPIDebug().println(MPIDebug.SYSTEM, "peerUnContacted " + peer.getID());
		if (task.getTerminated() != null) {// If peer has finished job and the listener is not turned off for some reason, remove it.
			F2FComputing.removePeerPresenceListener(this);
			return;
		}
		Collection<TaskProxy> taskProxies = task.getTaskProxies();
		for (TaskProxy tp : taskProxies) {
			task.getMPIDebug().println(MPIDebug.SYSTEM, "taskPeers " + tp.getRemoteTaskDescription().getPeerID() + " " + tp.getRemoteTaskID());
			if (task.getRankTable() != null && peer.getID().equals(tp.getRemoteTaskDescription().getPeerID()) && (task.getTaskID().equals(task.getJob().getMasterTaskID()) || tp.getRemoteTaskID().equals(task.getJob().getMasterTaskID()))) {
				peerDead(task.getRankTable().getIndexByTaskID(tp.getRemoteTaskID()));
			}
		}
	}

	public void peerDead(int deadNodeRank) {
		RankTable rankTable = task.getRankTable();
		if (task.getMyRankInList() == deadNodeRank) {// I am dead
			task.exit("Message, that I am dead?");
			return;
		}
		synchronized (rankTable) {
			if (!rankTable.isAlive(deadNodeRank)) {
				task.getMPIDebug().println(MPIDebug.SYSTEM, "Peer " + rankTable.getTaskID(deadNodeRank) + " allready marked deade");
				return;
			}
			task.getMPIDebug().println(MPIDebug.SYSTEM, "Peer " + rankTable.getTaskID(deadNodeRank) + " died");
			rankTable.setAlive(deadNodeRank, false);
		}
		// Check if at least 1 rank still alive
		boolean[] rankAliveNode = new boolean[task.getCommSize()];
		// init with false;
		for (int i = 0; i < task.getCommSize(); i++) {
			rankAliveNode[i] = false;
		}
		synchronized (rankTable) {
			for (int i = 0; i < rankTable.size(); i++) {
				if (rankTable.isAlive(i)) {
					rankAliveNode[rankTable.getRank(i)] = true;
				}
			}
		}
		for (int i = 0; i < task.getCommSize(); i++) {
			if (!rankAliveNode[i]) {
				task.exit("[Error] rank " + i + " process failed (i.e. all replicas for that rank failed). Application did not complete. <<detected by " + task.getMyRankInList() + ">>");
				return;
			}
		}
		// If master can't see the peer, then it is dead and we must notify other peers.
		if (task.getTaskID().equals(task.getJob().getMasterTaskID())) {
			NotifyMessage message = new NotifyMessage(rankTable.getTaskID(deadNodeRank));
			// send messages to all nodes, that a node is dead
			for (int i = 1; i < rankTable.size(); i++) {
				if (rankTable.isAlive(i)) {
					task.sendMessage(rankTable.getTaskID(i), message);
				}
			}
		} else {
			// if dead node is master, check if it's a new master
			// if it's a new master then resend message from backupBuffer
			if (deadNodeRank == task.getReplicaMaster()) {
				// Check for new master
				for (int i = 0; i < rankTable.size(); i++) {
					if (task.getMyRank() == rankTable.getRank(i)) {
						if (rankTable.isAlive(i)) {
							task.setReplicaMaster(i);
							break;
						}
					}
				}
				// if i'm a new master resend mesages in backup
				if (task.isMaster()) {
					task.getMessageHandler().sendBackupBuffer();
				}
			}
		}
	}
}
