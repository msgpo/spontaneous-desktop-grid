package ee.ut.f2f.core.mpi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Properties;

import ee.ut.f2f.core.mpi.common.MapRankTable;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.internal.IStatus;
import ee.ut.f2f.core.mpi.internal.MessageHandler;
import ee.ut.f2f.core.mpi.internal.MessageIDLog;
import ee.ut.f2f.core.mpi.internal.SendBufferInformation;
import ee.ut.f2f.core.mpi.message.DataMessage;
import ee.ut.f2f.core.mpi.message.UpdateStatusMessage;

/**
 * Point-to-Point communication class
 */
public class Comm {

	private static int globalCommID = 0;
	private MessageHandler msgHandle;
	protected int commID;
	protected Group myGroup;
	// protected RankTable rankTable;
	protected MapRankTable mapRankTable;
	protected Properties midLog = new Properties();

	private int numReplica;
	private int[] myReplica;
	private static int MAX_REPLICA = 256;
	private int numProc;
	private int waitingTime;
	// private boolean master;

	private MessageIDLog sendLog;
	private ArrayList<SendBufferInformation> backupMessage;

	// Comm world construct
	/**
	 * Internal use
	 */
	public Comm(MessageHandler msgHandle, RankTable rankTable, int rank, int rankInList, int numRank, MapRankTable mapRankTable) {
		sendLog = new MessageIDLog();
		backupMessage = new ArrayList<SendBufferInformation>();

		this.msgHandle = msgHandle;
		this.msgHandle.setSendBackupAndLog(backupMessage, sendLog);

		myGroup = new Group(msgHandle, rankTable, rank, rankInList, numRank, backupMessage, sendLog, mapRankTable);

		commID = globalCommID;
		globalCommID++;

		// find my replica
		myReplica = new int[MAX_REPLICA];
		RankTable tmpTable = myGroup.__getCommTable();

		MapRankTable tmpMapTable = myGroup.__getMapCommTable();
		numProc = tmpMapTable.size();
		int myRank = myGroup.Rank();
		for (int i = 0; i < numProc; i++) {
			int realRIL = tmpMapTable.getRankInList(i);
			if (tmpTable.isAlive(realRIL)) {
				if (myRank == tmpMapTable.getRank(i)) {
					myReplica[numReplica] = i;
					numReplica++;
				}
			}
		}
		// find master
		int myRankInList = myGroup.RankInList();
		int myMaster = myRankInList;
		for (int i = 0; i < numReplica; i++) {
			int realRIL = tmpMapTable.getRankInList(myReplica[i]);
			// Recheck isAlive again to make sure
			if (tmpTable.isAlive(realRIL)) {
				if (myMaster > myReplica[i]) {
					myMaster = myReplica[i];
				}
			}
		}
		msgHandle.getTask().setReplicaMaster(myMaster);
		waitingTime = (((int) (Math.log(numProc) / Math.log(2)) * 3) * msgHandle.getTGossip() * 2) + msgHandle.getTMargin();

	}

	// construct from Group class
	/**
	 * Create a new communicator by group
	 * 
	 * @param group
	 *            Group
	 */
	public Comm(Group group) {
		myGroup = group;
		this.msgHandle = group.__getMessageHandler();

		commID = globalCommID;
		globalCommID++;

		// find my replica
		myReplica = new int[MAX_REPLICA];
		RankTable tmpTable = myGroup.__getCommTable();
		MapRankTable tmpMapTable = myGroup.__getMapCommTable();
		// int numProc = tmpTable.size();
		numProc = tmpMapTable.size();
		int myRank = myGroup.Rank();

		for (int i = 0; i < numProc; i++) {
			int realRIL = tmpMapTable.getRankInList(i);
			if (tmpTable.isAlive(realRIL)) {
				if (myRank == tmpMapTable.getRank(i)) {
					myReplica[numReplica] = i;
					numReplica++;
				}
			}
		}

		// find master
		int myRankInList = myGroup.RankInList();
		int myMaster = myRankInList;
		for (int i = 0; i < numReplica; i++) {
			int realRIL = tmpMapTable.getRankInList(myReplica[i]);
			// Recheck isAlive again to make sure
			if (tmpTable.isAlive(realRIL)) {
				if (myMaster > myReplica[i]) {
					myMaster = myReplica[i];
				}
			}
		}
		msgHandle.getTask().setReplicaMaster(myMaster);
		sendLog = myGroup.__getLog();
		backupMessage = myGroup.__getBackupMessage();
		waitingTime = (((int) (Math.log(numProc) / Math.log(2)) * 3) * msgHandle.getTGossip() * 2) + msgHandle.getTMargin();
	}

