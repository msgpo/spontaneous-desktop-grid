package ee.ut.f2f.core.mpi.internal;

import java.util.ArrayList;
import java.util.Date;

import ee.ut.f2f.core.mpi.MPIDebug;
import ee.ut.f2f.core.mpi.MPITask;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.common.Tag;
import ee.ut.f2f.core.mpi.message.BasicMessage;
import ee.ut.f2f.core.mpi.message.DataMessage;
import ee.ut.f2f.core.mpi.message.MPIMessage;
import ee.ut.f2f.core.mpi.message.MessageCmd;
import ee.ut.f2f.core.mpi.message.NotifyMessage;
import ee.ut.f2f.core.mpi.message.OutputMessage;
import ee.ut.f2f.core.mpi.message.RequestQuitMessage;
import ee.ut.f2f.core.mpi.message.UpdateStatusMessage;

/**
 * Asynchronous message handler for handling all received messages
 */
public class MessageHandler {
	private MPITask task;
	private boolean ready; // Ready Flag
	public ArrayList<DataMessage> messageBuffer; // Receive Part
	private ArrayList<MPIMessage> MPIMessageSYN1 = new ArrayList<MPIMessage>();
	private MessageIDLog recvLog; // to compare duplicate message
	private int[] sequenceTo, sequenceFrom; // a counter for trakking message order

	private ArrayList<SendBufferInformation> backupBuffer; // Send Part
	private MessageIDLog sendLog; // backup log

	private int t_gossip; // time for each gossip
	private int t_margin; // margin time because starting counting heartbeat
	// table
	private int t_hang;

	public MessageHandler(MPITask task) {
		this.task = task;
		this.ready = false;
		this.messageBuffer = new ArrayList<DataMessage>();
		this.recvLog = new MessageIDLog();
		this.sequenceTo = null;
		this.sequenceFrom = null;
	}

	public void setSendBackupAndLog(ArrayList<SendBufferInformation> backupBuffer, MessageIDLog sendLog) {
		this.backupBuffer = backupBuffer;
		this.sendLog = sendLog;
		synchronized (this) {
			this.notifyAll();
		}
	}

	public void setTHang(int t_hang) {
		this.t_hang = t_hang;
	}

	public int getTHang() {
		return t_hang;
	}

	public void messageReceived(BasicMessage message, String fromTaskID) {
		String mid;
		if (message instanceof MPIMessage) {
			MPIMessage mpiMsg = (MPIMessage) message;
			task.getMPIDebug().println(MPIDebug.SYSTEM, "recived MPIMessage qirh CMD " + mpiMsg.getCmd() + " " + mpiMsg.getRank());
			switch (mpiMsg.getCmd()) {
			case MessageCmd.MPI_SYN1:
				synchronized (getMPIMessageSYN1()) {
					getMPIMessageSYN1().add(mpiMsg);
				}
				synchronized (task) {
					task.notifyAll();
				}
				task.getMPIDebug().println(MPIDebug.SYSTEM, "n done " + new Date());
				break;
			case MessageCmd.MPI_SYN2:
				ready = true;
				task.setMyRank(mpiMsg.getRank());
				task.setMyRankInList(mpiMsg.getRankInList());
				task.setCommSize(mpiMsg.getSize());
				task.setRankTable(mpiMsg.getCommTable());
				t_gossip = mpiMsg.getTGossip();
				t_margin = mpiMsg.getTMargin();
				t_hang = mpiMsg.getTHang();
				setSequence(task.getCommSize());
				getTask().getMPIDebug().setName("F2F DEBUG WINDOW: Task - " + task.getTaskID() + " Rank - " + task.getMyRank());
				synchronized (task) {
					task.notifyAll();
				}
				break;
			}
		} else if (message instanceof DataMessage) {
			DataMessage dataMsg = (DataMessage) message;
			mid = dataMsg.getMID();
			if (recvLog.isExist(mid) == -1) {
				// If message ID is not exist Put it in Buffer
				synchronized (messageBuffer) {
					getTask().getMPIDebug().println(MPIDebug.SYSTEM - 1, "recived " + dataMsg.getSequence()+ " tag "+ dataMsg.getTag());
					messageBuffer.add(dataMsg);
				}
				synchronized (recvLog) {
					recvLog.add(mid);
				}
			}
		} else if (message instanceof UpdateStatusMessage) {
			UpdateStatusMessage updateMsg = (UpdateStatusMessage) message;
			int i = 0;
			while (sendLog == null) {
				i++;
				task.getMPIDebug().println(MPIDebug.SYSTEM, "sleep (100) in place X " + i);
				task.wait(1000);
			}
			// Check if message is existed
			mid = updateMsg.getMID();
			int logIndex;
			synchronized (sendLog) {
				logIndex = sendLog.isExist(mid);
			}
			if (logIndex != -1) {
				// If log is existed, it means that
				// this process is faster than master
				// then just remove log and backupBuffer
				synchronized (sendLog) {
					sendLog.remove(mid);
				}
				synchronized (backupBuffer) {
					backupBuffer.remove(logIndex);
				}

			} else {
				// log does not exist. it means that
				// this process is slower than master
				// then just add log
				synchronized (sendLog) {
					sendLog.add(mid);
				}
			}
		} else if (message instanceof RequestQuitMessage) {
			// just exit
			try {
				Thread.sleep(5 * 1000);
			} catch (Exception e) {
			}
			task.exit("RequestQuitMessage from " + fromTaskID);
		} else if (message instanceof NotifyMessage) {// some peer is dead must remove it
			task.getMPIDebug().println(MPIDebug.SYSTEM, "Recived : NotifyMessage");
			int index = task.getRankTable().getIndexByTaskID(((NotifyMessage) message).getDeadTaskID());
			task.getMPIPresenceListener().peerDead(index);
		} else if (message instanceof OutputMessage) {
			OutputMessage outputMsg = (OutputMessage) message;
			task.getMPIDebug().println(MPIDebug.SYSTEM, outputMsg.getOutput());
		} else {
			task.getMPIDebug().println(MPIDebug.SYSTEM, "** [Warning] received unknow type of message.");
		}
	}

