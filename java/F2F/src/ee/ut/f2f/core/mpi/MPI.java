package ee.ut.f2f.core.mpi;

import java.net.InetAddress;
import java.util.Collection;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.mpi.common.MapRankTable;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.common.Tag;
import ee.ut.f2f.core.mpi.exceptions.MPIAlreadyInitializedException;
import ee.ut.f2f.core.mpi.exceptions.MPINotInitializedException;
import ee.ut.f2f.core.mpi.message.MPIMessage;
import ee.ut.f2f.core.mpi.message.MessageCmd;
import ee.ut.f2f.util.LocalAddresses;
import ee.ut.f2f.util.logging.Logger;

public class MPI {

	private final static Logger logger = Logger.getLogger(MPI.class);
	/**
	 * Primitive datatype
	 */
	public static Datatype BYTE, CHAR, SHORT, BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, OBJECT;

	/**
	 * Primitive datatype
	 */
	public static Datatype BYTE2, CHAR2, SHORT2, INT2, LONG2, FLOAT2, DOUBLE2;

	/**
	 * Primitive datatype (not available yet)
	 */
	public static Datatype PACKED;

	/**
	 * Primitive datatype constant
	 */
	public static int ANY_SOURCE;

	/**
	 * Primitive datatype constant
	 */
	public static int ANY_TAG;

	/**
	 * Return value of comparing MPI Group
	 */
	public static int UNEQUAL, SIMILAR, IDENT;

	/**
	 * Undefined Rank
	 */
	public static int UNDEFINED;

	/**
	 * Primitive collective-operation
	 */
	public static Op MAX, MIN, SUM, PROD, BAND, MAXLOC, MINLOC;

	/**
	 * MPI_COMM_WORLD
	 */
	private TypelessComm COMM_WORLD = null;
	private MPITask task = null;
	
	static {
		// Initialized Basic Datatype
		BYTE = new Datatype(Datatype.BYTE);
		CHAR = new Datatype(Datatype.CHAR);
		SHORT = new Datatype(Datatype.SHORT);
		BOOLEAN = new Datatype(Datatype.BOOLEAN);
		INT = new Datatype(Datatype.INT);
		LONG = new Datatype(Datatype.LONG);
		FLOAT = new Datatype(Datatype.FLOAT);
		DOUBLE = new Datatype(Datatype.DOUBLE);
		PACKED = new Datatype(Datatype.PACKED);
		STRING = new Datatype(Datatype.STRING);
		OBJECT = new Datatype(Datatype.OBJECT);
		BYTE2 = BYTE.Contiguous(2);
		CHAR2 = CHAR.Contiguous(2);
		SHORT2 = SHORT.Contiguous(2);
		INT2 = INT.Contiguous(2);
		LONG2 = LONG.Contiguous(2);
		FLOAT2 = FLOAT.Contiguous(2);
		DOUBLE2 = DOUBLE.Contiguous(2);
		ANY_SOURCE = Tag.MPI_ANYSOURCE;
		ANY_TAG = Tag.MPI_ANYTAG;
		UNDEFINED = -3;
		UNEQUAL = 0;
		SIMILAR = 1;
		IDENT = 2;

		// Initialized Basic Operation
		MAX = new Op(Op.MAX);
		MIN = new Op(Op.MIN);
		SUM = new Op(Op.SUM);
		PROD = new Op(Op.PROD);
		BAND = new Op(Op.BAND);
		MAXLOC = new Op(Op.MAXLOC);
		MINLOC = new Op(Op.MINLOC);
	}

	public MPI(MPITask task) {
		this.task = task;
	}

	public void Init() {
		Init(0, 0);
	}

	public void Init(int maxRank) {
		Init(maxRank, 0);
	}