	/**
	 * Returns group of this communicator
	 * 
	 * @return group
	 */
	public Group Group() {
		// return (new Group(myGroup));
		return myGroup;
	}

	/**
	 * Returns the number of processes in communicator.
	 * 
	 * @return number of processes in co
	 */
	public int Size() {
		return myGroup.Size();
	}

	/**
	 * Internal use.
	 */
	public int SizeTotal() {
		return myGroup.__sizetotal();
	}

	/**
	 * Returns my rank number in communicator.
	 * 
	 * @return MPI rank
	 */
	public int Rank() {
		return myGroup.Rank();
	}

	/**
	 * Basic send operation.
	 * 
	 * @param sendBuffer
	 *            send buffer array
	 * @param offset
	 *            initial offset in send buffer
	 * @param count
	 *            number of items to send
	 * @param datatype
	 *            datatype of send buffer items
	 * @param dest
	 *            rank of destination
	 * @param tag
	 *            message tag
	 */
	public int Send(Object sendBuffer, int offset, int count, Datatype datatype, int dest, int tag) {
		if (Rank() == MPI.UNDEFINED) {
			return -1;
		}
		msgHandle.getTask().getMPIDebug().println(MPIDebug.SYSTEM, "start Send to " + dest + " from " + Rank() + " whit " + tag + " " + msgHandle.getTask().isMaster() + " " + (msgHandle.getSequenceTo()[dest] + 1));
		String messageID = getMessageID(Rank(), dest, tag);
		// Prepare buffer for sending
		int baseType = datatype.getBaseType();
		int dispSeqLen = datatype.getDisplacementSequence().length;
		DataMessage msg = new DataMessage(messageID, Rank(), dest, tag, msgHandle.getSequenceTo()[dest]++);
		switch (baseType) {
		case Datatype.OBJECT:
			byte[] tmp = Object_Serialize(sendBuffer, offset, count, datatype);
			msg.addData(tmp);
			break;
		case Datatype.STRING:
			String[] str_buffer = (String[]) sendBuffer;
			String[] str_newBuffer = new String[dispSeqLen * count];
			for (int i = 0; i < dispSeqLen * count; i++) {
				str_newBuffer[i] = new String(str_buffer[i]);
			}
			msg.addData(str_newBuffer);
			break;

		case Datatype.BYTE:
			byte[] b_buffer = (byte[]) sendBuffer;
			byte[] b_newBuffer = new byte[dispSeqLen * count];
			System.arraycopy(b_buffer, offset, b_newBuffer, 0, dispSeqLen * count);
			msg.addData(b_newBuffer);
			break;

		case Datatype.CHAR:
			char[] ch_buffer = (char[]) sendBuffer;
			char[] ch_newBuffer = new char[dispSeqLen * count];
			System.arraycopy(ch_buffer, offset, ch_newBuffer, 0, dispSeqLen * count);
			msg.addData(ch_newBuffer);
			break;

		case Datatype.SHORT:
			short[] sh_buffer = (short[]) sendBuffer;
			short[] sh_newBuffer = new short[dispSeqLen * count];
			System.arraycopy(sh_buffer, offset, sh_newBuffer, 0, dispSeqLen * count);
			msg.addData(sh_newBuffer);
			break;

		case Datatype.INT:
			int[] i_buffer = (int[]) sendBuffer;
			int[] i_newBuffer = new int[dispSeqLen * count];
			System.arraycopy(i_buffer, offset, i_newBuffer, 0, dispSeqLen * count);
			msg.addData(i_newBuffer);
			break;

		case Datatype.LONG:
			long[] l_buffer = (long[]) sendBuffer;
			long[] l_newBuffer = new long[dispSeqLen * count];
			System.arraycopy(l_buffer, offset, l_newBuffer, 0, dispSeqLen * count);
			msg.addData(l_newBuffer);
			break;

		case Datatype.FLOAT:
			float[] f_buffer = (float[]) sendBuffer;
			float[] f_newBuffer = new float[dispSeqLen * count];
			System.arraycopy(f_buffer, offset, f_newBuffer, 0, dispSeqLen * count);
			msg.addData(f_newBuffer);
			break;

		case Datatype.DOUBLE:
			double[] d_buffer = (double[]) sendBuffer;
			double[] d_newBuffer = new double[dispSeqLen * count];
			System.arraycopy(d_buffer, offset, d_newBuffer, 0, dispSeqLen * count);
			msg.addData(d_newBuffer);
			break;
		case Datatype.BOOLEAN:
			boolean[] bl_buffer = (boolean[]) sendBuffer;
			boolean[] bl_newBuffer = new boolean[dispSeqLen * count];
			System.arraycopy(bl_buffer, offset, bl_newBuffer, 0, dispSeqLen * count);
			msg.addData(bl_newBuffer);
			break;
		}

		if (msgHandle.getTask().isMaster()) { // Master of replica
			// Get IP:Port of Destination
			RankTable commTable = myGroup.__getCommTable();
			MapRankTable mapCommTable = myGroup.__getMapCommTable();
			ArrayList<Integer> dstRIL = mapCommTable.getRankInListByRank(dest);
			int numDest = dstRIL.size();

			long start_sendtime;
			long current_sendtime;
			for (int i = 0; i < numDest; i++) {
				int ril = dstRIL.get(i).intValue();
				start_sendtime = System.currentTimeMillis();
				while (commTable.isAlive(ril)) {
					try {
						msgHandle.getTask().sendMessage(commTable.getTaskID(ril), msg);
						break;
					} catch (Exception e) {
						current_sendtime = System.currentTimeMillis();
						if (((int) (current_sendtime - start_sendtime)) < waitingTime) {
							try {
								Thread.sleep(1000);
							} catch (Exception ie) {
							}
						} else {
							getMessageHandler().getTask().exit("** [Error] could not connect to " + commTable.getTaskID(ril) + ". Maybe a firewall blocks connection.");
							getMessageHandler().getTask().isTerminated();
						}
					}
				}
			}

			// Update status to other replicas
			UpdateStatusMessage updateMsg = new UpdateStatusMessage(messageID);
			int repRIL;
			int realRepRIL;
			msgHandle.getTask().getMPIDebug().println(MPIDebug.SYSTEM - 5, "Strating sync to replica " + numReplica);
			for (int i = 0; i < numReplica; i++) {
				repRIL = myReplica[i];
				realRepRIL = mapCommTable.getRankInList(myReplica[i]);
				if (repRIL != myGroup.RankInList()) {
					start_sendtime = System.currentTimeMillis();
					while (commTable.isAlive(realRepRIL)) {
						try {
							msgHandle.getTask().getMPIDebug().println(MPIDebug.SYSTEM - 5, "message sent to " + commTable.getTaskID(realRepRIL));
							msgHandle.getTask().sendMessage(commTable.getTaskID(realRepRIL), updateMsg);
							break;
						} catch (Exception e) {
							current_sendtime = System.currentTimeMillis();
							if (((int) (current_sendtime - start_sendtime)) < waitingTime) {
								try {
									Thread.sleep(1000);
								} catch (Exception ie) {
								}
							} else {
								getMessageHandler().getTask().exit("** [Error] could not connect to replica " + commTable.getTaskID(realRepRIL) + ". Maybe a firewall blocks connection.");
								getMessageHandler().getTask().isTerminated();
							}
						}
					}
				}
			}
			msgHandle.getTask().getMPIDebug().println(MPIDebug.SYSTEM - 5, "Sync done");
		} else {
			int logIndex;
			synchronized (sendLog) {
				logIndex = sendLog.isExist(messageID);

				if (logIndex != -1) {
					// master already done sending this message
					// just remove this messageID out of log
					sendLog.remove(messageID);
				} else {
					// master is not done sending this message yet
					// so backup this message in buffer in case master failed
					// this process can resend this message
					sendLog.add(messageID);
					SendBufferInformation backup = new SendBufferInformation(messageID, Rank(), dest, msg, tag);
					synchronized (backupMessage) {
						backupMessage.add(backup);
					}
				}
			}
		}

		return count; // return value

	}