	public int getMessageBufferSize() {
		return (messageBuffer.size());
	}

	public boolean isReady() {
		task.isTerminated();
		return ready;
	}

	public Object getDataFromBuffer(int fromRank, int toRank, int tag, IStatus status) {
		DataMessage tmp;
		synchronized (messageBuffer) {
			int numElement = messageBuffer.size();
			for (int i = 0; i < numElement; i++) {
				tmp = messageBuffer.get(i);
				boolean match_dst = (tmp.getFromRank() == fromRank) && (tmp.getToRank() == toRank);
				boolean match_tag = tmp.getTag() == tag;
				boolean any_src = fromRank == Tag.MPI_ANYSOURCE;
				boolean any_tag = tag == Tag.MPI_ANYTAG;
				if ((match_dst || any_src) && (match_tag || any_tag) && tmp.getSequence() <= getSequenceFrom()[tmp.getFromRank()]) {
					status.setStatus(tmp.getFromRank(), tmp.getTag());
					task.getMPIDebug().println(MPIDebug.SYSTEM, "getDataFromBuffer from rank " + tmp.getFromRank() + " sequence = " + tmp.getSequence() + " limit was " + getSequenceFrom()[tmp.getFromRank()]);
					return tmp.getData();
				}
			}
		}
		return null;
	}

	// TODO : performance may drop because we search 2 times
	public void removeDataFromBuffer(int fromRank, int toRank, int tag) {
		DataMessage tmp;
		synchronized (messageBuffer) {
			int numElement = messageBuffer.size();
			for (int i = 0; i < numElement; i++) {
				tmp = messageBuffer.get(i);

				boolean match_dst = (tmp.getFromRank() == fromRank) && (tmp.getToRank() == toRank);
				boolean match_tag = tmp.getTag() == tag;
				boolean any_src = fromRank == Tag.MPI_ANYSOURCE;
				boolean any_tag = tag == Tag.MPI_ANYTAG;

				if ((match_dst || any_src) && (match_tag || any_tag) && tmp.getSequence() <= getSequenceFrom()[tmp.getFromRank()]) {
					messageBuffer.remove(i);
					getSequenceFrom()[tmp.getFromRank()]++;
					return;
				}
			}
		}
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

	public MPITask getTask() {
		return task;
	}

	public void setTask(MPITask task) {
		this.task = task;
	}

	public ArrayList<MPIMessage> getMPIMessageSYN1() {
		return MPIMessageSYN1;
	}

	public void sendBackupBuffer() {
		RankTable rankTable = task.getRankTable();
		synchronized (backupBuffer) {
			// While backupBuffer is not empty, send message to destination
			while (backupBuffer.size() != 0) {
				SendBufferInformation binfo = backupBuffer.remove(0);
				ArrayList<String> dests = rankTable.getTaskIDsByRank(binfo.toRank());
				// send message to destination
				for (String destTaskID : dests) {
					task.sendMessage(destTaskID, binfo.getData());
				}
				// update status to replica
				UpdateStatusMessage updateMsg = new UpdateStatusMessage(binfo.getMID());
				ArrayList<Integer> myRep = rankTable.getRankInListByRank(task.getMyRank());
				for (Integer ranks : myRep) {
					int rRank = ranks.intValue();
					if ((rRank != task.getMyRankInList()) && (rankTable.isAlive(rRank))) {
						task.sendMessage(rankTable.getTaskID(rRank), updateMsg);
					}
				}
				// remove backup buffer
				sendLog.remove(binfo.getMID());
			}
		}
	}

	public int[] getSequenceTo() {
		return sequenceTo;
	}

	public int[] getSequenceFrom() {
		return sequenceFrom;
	}

	public void setSequence(int max) {
		this.sequenceTo = new int[max];
		this.sequenceFrom = new int[max];
		for (int i = 0; i < sequenceTo.length; i++) {
			sequenceTo[i] = 0;
			sequenceFrom[i] = 0;
		}
	}
}
