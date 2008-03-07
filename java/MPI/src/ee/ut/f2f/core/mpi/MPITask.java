package ee.ut.f2f.core.mpi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.internal.MPIPresenceListener;
import ee.ut.f2f.core.mpi.internal.MessageHandler;
import ee.ut.f2f.core.mpi.message.BasicMessage;
import ee.ut.f2f.core.mpi.message.RequestQuitMessage;
import ee.ut.f2f.util.logging.Logger;

public abstract class MPITask extends Task {

	private final static Logger logger = Logger.getLogger(MPITask.class);

	private MessageHandler messageHandler = new MessageHandler(this);
	private MPIPresenceListener listener = null;
	private String terminated = null;
	private RankTable rankTable; // Communication Table
	private int commSize; // Number of MPI Rank
	private int myRank; // Rank in MPI
	private int myRankInList; // Rank in Comm Table
	private int replicaMaster; // Master Rank of this replica
	private MPI mpi = new MPI();
	MPIDebug mpiDebug = null;

	public void messageReceivedEvent(String taskID) {
		try {
		Object msg = getTaskProxy(taskID).receiveMessage();

		if (msg == null) {
			getMPIDebug().println(MPIDebug.SYSTEM, "Message from " + taskID + " was null");
		} else {
			getMPIDebug().println(MPIDebug.SYSTEM, "A " + msg.getClass() + " message from " + taskID);
			logger.info("A " + msg.getClass() + " message from " + taskID);
			if (msg instanceof BasicMessage) {
				getMessageHandler().messageReceived((BasicMessage) msg, taskID);
			}
		}
		} catch (Exception e) {
			logger.error("messageReceived faild", e);
		}
		catch (Error e)
		{
			logger.error("Some ERROR with sendMessage!!",e );
		}
	}

	public void sendMessage(String taskID, Object message) {
		TaskProxy tp = getTaskProxy(taskID);
		if (tp != null) {
			try {
				getMPIDebug().println(MPIDebug.SYSTEM, "sendMessage from " + getTaskID() + " to " + tp.getRemoteTaskID());
				tp.sendMessage(message);
			} catch (Exception e) {
				logger.info("Send faild", e);
				getMPIDebug().println(MPIDebug.SYSTEM + 20, "Error sendMessage to " + tp.getRemoteTaskID() + " retry");
				try {
					tp.sendMessage(message);
				} catch (CommunicationFailedException ex) {
					exit("Error communikating with task " + tp.getRemoteTaskID());
					throw new MPIException();
				}
				getMPIDebug().println(MPIDebug.SYSTEM - 1, "sending done");
			}
			catch (Error e)
			{
				logger.error("Some ERROR with sendMessage!!",e );
			}
		} else {
			getMPIDebug().println(MPIDebug.SYSTEM, "No proxy for task " + taskID);
		}
	}

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	public void addPeerPresenceListener() {
		getMPIDebug().println(MPIDebug.SYSTEM, "add addPeerPresenceListener");
		if (listener == null) {
			listener = new MPIPresenceListener(this);
			F2FComputing.addPeerPresenceListener(listener);
		} else {
			getMPIDebug().println(MPIDebug.SYSTEM, "Listener exists!");
		}
	}

	public void exit(String message) {
		String msg = "EXIT " + getTaskID() + " (" + new Date() + ")";
		if (message != null) {
			msg = msg + " : " + message;
		}
		if (terminated != null) {
			getMPIDebug().println(MPIDebug.SYSTEM, "Another try for " + msg);
			return;
		}
		getMPIDebug().println(MPIDebug.SYSTEM + 20, msg);
		F2FComputing.removePeerPresenceListener(listener);
		terminated = msg;
		// If master has done then send nodes a message to quit.
		if (getTaskID().equals(getJob().getMasterTaskID())) {
			RequestQuitMessage rqm = new RequestQuitMessage();
			for (int i = 1; i < getRankTable().size(); i++) {
				if (getRankTable().isAlive(i)) {
					sendMessage(getRankTable().getTaskID(i), rqm);
				}
			}
		}
	}

	public BufferedReader getFailFromClasspath(String fileName) {
		InputStream s = getClass().getClassLoader().getResourceAsStream(fileName);
		return new BufferedReader(new InputStreamReader(s));
	}

	public boolean isTerminated() {
		if (terminated != null) {
			getMPIDebug().println(MPIDebug.SYSTEM, "Will try to terminate now ( " + (new Date()) + ")");
			throw new MPITerminateException(terminated);
		}
		return false;
	}

	public MPIDebug getMPIDebug() {
		if (mpiDebug == null) {

			synchronized (MPIDebug.class) {
				if (mpiDebug == null)
					mpiDebug = new MPIDebug(this);
			}
		}
		return mpiDebug;
	}

	public void setReplicaMaster(int rankInList) {
		this.replicaMaster = rankInList;
		if (isMaster()) {
			getMPIDebug().println(MPIDebug.SYSTEM, "I AM A REPLICA MASTER");
		} else {
			getMPIDebug().println(MPIDebug.SYSTEM, "I AM A REPLICA");
		}
	}

	public int getReplicaMaster() {
		return replicaMaster;
	}

	public boolean isMaster() {
		return getReplicaMaster() == getMyRankInList();
	}

	public void wait(int time) {
		try {
			synchronized (this) {
				super.wait(time);
			}
		} catch (InterruptedException e) {
		}
	}

	public MPIPresenceListener getMPIPresenceListener() {
		if (listener == null) {
			addPeerPresenceListener();
		}
		return listener;
	}

	public MPI MPI() {
		return mpi;
	}

	public int getMyRank() {
		return myRank;
	}

	public void setMyRank(int myRank) {
		this.myRank = myRank;
	}

	public int getMyRankInList() {
		return myRankInList;
	}

	public void setMyRankInList(int myRankInList) {
		this.myRankInList = myRankInList;
	}

	public void setRankTable(RankTable rankTable) {
		this.rankTable = rankTable;
	}

	public RankTable getRankTable() {
		return rankTable;
	}

	public int getCommSize() {
		return commSize;
	}

	public void setCommSize(int commSize) {
		this.commSize = commSize;
	}

	public String getTerminated() {
		return terminated;
	}
}