	/**
	 * Begins a non-blocking receive.
	 * 
	 * @param recvBuffer
	 *            receive buffer array
	 * @param offset
	 *            initial offset in receive buffer
	 * @param count
	 *            number of items to receive
	 * @param datatype
	 *            datatype of received items
	 * @param src
	 *            rank of source
	 * @param tag
	 *            message tag
	 */
	public Request Irecv(Object recvBuffer, int offset, int count, Datatype datatype, int src, int tag) {

		if (Rank() == MPI.UNDEFINED) {
			return null;
		}
		Request reqret = new Request(msgHandle, recvBuffer, offset, count, datatype, src, tag, Rank());
		return reqret;
	}

	public Status Sendrecv(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, int dest, int sendTag, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType, int source, int recvTag) {
		Request req = Irecv(recvBuffer, recvOffset, recvCount, recvType, source, recvTag);
		Send(sendBuffer, sendOffset, sendCount, sendType, dest, sendTag);

		return req.Wait(); // return status
	}

	/**
	 * Basic (blocking) receive.
	 * 
	 * @param recvBuffer
	 *            receive buffer array
	 * @param offset
	 *            initial offset in receive buffer
	 * @param count
	 *            number of items to receive
	 * @param datatype
	 *            datatype of received items
	 * @param src
	 *            rank of source
	 * @param tag
	 *            message tag
	 */