	/**
	 * Initialization the MPI program
	 * 
	 * @param maxRank
	 *            An optional parameter, says how many different slave processes we need in calculation. If it is 0, then the value is ignored. If there are more peers then maxRank then the remaining peers will be synchronized peers for backup. 
	 * @param numOfJobsPerPeer
	 *            An optional parameter, says how many slave processes we submit per peer. If it is 0, then the value is ignored. This value can be used to make virtual peers, so if you have just a few computers in your disposal then the program handles one peer as it where numOfJobsPerPeer different peers. 
	 */
	public void Init(int maxRank, int numOfJobsPerPeer) {
		if (Initialized()) {
			throw new MPIAlreadyInitializedException();
		}
		task.addPeerPresenceListener();
		MPIMessage mpiMsg = null;
		RankTable rankTable = new RankTable();
		MapRankTable mapRankTable = new MapRankTable();
		task.getMPIDebug().println(MPIDebug.START_UP, "task.getJob().getMasterTaskID() = " + task.getJob().getMasterTaskID() + " task.getTaskID() = " + task.getTaskID());
		if (task.getTaskID().equals(task.getJob().getMasterTaskID())) {
			Collection<F2FPeer> peers = task.getJob().getPeers();
			if (maxRank < 1) {
				maxRank = peers.size() + 1;
			}
			task.getMessageHandler().setSequence(maxRank + 1);
			task.getMPIDebug().println(MPIDebug.START_UP, "Start master with " + peers.size() + " peers");
			try {
				for (int i = 0; i < numOfJobsPerPeer || i < 1; i++) {
					task.getJob().submitTasks(task.getClass().getName(), peers.size(), peers);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			task.getMPIDebug().println(MPIDebug.START_UP, "Info, the peers have got their work");
			rankTable.addProcess(0, task.getTaskID()); // add self for rank 0
			mapRankTable.addMap(0, 0);
			int assignRank = 1;
			int readyNode = 0;
			while (!task.isTerminated()) {
				synchronized (task.getMessageHandler().getMPIMessageSYN1()) {
					if (task.getMessageHandler().getMPIMessageSYN1().size() > 0) {
						mpiMsg = task.getMessageHandler().getMPIMessageSYN1().remove(0);
					}
				}
				if (mpiMsg == null) {
					task.wait(1000);
				}
				// Construct a table
				if (mpiMsg != null) {
					task.getMPIDebug().println(MPIDebug.START_UP, "* got SYN1:" + assignRank + ":" + mpiMsg.getTaskID());
					rankTable.addProcess(assignRank, mpiMsg.getTaskID());
					readyNode++;
					mapRankTable.addMap(assignRank, readyNode);
					if (readyNode >= task.getJob().getTaskIDs().size() - 1) {
						break;
					}
					assignRank++;
					if (assignRank >= maxRank) {
						assignRank = 1;
					}
					mpiMsg = null;
				}
			}

			// Send List and rank in list to all processes
			task.getMPIDebug().println(MPIDebug.START_UP, "Start message handler");
			task.setRankTable(rankTable);
			task.setCommSize(maxRank);
			task.getMPIDebug().println(MPIDebug.START_UP, "Send sync to slaves " + rankTable.size());
			for (int i = 1; i < rankTable.size(); i++) { // Skip process 0
				mpiMsg = new MPIMessage(MessageCmd.MPI_SYN2);
				mpiMsg.setCommTable(rankTable);
				mpiMsg.setRank(rankTable.getRank(i));
				mpiMsg.setRankInList(i);
				mpiMsg.setSize(maxRank);
				try {
					task.getMPIDebug().println(MPIDebug.START_UP, "* send SYN2: " + rankTable.getTaskID(i));
					task.sendMessage(rankTable.getTaskID(i), mpiMsg);
				} catch (Exception e) {
					logger.info("Sync with master faild", e);
					task.getMPIDebug().println(MPIDebug.START_UP, "Sync with master faild");
				}
			}
			task.getMPIDebug().println(MPIDebug.START_UP, "master up");
		} else {
			mpiMsg = new MPIMessage(MessageCmd.MPI_SYN1);
			mpiMsg.setTaskID(task.getTaskID());
			try {
				task.sendMessage(task.getJob().getMasterTaskID(), mpiMsg);
			} catch (Exception e) {
				task.exit("Sync with master faild");
			}
			// BLOCK for List and its rank in List from Rank 0
			while (!task.getMessageHandler().isReady()) {
				task.wait(1000);
			}
			rankTable = task.getRankTable();
			for (int i = 0; i < rankTable.size(); i++) {
				mapRankTable.addMap(rankTable.getRank(i), i);
			}
		}

		// Create comm world
		COMM_WORLD = new TypelessComm(task.getMessageHandler(), rankTable, task.getMyRank(), task.getMyRankInList(), task.getCommSize(), mapRankTable);
		COMM_WORLD().Barrier();
	}

	/**
	 * Finalization the MPI program
	 */
	public void Finalize() {
		COMM_WORLD().Barrier();
		COMM_WORLD().getMessageHandler().getTask().exit(null);
		COMM_WORLD = null;
	}

	public TypelessComm COMM_WORLD() {
		if (COMM_WORLD == null) {
			throw new MPINotInitializedException();
		}
		return COMM_WORLD;
	}
	
	/**
	 * Says if MPI has been initialized or not
	 * 
	 * @return a boolean
	 */
	public boolean Initialized() {
		return COMM_WORLD != null;
	}

	/**
	 * Returns an elapsed time on the calling processor (seconds)
	 * 
	 * @return elapsed wallclock time in seconds since some time in the past
	 */
	public double Wtime() {
		return (System.currentTimeMillis() / 1000.0);
	}

	/**
	 * Returns the resolution of Wtime
	 * 
	 * @return resolution of wtime in seconds
	 */
	public double Wtick() {
		double tick = 0.0;
		double t;

		if (tick == 0.0) {
			tick = System.currentTimeMillis();
			tick = System.currentTimeMillis() - tick;

			for (int counter = 0; counter < 10; counter++) {
				t = System.currentTimeMillis();
				t = System.currentTimeMillis() - t;
				if (t < tick) {
					tick = t;
				}
			}

			tick = (tick > 0.0) ? tick : 1.0e-5;
		}
		return tick / 1000.0;
	}

	/**
	 * Returns a local hostname - this one is part of the MPJ API
	 * 
	 * @return local hostname
	 */
	public String Get_processor_name() {
		try {
			LocalAddresses la = LocalAddresses.getInstance();
			StringBuffer sb = new StringBuffer();
			for (InetAddress ia : la.getLocalIPv4Addresses()) {
				sb.append(ia.getHostName()).append("_");
			}
			sb.append(F2FComputing.getLocalPeer().getID());
			return (sb.toString());
		} catch (Exception e) {
			return null;
		}
	}
}