	public Status Recv(Object recvBuffer, int offset, int count, Datatype datatype, int src, int tag) {
		if (Rank() == MPI.UNDEFINED) {
			return null;
		}
		Object data;
		IStatus istatus = new IStatus();
		Status status = null;
		msgHandle.getTask().getMPIDebug().println(MPIDebug.SYSTEM, "start Recv from " + src + " to " + Rank() + " with " + tag + " " + msgHandle.getSequenceFrom()[src]);
		long time = System.currentTimeMillis();
		int c = 2;
		while (true) {
			long newTime = System.currentTimeMillis();
			msgHandle.getTask().isTerminated();
			data = msgHandle.getDataFromBuffer(src, Rank(), tag, istatus);
			if (data != null) {
				status = new Status(istatus.MPI_SOURCE(), istatus.MPI_TAG(), 0);
				break;
			} else {
				try {
					msgHandle.getTask().wait(100);
				} catch (Exception e) {
				}
			}
			if (newTime > time + 30 * 1000 * c) {
				c++;
				time = newTime;
				msgHandle.getTask().getMPIDebug().println(MPIDebug.SYSTEM + 20, "stuck : start Recv from " + src + " to " + Rank() + " with " + tag + " " + msgHandle.getSequenceFrom()[src] + " buffer = " + msgHandle.messageBuffer.size());
				if (c > 3) {
					for (int j = 0; j < msgHandle.messageBuffer.size(); j++) {
						DataMessage tmp2 = msgHandle.messageBuffer.get(j);
						msgHandle.getTask().getMPIDebug().println(MPIDebug.SYSTEM + 20,
								"remaining getDataFromBuffer from rank " + tmp2.getFromRank() + " (to " + tmp2.getToRank() + ") sequence = " + tmp2.getSequence() + " tag " + tmp2.getTag() + " limit was " + msgHandle.getSequenceFrom()[tmp2.getFromRank()]);
					}
				}
			}
		}
		int baseType = datatype.getBaseType();
		int length;
		if (baseType == Datatype.OBJECT) {
			Object_Deserialize(recvBuffer, (byte[]) data, offset, count, datatype);
			length = 1;

		} else {
			int dispSeqLen = datatype.getDisplacementSequence().length;
			getMessageHandler().getTask().getMPIDebug().println(MPIDebug.SYSTEM - 1, "count =? " + dispSeqLen + ", " + count + " type = " + datatype.getBaseType());
			length = copyBufferCheck(data, 0, recvBuffer, offset, dispSeqLen * count, datatype);
		}
		status.setLength(length);
		msgHandle.removeDataFromBuffer(src, Rank(), tag);
		return status;
	}

	private String getMessageID(int src, int dest, int tag) {
		int iNumSent;
		String midKey = src + "_" + dest + "_" + tag;
		String strNumSent = midLog.getProperty(midKey);
		if (strNumSent == null) {
			iNumSent = 0;
			// Add my messageID to hashtable
			midLog.setProperty(midKey, "1");
		} else {
			iNumSent = Integer.parseInt(strNumSent);
			// Remove messageID from hashtable
			midLog.remove(midKey);
			// Add new value from messageID to hastable;
			midLog.setProperty(midKey, Integer.toString(iNumSent + 1));
		}
		return commID + "_" + midKey + "_" + iNumSent;
	}

	protected int copyBuffer(Object srcBuffer, int srcOffset, Object dstBuffer, int dstOffset, int count, Datatype datatype) {
		int dispSeqLen = datatype.getDisplacementSequence().length;
		switch (datatype.getBaseType()) {
		case Datatype.OBJECT:
			Object[] obj_src = (Object[]) srcBuffer;
			Object[] obj_dst = (Object[]) dstBuffer;
			for (int i = 0; i < count; i++) {
				obj_dst[i + srcOffset] = obj_src[i + dstOffset];
			}
			break;

		case Datatype.STRING:
			String[] str_src = (String[]) srcBuffer;
			String[] str_dst = (String[]) dstBuffer;
			for (int i = 0; i < count; i++) {
				str_dst[i + srcOffset] = str_src[i + dstOffset];
			}
			break;
		case Datatype.BYTE:
			byte[] b_src = (byte[]) srcBuffer;
			byte[] b_dst = (byte[]) dstBuffer;
			System.arraycopy(b_src, srcOffset, b_dst, dstOffset, dispSeqLen * count);
			break;
		case Datatype.CHAR:
			char[] ch_src = (char[]) srcBuffer;
			char[] ch_dst = (char[]) dstBuffer;
			System.arraycopy(ch_src, srcOffset, ch_dst, dstOffset, dispSeqLen * count);
			break;
		case Datatype.SHORT:
			short[] sh_src = (short[]) srcBuffer;
			short[] sh_dst = (short[]) dstBuffer;
			System.arraycopy(sh_src, srcOffset, sh_dst, dstOffset, dispSeqLen * count);
			break;
		case Datatype.INT:
			int[] i_src = (int[]) srcBuffer;
			int[] i_dst = (int[]) dstBuffer;
			System.arraycopy(i_src, srcOffset, i_dst, dstOffset, dispSeqLen * count);
			break;
		case Datatype.LONG:
			long[] l_src = (long[]) srcBuffer;
			long[] l_dst = (long[]) dstBuffer;
			System.arraycopy(l_src, srcOffset, l_dst, dstOffset, dispSeqLen * count);
			break;
		case Datatype.FLOAT:
			float[] f_src = (float[]) srcBuffer;
			float[] f_dst = (float[]) dstBuffer;
			System.arraycopy(f_src, srcOffset, f_dst, dstOffset, dispSeqLen * count);
			break;
		case Datatype.DOUBLE:
			double[] d_src = (double[]) srcBuffer;
			double[] d_dst = (double[]) dstBuffer;
			System.arraycopy(d_src, srcOffset, d_dst, dstOffset, dispSeqLen * count);
			break;
		case Datatype.BOOLEAN:
			boolean[] bl_src = (boolean[]) srcBuffer;
			boolean[] bl_dst = (boolean[]) dstBuffer;
			System.arraycopy(bl_src, srcOffset, bl_dst, dstOffset, dispSeqLen * count);
			break;

		}

		// TODO: CHECK HERE
		return (count * datatype.getBaseSize());
	}

	protected int copyBufferCheck(Object srcBuffer, int srcOffset, Object dstBuffer, int dstOffset, int count, Datatype datatype) {
		int dispSeqLen = datatype.getDisplacementSequence().length;
		try {
			switch (datatype.getBaseType()) {
			case Datatype.OBJECT:
				Object[] obj_src = (Object[]) srcBuffer;
				Object[] obj_dst = (Object[]) dstBuffer;
				if (obj_src.length > count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + obj_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = obj_src.length;
				}

				for (int i = 0; i < count; i++) {
					obj_dst[i + dstOffset] = obj_src[i + srcOffset];
				}
				break;

			case Datatype.STRING:
				String[] str_src = (String[]) srcBuffer;
				String[] str_dst = (String[]) dstBuffer;
				if (str_src.length > count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + str_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = str_src.length;
				}

				for (int i = 0; i < count; i++) {
					str_dst[i + dstOffset] = str_src[i + srcOffset];
				}
				break;

			case Datatype.BYTE:
				byte[] b_src = (byte[]) srcBuffer;
				byte[] b_dst = (byte[]) dstBuffer;
				if (b_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size is bigger than receiving message size");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = b_src.length;
				}
				System.arraycopy(b_src, srcOffset, b_dst, dstOffset, count);
				break;
			case Datatype.CHAR:
				char[] ch_src = (char[]) srcBuffer;
				char[] ch_dst = (char[]) dstBuffer;
				if (ch_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + ch_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = ch_src.length;
				}
				System.arraycopy(ch_src, srcOffset, ch_dst, dstOffset, count);
				break;
			case Datatype.SHORT:
				short[] sh_src = (short[]) srcBuffer;
				short[] sh_dst = (short[]) dstBuffer;
				if (sh_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + sh_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = sh_src.length;
				}
				System.arraycopy(sh_src, srcOffset, sh_dst, dstOffset, count);
				break;
			case Datatype.INT:
				int[] i_src = (int[]) srcBuffer;
				int[] i_dst = (int[]) dstBuffer;
				if (i_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + i_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = i_src.length;
				}
				System.arraycopy(i_src, srcOffset, i_dst, dstOffset, count);
				break;
			case Datatype.LONG:
				long[] l_src = (long[]) srcBuffer;
				long[] l_dst = (long[]) dstBuffer;
				if (l_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + l_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = l_src.length;
				}
				System.arraycopy(l_src, srcOffset, l_dst, dstOffset, count);
				break;
			case Datatype.FLOAT:
				float[] f_src = (float[]) srcBuffer;
				float[] f_dst = (float[]) dstBuffer;
				if (f_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + f_src.length + ") is bigger than receiving message size(" + count + ")");
					return -1;
				} else {
					count = f_src.length;
				}
				System.arraycopy(f_src, srcOffset, f_dst, dstOffset, count);
				break;
			case Datatype.DOUBLE:
				double[] d_src = (double[]) srcBuffer;
				double[] d_dst = (double[]) dstBuffer;
				if (d_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + d_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = d_src.length;
				}
				System.arraycopy(d_src, srcOffset, d_dst, dstOffset, count);
				break;
			case Datatype.BOOLEAN:
				boolean[] bl_src = (boolean[]) srcBuffer;
				boolean[] bl_dst = (boolean[]) dstBuffer;
				if (bl_src.length > dispSeqLen * count) {
					getMessageHandler().getTask().exit("** [Error]: sending message size(" + bl_src.length + ") is bigger than receiving message size(" + count + ")");
					getMessageHandler().getTask().isTerminated();
				} else {
					count = bl_src.length;
				}
				System.arraycopy(bl_src, srcOffset, bl_dst, dstOffset, count);
				break;

			}
		} catch (Exception ex) {
			getMessageHandler().getTask().getMPIDebug().println(MPIDebug.SYSTEM - 1, "ERRRRRRRRRRRRRRROR with last message");
			getMessageHandler().getTask().getMPIDebug().println(MPIDebug.SYSTEM - 1, "type " + srcBuffer.getClass());
			throw new RuntimeException(ex);
		}
		return (count * datatype.getBaseSize());
	}

	public static byte[] Object_Serialize(Object buf, int offset, int count, Datatype type) {
		byte[] byte_buf;
		Object buf_els[];
		ByteArrayOutputStream o;
		ObjectOutputStream out;

		if (type.baseType == Datatype.OBJECT) {
			buf_els = (Object[]) buf;
			try {
				o = new ByteArrayOutputStream();
				out = new ObjectOutputStream(o);
				for (int i = 0; i < count; i++) {
					out.writeObject(buf_els[offset + i]);
					out.flush();
				}
				out.close();
				byte_buf = o.toByteArray();
			} catch (Exception ex) {
				ex.printStackTrace();
				byte_buf = null;
			}
			return byte_buf;

		}
		return null;
	}

	public static void Object_Deserialize(Object buf, byte[] byte_buf, int offset, int count, Datatype type) {
		if (type.baseType == Datatype.OBJECT) {
			Object buf_els[] = (Object[]) buf;
			try {
				ByteArrayInputStream in = new ByteArrayInputStream(byte_buf);
				ObjectInputStream s = new ObjectInputStream(in);
				for (int i = 0; i < count; i++) {
					buf_els[offset + i] = s.readObject();
				}
				s.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	MessageHandler getMessageHandler() {
		return msgHandle;
	}
}
