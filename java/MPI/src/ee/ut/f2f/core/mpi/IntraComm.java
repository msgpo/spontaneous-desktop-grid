package ee.ut.f2f.core.mpi;

import java.util.ArrayList;
import java.util.List;

import ee.ut.f2f.core.mpi.common.MapRankTable;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.internal.BinomialTree;
import ee.ut.f2f.core.mpi.internal.MessageHandler;
import ee.ut.f2f.core.mpi.internal.ProcTree;

/**
 * Collective communication class
 */
public class IntraComm extends Comm {
	private int systemTAG = 100000; // start from 100,000

	/**
	 * Internal use
	 */
	public IntraComm(MessageHandler msgHandle, RankTable rankTable, int rank, int rankInList, int numRank, MapRankTable mapRankTable) {
		super(msgHandle, rankTable, rank, rankInList, numRank, mapRankTable);
	}

	/**
	 * Create a new collective communicator by group
	 * 
	 * @param group
	 *            Group
	 */
	public IntraComm(Group group) {
		super(group);
	}

	/**
	 * Create a new intra-communicator
	 * 
	 * @param group
	 *            Group
	 * @return a new inter-communicator
	 */
	public IntraComm Create(Group group) {
		IntraComm newComm = new IntraComm(group);
		return newComm;
	}

	/**
	 * Barrier Tree implementation : use MPJExpress code for a fair comparison
	 * 
	 */
	private void BarrierTreeImpl() {
		ProcTree procTree = new ProcTree();
		procTree.buildTree(Rank(), Size());
		int offset = 0;
		Datatype type = MPI.BYTE;
		byte[] dummy = new byte[1];
		int count = 1;
		// ------------------anti-bcast-------------------
		getMessageHandler().getTask().getMPIDebug().println(MPIDebug.SYSTEM, "BarrierTree start " + procTree.isRoot + " " + procTree.child.length + " " + procTree.numChildren + " " + procTree.parent + " " + systemTAG);
		if (procTree.isRoot) {
			for (int i = 0; i < procTree.child.length; i++) {
				if (procTree.child[i] != -1) {
					Recv(dummy, offset, count, type, procTree.child[i], systemTAG - procTree.child[i]);
				}
			}
		} else {
			for (int i = 0; i < procTree.child.length; i++) {
				if (procTree.child[i] != -1) {
					Recv(dummy, offset, count, type, procTree.child[i], systemTAG - procTree.child[i]);
				}
			}
			Send(dummy, offset, count, type, procTree.parent, systemTAG - Rank());
		}
		// ------------------bcast-------------------
		if (procTree.isRoot) {
			for (int i = 0; i < procTree.child.length; i++) {
				if (procTree.child[i] != -1) {
					Send(dummy, offset, count, type, procTree.child[i], systemTAG - procTree.child[i]);
				}
			}
		} else {
			Recv(dummy, offset, count, type, procTree.parent, systemTAG - Rank());
			for (int i = 0; i < procTree.child.length; i++) {
				if (procTree.child[i] != -1) {
					Send(dummy, offset, count, type, procTree.child[i], systemTAG - procTree.child[i]);
				}
			}
		}
		getMessageHandler().getTask().getMPIDebug().println(MPIDebug.SYSTEM, "BarrierTree end");
	}

	/**
	 * Synchronize MPI processes
	 */
	public void Barrier() {
		BarrierTreeImpl();
	}

	/**
	 * Broadcast a message to all MPI processes
	 * 
	 * @param buffer
	 *            Send object
	 * @param offset
	 *            Offset of send object
	 * @param count
	 *            Number of elements
	 * @param datatype
	 *            Type of send object
	 * @param root
	 *            MPI Rank of root node
	 */
	public void Bcast(Object buffer, int offset, int count, Datatype datatype, int root) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		BcastBinomial(buffer, offset, count, datatype, root);
	}

	/**
	 * Broadcast a message to all MPI processes with binomial tree method
	 * 
	 * @param buffer
	 *            Send object
	 * @param offset
	 *            Offset of send object
	 * @param count
	 *            Number of elements
	 * @param datatype
	 *            Type of send object
	 * @param root
	 *            MPI Rank of root node
	 */
	private void BcastBinomial(Object buffer, int offset, int count, Datatype datatype, int root) {
		int numNode = Size();
		BinomialTree bt = new BinomialTree(numNode);
		int maxDegree = bt.getMaxDegree();
		int[] children = new int[maxDegree + 1];
		int visualRank = Rank() - root;
		// Rotate Rank
		if (visualRank < 0) {
			visualRank += Size();
		}
		int parent = bt.getParentChildrenInverse(visualRank, children);
		// Recv from Parent
		// If parent == -1, then it's a root node
		if (parent != -1) {
			// rotate tree for each root != 0
			parent = (parent + root) % numNode;
			Recv(buffer, offset, count, datatype, parent, systemTAG);
		}
		// Send to Children
		int i = 0;
		while (true) {
			if (children[i] == -1)
				break;

			Send(buffer, offset, count, datatype, ((children[i] + root) % numNode), systemTAG);
			i++;
		}
		systemTAG++;
	}

	/**
	 * MPI collective operation reduce from all MPI processes (Binomail tree by default)
	 * 
	 * @param sendBuffer
	 *            Send object
	 * @param sendOffset
	 *            Send object offset
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param count
	 *            Number of elements
	 * @param datatype
	 *            MPI datatype
	 * @param op
	 *            Operation used in reduce
	 * @param root
	 *            MPI rank of root node which maintain the result
	 */
	public void Reduce(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Datatype datatype, Op op, int root) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		if (op.isCommute()) {
			ReduceBinomial(sendBuffer, sendOffset, recvBuffer, recvOffset, count, datatype, op, root);
		} else {
			ReduceFlat(sendBuffer, sendOffset, recvBuffer, recvOffset, count, datatype, op, root);
		}
	}

	/**
	 * MPI collective operation reduce from all MPI processes using binomial tree method
	 * 
	 * @param sendBuffer
	 *            Send object
	 * @param sendOffset
	 *            Send object offset
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param count
	 *            Number of elements
	 * @param datatype
	 *            MPI datatype
	 * @param op
	 *            Operation used in reduce
	 * @param root
	 *            MPI rank of root node which maintain the result
	 */
	private void ReduceBinomial(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Datatype datatype, Op op, int root) {
		int numNode = Size();
		BinomialTree bt = new BinomialTree(numNode);
		int maxDegree = bt.getMaxDegree();
		int[] children = new int[maxDegree + 1];
		int visualRank = Rank() - root;
		// Rotate Rank
		if (visualRank < 0) {
			visualRank += Size();
		}
		int parent = bt.getParentChildren(visualRank, children);
		int baseType = datatype.getBaseType();
		// Convert visualRank to real Rank
		if (parent != -1) {
			parent = (parent + root) % Size();
		}
		int numChild = children.length;
		for (int i = 0; i < numChild; i++) {
			if (children[i] == -1) {
				break;
			}
			children[i] = (children[i] + root) % Size();
		}
		Object result = null;
		Object tmpRecv = null;
		int dispSeqLen = datatype.getDisplacementSequence().length;
		// Copy sendBuffer to result buffer
		switch (baseType) {
		case Datatype.BYTE:
			byte[] b_send = (byte[]) sendBuffer;
			byte[] b_tmpBuffer = new byte[dispSeqLen * count];
			byte[] b_tmpRecv = new byte[dispSeqLen * count];
			System.arraycopy(b_send, sendOffset, b_tmpBuffer, 0, dispSeqLen * count);
			result = b_tmpBuffer;
			tmpRecv = b_tmpRecv;
			break;
		case Datatype.CHAR:
			char[] ch_send = (char[]) sendBuffer;
			char[] ch_tmpBuffer = new char[dispSeqLen * count];
			char[] ch_tmpRecv = new char[dispSeqLen * count];
			System.arraycopy(ch_send, sendOffset, ch_tmpBuffer, 0, dispSeqLen * count);
			result = ch_tmpBuffer;
			tmpRecv = ch_tmpRecv;
			break;
		case Datatype.SHORT:
			short[] sh_send = (short[]) sendBuffer;
			short[] sh_tmpBuffer = new short[dispSeqLen * count];
			short[] sh_tmpRecv = new short[dispSeqLen * count];
			System.arraycopy(sh_send, sendOffset, sh_tmpBuffer, 0, dispSeqLen * count);
			result = sh_tmpBuffer;
			tmpRecv = sh_tmpRecv;
			break;
		case Datatype.INT:
			int[] i_send = (int[]) sendBuffer;
			int[] i_tmpBuffer = new int[dispSeqLen * count];
			int[] i_tmpRecv = new int[dispSeqLen * count];
			System.arraycopy(i_send, sendOffset, i_tmpBuffer, 0, dispSeqLen * count);
			result = i_tmpBuffer;
			tmpRecv = i_tmpRecv;
			break;
		case Datatype.LONG:
			long[] l_send = (long[]) sendBuffer;
			long[] l_tmpBuffer = new long[dispSeqLen * count];
			long[] l_tmpRecv = new long[dispSeqLen * count];
			System.arraycopy(l_send, sendOffset, l_tmpBuffer, 0, dispSeqLen * count);
			result = l_tmpBuffer;
			tmpRecv = l_tmpRecv;
			break;
		case Datatype.FLOAT:
			float[] f_send = (float[]) sendBuffer;
			float[] f_tmpBuffer = new float[dispSeqLen * count];
			float[] f_tmpRecv = new float[dispSeqLen * count];
			System.arraycopy(f_send, sendOffset, f_tmpBuffer, 0, dispSeqLen * count);
			result = f_tmpBuffer;
			tmpRecv = f_tmpRecv;
			break;
		case Datatype.DOUBLE:
			double[] d_send = (double[]) sendBuffer;
			double[] d_tmpBuffer = new double[dispSeqLen * count];
			double[] d_tmpRecv = new double[dispSeqLen * count];
			System.arraycopy(d_send, sendOffset, d_tmpBuffer, 0, dispSeqLen * count);
			result = d_tmpBuffer;
			tmpRecv = d_tmpRecv;
			break;
		}
		// Recv from Childrens
		int i = 0;
		while (true) {
			if (children[i] == -1)
				break;
			// recv from a child to recvOffer
			Recv(tmpRecv, 0, count, datatype, children[i], systemTAG);
			// do operation between recvBuffer and the result buffer
			// op.Call(recvBuffer, recvOffset, result, 0, count, datatype);
			op.Call(tmpRecv, 0, result, 0, count, datatype);
			i++;
		}
		// Send a result buffer to Parent
		if (parent != -1) {
			Send(result, 0, count, datatype, parent, systemTAG);
		} else {
			// copy result to recvBuffer
			copyBuffer(result, 0, recvBuffer, recvOffset, count, datatype);
		}
		systemTAG++;
	}

	/**
	 * MPI collective operation reduce from all MPI processes using flat tree method
	 * 
	 * @param sendBuffer
	 *            Send object
	 * @param sendOffset
	 *            Send object offset
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param count
	 *            Number of elements
	 * @param datatype
	 *            MPI datatype
	 * @param op
	 *            Operation used in reduce
	 * @param root
	 *            MPI rank of root node which maintain the result
	 */
	private void ReduceFlat(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Datatype datatype, Op op, int root) {
		if (Rank() != root) {
			// If i'm not root, send my sendBuffer to root
			Send(sendBuffer, sendOffset, count, datatype, root, systemTAG);
		} else {
			// i'm root wait for message and do OP
			int baseType = datatype.getBaseType();
			switch (baseType) {
			case Datatype.BYTE:
				byte[] b_send = (byte[]) sendBuffer;
				byte[] b_recv = (byte[]) recvBuffer;
				byte[] b_tmpBuffer = new byte[count * datatype.Extent()];
				for (int i = 0; i < Size(); i++) {
					if (i != root) {
						if (op.isCommute()) {
							Recv(b_tmpBuffer, 0, count, datatype, i, systemTAG);
							// later change i to MPI_ANY_SRC
						} else {
							Recv(b_tmpBuffer, 0, count, datatype, i, systemTAG);
						}

						if (i == 0) {
							for (int j = 0; j < count; i++) {
								b_recv[j] = b_tmpBuffer[j];
							}
						} else {
							op.Call(b_tmpBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					} else {
						if (i == 0) {
							for (int j = 0; j < count; j++) {
								b_recv[j] = b_send[j];
							}
						} else {
							op.Call(sendBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					}
				}
				break;
			case Datatype.CHAR:
				// Put root value in recv
				char[] ch_send = (char[]) sendBuffer;
				char[] ch_recv = (char[]) recvBuffer;
				char[] ch_tmpBuffer = new char[count * datatype.Extent()];
				for (int i = 0; i < Size(); i++) {
					if (i != root) {
						if (op.isCommute()) {
							Recv(ch_tmpBuffer, 0, count, datatype, i, systemTAG);
							// later change i to MPI_ANY_SRC
						} else {
							Recv(ch_tmpBuffer, 0, count, datatype, i, systemTAG);
						}

						if (i == 0) {
							for (int j = 0; j < count; j++)
								ch_recv[j] = ch_tmpBuffer[j];
						} else {
							op.Call(ch_tmpBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					} else {
						if (i == 0) {
							for (int j = 0; j < count; j++)
								ch_recv[j] = ch_send[j];
						} else {
							op.Call(sendBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					}
				}
				break;
			case Datatype.SHORT:
				short[] sh_send = (short[]) sendBuffer;
				short[] sh_recv = (short[]) recvBuffer;
				short[] sh_tmpBuffer = new short[count * datatype.Extent()];
				for (int i = 0; i < Size(); i++) {
					if (i != root) {
						if (op.isCommute()) {
							Recv(sh_tmpBuffer, 0, count, datatype, i, systemTAG);
							// later change i to MPI_ANY_SRC
						} else {
							Recv(sh_tmpBuffer, 0, count, datatype, i, systemTAG);
						}

						if (i == 0) {
							for (int j = 0; j < count; j++)
								sh_recv[j] = sh_tmpBuffer[j];
						} else {
							op.Call(sh_tmpBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					} else {
						if (i == 0) {
							for (int j = 0; j < count; j++)
								sh_recv[j] = sh_send[j];
						} else {
							op.Call(sendBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					}
				}
				break;
			case Datatype.INT:
				int[] i_send = (int[]) sendBuffer;
				int[] i_recv = (int[]) recvBuffer;
				int[] i_tmpBuffer = new int[count * datatype.Extent()];
				for (int i = 0; i < Size(); i++) {
					if (i != root) {
						if (op.isCommute()) {
							Recv(i_tmpBuffer, 0, count, datatype, i, systemTAG);
							// later change i to MPI_ANY_SRC
						} else {
							Recv(i_tmpBuffer, 0, count, datatype, i, systemTAG);
						}

						if (i == 0) {
							for (int j = 0; j < count; j++)
								i_recv[j] = i_tmpBuffer[j];
						} else {
							op.Call(i_tmpBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					} else {

						if (i == 0) {
							for (int j = 0; j < count; j++)
								i_recv[j] = i_send[j];
						} else {
							op.Call(sendBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					}
				}
				break;
			case Datatype.LONG:
				long[] l_send = (long[]) sendBuffer;
				long[] l_recv = (long[]) recvBuffer;
				long[] l_tmpBuffer = new long[count * datatype.Extent()];
				for (int i = 0; i < Size(); i++) {
					if (i != root) {
						if (op.isCommute()) {
							Recv(l_tmpBuffer, 0, count, datatype, i, systemTAG);
							// later change i to MPI_ANY_SRC
						} else {
							Recv(l_tmpBuffer, 0, count, datatype, i, systemTAG);
						}

						if (i == 0) {
							for (int j = 0; j < count; j++)
								l_recv[j] = l_tmpBuffer[j];
						} else {
							op.Call(l_tmpBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					} else {

						if (i == 0) {
							for (int j = 0; j < count; j++)
								l_recv[j] = l_send[j];
						} else {
							op.Call(sendBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					}
				}
				break;
			case Datatype.FLOAT:
				float[] f_send = (float[]) sendBuffer;
				float[] f_recv = (float[]) recvBuffer;
				float[] f_tmpBuffer = new float[count * datatype.Extent()];
				for (int i = 0; i < Size(); i++) {
					if (i != root) {
						if (op.isCommute()) {
							Recv(f_tmpBuffer, 0, count, datatype, i, systemTAG);
							// later change i to MPI_ANY_SRC
						} else {
							Recv(f_tmpBuffer, 0, count, datatype, i, systemTAG);
						}

						if (i == 0) {
							for (int j = 0; j < count; j++)
								f_recv[j] = f_tmpBuffer[j];
						} else {
							op.Call(f_tmpBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					} else {
						if (i == 0) {
							for (int j = 0; j < count; j++)
								f_recv[j] = f_send[j];
						} else {
							op.Call(sendBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					}
				}
				break;
			case Datatype.DOUBLE:
				double[] d_send = (double[]) sendBuffer;
				double[] d_recv = (double[]) recvBuffer;
				double[] d_tmpBuffer = new double[count * datatype.Extent()];
				for (int i = 0; i < Size(); i++) {
					if (i != root) {
						if (op.isCommute()) {
							Recv(d_tmpBuffer, 0, count, datatype, i, systemTAG);
							// later change i to MPI_ANY_SRC
						} else {
							Recv(d_tmpBuffer, 0, count, datatype, i, systemTAG);
						}

						if (i == 0) {
							for (int j = 0; j < count; j++)
								d_recv[j] = d_tmpBuffer[j];
						} else {
							op.Call(d_tmpBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					} else {
						if (i == 0) {
							for (int j = 0; j < count; j++)
								d_recv[j] = d_send[j];
						} else {
							op.Call(sendBuffer, 0, recvBuffer, recvOffset, count, datatype);
						}
					}
				}
				break;
			}
		}
		systemTAG++;
	}

	/**
	 * Reduce the result and then broadcast it to all MPI processes
	 * 
	 * @param sendBuffer
	 *            Send object
	 * @param sendOffset
	 *            Send object offset
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param count
	 *            Number of elements
	 * @param datatype
	 *            MPI datatype
	 * @param op
	 *            Operation used in reduce
	 */
	public void Allreduce(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Datatype datatype, Op op) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		// Call Reduce by using process rank 0 as root
		Reduce(sendBuffer, sendOffset, recvBuffer, recvOffset, count, datatype, op, 0);
		// Broadcast a result from reduce operation to all ranks
		Bcast(recvBuffer, recvOffset, count, datatype, 0);
	}

	/**
	 * Reduce the result and then broadcast it to all MPI processes with variable size
	 * 
	 * @param sendBuffer
	 *            Send object
	 * @param sendOffset
	 *            Send object offset
	 * @param sendCount
	 *            Number of elements for sending
	 * @param sdispls
	 *            Displacement of send object
	 * @param sendType
	 *            MPI datatype of send object
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param recvCount
	 *            Number of elements for receiving
	 * @param rdispls
	 *            Displacement of receive object
	 * @param recvType
	 *            MPI datatype of receive object
	 */
	public void Alltoallv(Object sendBuffer, int sendOffset, int[] sendCount, int[] sdispls, Datatype sendType, Object recvBuffer, int recvOffset, int[] recvCount, int[] rdispls, Datatype recvType) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		AlltoallvAsynRotate(sendBuffer, sendOffset, sendCount, sdispls, sendType, recvBuffer, recvOffset, recvCount, rdispls, recvType);
	}

	private void AlltoallvAsynRotate(Object sendBuffer, int sendOffset, int[] sendCount, int[] sdispls, Datatype sendType, Object recvBuffer, int recvOffset, int[] recvCount, int[] rdispls, Datatype recvType) {
		int myRank = Rank();
		int mySize = Size();
		// Send to (rank+i)%size
		int sendTo;
		for (int i = 1; i < mySize; i++) {
			sendTo = (myRank + i) % mySize;
			Send(sendBuffer, sendOffset + sdispls[sendTo], sendCount[sendTo], sendType, sendTo, systemTAG);
		}
		// Recv from (size + (rank-i))%size
		int recvFrom;
		for (int i = 1; i < mySize; i++) {
			recvFrom = (mySize + (myRank - i)) % mySize;
			Recv(recvBuffer, recvOffset + rdispls[recvFrom], recvCount[recvFrom], recvType, recvFrom, systemTAG);
		}
		// Copy sendBuffer to recvBuffer
		copyBuffer(sendBuffer, sendOffset + sdispls[myRank], recvBuffer, recvOffset + rdispls[myRank], recvCount[myRank], recvType);
	}

	/**
	 * Reduce the result and then broadcast it to all MPI processes
	 * 
	 * @param sendBuffer
	 *            Send object
	 * @param sendOffset
	 *            Send object offset
	 * @param sendCount
	 *            Number of elements for sending
	 * @param sendType
	 *            MPI datatype of send object
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param recvCount
	 *            Number of elements for receiving
	 * @param recvType
	 *            MPI datatype of receive object
	 */
	public void Alltoall(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		AlltoallAsynRotate(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, recvType);
	}

	private void AlltoallAsynRotate(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType) {
		int sendOffsetCount;
		int recvOffsetCount;
		int myRank = Rank();
		int mySize = Size();
		int sendTo;
		// Send to (rank+i)%size
		for (int i = 1; i < mySize; i++) {
			sendTo = (myRank + i) % mySize;
			sendOffsetCount = sendTo * sendCount;
			Send(sendBuffer, sendOffset + sendOffsetCount, sendCount, sendType, sendTo, systemTAG);
		}
		// Recv from (size + (rank - i))%size
		int recvFrom;
		for (int i = 1; i < mySize; i++) {
			recvFrom = (mySize + (myRank - i)) % mySize;
			recvOffsetCount = recvFrom * recvCount;
			Recv(recvBuffer, recvOffset + recvOffsetCount, recvCount, recvType, recvFrom, systemTAG);
		}
		// Copy its sendBuffer to recvBuffer
		copyBuffer(sendBuffer, sendOffset + (myRank * sendCount), recvBuffer, recvOffset + (myRank * recvCount), recvCount, recvType);
	}

	/**
	 * Gathers together values from a group of tasks
	 * 
	 * @param sendBuffer
	 *            send object
	 * @param sendOffset
	 *            send object offset
	 * @param sendCount
	 *            number of elements for sending
	 * @param sendType
	 *            MPI datatype of send object
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 * @param recvType
	 *            MPI datatype of receive object
	 * @param root
	 *            node to gather the value
	 */
	public void Gather(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType, int root) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		GatherFlatTree(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, recvType, root);
	}

	private void GatherFlatTree(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype, Object recvbuf, int recvoffset, int recvcount, Datatype recvtype, int root) {
		if (Rank() == root) {
			for (int i = 0; i < Size(); i++) {
				if (i != root) {
					// receive message from others
					Recv(recvbuf, recvoffset + (i * recvcount), recvcount, recvtype, i, systemTAG);
				} else {
					// my message just copy from sendbuf to recvbuf
					copyBuffer(sendbuf, sendoffset, recvbuf, recvoffset + (root * recvcount), recvcount, recvtype);
				}
			}
		} else {
			// I'm not root so I send my buffer to root
			Send(sendbuf, sendoffset, sendcount, sendtype, root, systemTAG);
		}
		systemTAG++;
	}

	/**
	 * Gathers into specified locations from all processes in group
	 * 
	 * @param sendBuffer
	 *            send object
	 * @param sendOffset
	 *            send object offset
	 * @param sendCount
	 *            number of elements in send object
	 * @param sendType
	 *            datatype of send object elements
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            integer array (of length group size) containing the number of elements that are received from each process
	 * @param displs
	 *            integer array (of length group size). Entry i specifies the displacement relative to recvOffset of recvBuffer at which to place the incoming data from process i
	 * @param recvType
	 *            MPI datatype of receive object
	 * @param root
	 *            node to gather the value
	 */
	public void Gatherv(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int[] recvCount, int[] displs, Datatype recvType, int root) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		GathervFlatTree(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, displs, recvType, root);
	}

	private void GathervFlatTree(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype, Object recvbuf, int recvoffset, int[] recvcount, int[] displs, Datatype recvtype, int root) {
		if (Rank() == root) {
			for (int i = 0; i < Size(); i++) {
				if (i != root) {
					// receive message from others
					Recv(recvbuf, recvoffset + displs[i], recvcount[i], recvtype, i, systemTAG);
				} else {
					// my message just copy from sendbuf to recvbuf
					copyBuffer(sendbuf, sendoffset, recvbuf, recvoffset + displs[i], recvcount[i], recvtype);
				}
			}
		} else {
			// I'm not root so I send my buffer to root
			Send(sendbuf, sendoffset, sendcount, sendtype, root, systemTAG);
		}
		systemTAG++;
	}

	/**
	 * Gathers data from all tasks and distribute it to all
	 * 
	 * @param sendBuffer
	 *            send object
	 * @param sendOffset
	 *            send object offset
	 * @param sendCount
	 *            number of elements for sending
	 * @param sendType
	 *            MPI datatype of send object
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 * @param recvType
	 *            MPI datatype of receive object
	 */
	public void Allgather(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		AllGatherSimple(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, recvType);
	}

	private void AllGatherSimple(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType) {
		// Gather to rank 0
		Gather(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, recvType, 0);
		// Bcast
		Bcast(recvBuffer, recvOffset, recvCount * Size(), recvType, 0);
	}

	/**
	 * Gathers data from all tasks and deliver it to all
	 * 
	 * @param sendBuffer
	 *            send object
	 * @param sendOffset
	 *            send object offset
	 * @param sendCount
	 *            number of elements for sending
	 * @param sendType
	 *            MPI datatype of send object
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            integer array (of length group size) containing the number of elements that are received from each process
	 * @param displs
	 *            integer array (of length group size). Entry i specifies the displacement relative to recvOffset of recvBuffer at which to place the incoming data from process i
	 * @param recvType
	 *            MPI datatype of receive object
	 */
	public void Allgatherv(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int[] recvCount, int[] displs, Datatype recvType) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		AllGathervSimple(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, displs, recvType);
	}

	private void AllGathervSimple(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int[] recvCount, int[] displs, Datatype recvType) {
		// Gather to rank 0
		Gatherv(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, displs, recvType, 0);
		// Bcast with size of recvCount
		int counter = displs[displs.length - 1] + recvCount[recvCount.length - 1];
		Bcast(recvBuffer, recvOffset, counter, recvType, 0);
	}

	/**
	 * Sends data from one task to all other tasks in a group
	 * 
	 * @param sendBuffer
	 *            send object
	 * @param sendOffset
	 *            send object offset
	 * @param sendCount
	 *            number of elements for sending
	 * @param sendType
	 *            MPI datatype of send object
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 * @param recvType
	 *            MPI datatype of receive object
	 * @param root
	 *            rank of sending process
	 */
	public void Scatter(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType, int root) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		ScatterSimple(sendBuffer, sendOffset, sendCount, sendType, recvBuffer, recvOffset, recvCount, recvType, root);
	}

	private void ScatterSimple(Object sendBuffer, int sendOffset, int sendCount, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType, int root) {
		int mySize = Size();
		if (Rank() == root) {
			// I'm root so distribute my sendBuffer
			for (int i = 0; i < mySize; i++) {
				if (i != root) {
					Send(sendBuffer, sendOffset + (i * sendCount), sendCount, sendType, i, systemTAG);
				} else {
					copyBuffer(sendBuffer, sendOffset + (i * sendCount), recvBuffer, recvOffset, recvCount, recvType);
				}
			}
		} else {
			// Wait data from root
			Recv(recvBuffer, recvOffset, recvCount, recvType, root, systemTAG);
		}
		systemTAG++;
	}

	/**
	 * Sends a buffer in parts to all tasks in a group
	 * 
	 * @param sendBuffer
	 *            send object
	 * @param sendOffset
	 *            send object offset
	 * @param sendCount
	 *            integer array (of length group size) specifying the number of elements to send to each processor
	 * @param displs
	 *            integer array (of length group size). Entry i specifies the displacement relative to process i
	 * @param sendType
	 *            MPI datatype of send object
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 * @param recvType
	 *            MPI datatype of receive object
	 * @param root
	 *            rank of sending process
	 */
	public void Scatterv(Object sendBuffer, int sendOffset, int[] sendCount, int[] displs, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType, int root) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		ScattervSimple(sendBuffer, sendOffset, sendCount, displs, sendType, recvBuffer, recvOffset, recvCount, recvType, root);
	}

	private void ScattervSimple(Object sendBuffer, int sendOffset, int[] sendCount, int[] displs, Datatype sendType, Object recvBuffer, int recvOffset, int recvCount, Datatype recvType, int root) {
		int mySize = Size();
		if (Rank() == root) {
			// I'm root so distribute my sendBuffer
			for (int i = 0; i < mySize; i++) {
				if (i != root) {
					Send(sendBuffer, sendOffset + displs[i], sendCount[i], sendType, i, systemTAG);
				} else {
					copyBuffer(sendBuffer, sendOffset + displs[i], recvBuffer, recvOffset, recvCount, recvType);
				}
			}
		} else {
			// Wait data from root
			Recv(recvBuffer, recvOffset, recvCount, recvType, root, systemTAG);
		}
		systemTAG++;

	}

	/**
	 * Combines value and scatters the results
	 * 
	 * @param sendBuffer
	 *            send object
	 * @param sendOffset
	 *            send object offset
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            integer array specifying the number of elements in result distributed to each process
	 * @param datatype
	 *            data type of elements of sending object (handle)
	 * @param op
	 *            operation (handle)
	 */
	public void Reduce_scatter(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int[] recvCount, Datatype datatype, Op op) {
		if (Rank() == MPI.UNDEFINED) {
			return;
		}
		Reduce_scatterSimple(sendBuffer, sendOffset, recvBuffer, recvOffset, recvCount, datatype, op);
	}

	private void Reduce_scatterSimple(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int[] recvCount, Datatype datatype, Op op) {
		int mySize = Size();
		int count = 0;

		// calculate total size
		for (int i = 0; i < mySize; i++) {
			count += recvCount[i];
		}
		// construct displs
		int[] displs = new int[mySize];
		displs[0] = 0;
		for (int i = 0; i < mySize - 1; i++) {
			displs[i + 1] = displs[i] + recvCount[i];
		}
		Object tmpBuffer = null;
		switch (datatype.getBaseType()) {
		case Datatype.BYTE:
			byte[] b_buffer = new byte[count];
			tmpBuffer = b_buffer;
			break;
		case Datatype.CHAR:
			char[] c_buffer = new char[count];
			tmpBuffer = c_buffer;
			break;
		case Datatype.SHORT:
			short[] s_buffer = new short[count];
			tmpBuffer = s_buffer;
			break;
		case Datatype.INT:
			int[] i_buffer = new int[count];
			tmpBuffer = i_buffer;
			break;
		case Datatype.LONG:
			long[] l_buffer = new long[count];
			tmpBuffer = l_buffer;
			break;
		case Datatype.FLOAT:
			float[] f_buffer = new float[count];
			tmpBuffer = f_buffer;
			break;
		case Datatype.DOUBLE:
			double[] d_buffer = new double[count];
			tmpBuffer = d_buffer;
			break;

		}
		Reduce(sendBuffer, 0, tmpBuffer, 0, count, datatype, op, 0);
		Scatterv(tmpBuffer, 0, recvCount, displs, datatype, recvBuffer, 0, recvCount[Rank()], datatype, 0);
	}

	/**
	 * Computes the scan (partial reductions) of data on a collection of processes
	 * 
	 * @param sendBuffer
	 *            send buffer
	 * @param sendOffset
	 *            send buffer offset
	 * @param recvBuffer
	 *            receive buffer
	 * @param recvOffset
	 *            receive buffer offset
	 * @param count
	 *            number of elements
	 * @param datatype
	 *            data type
	 * @param op
	 *            operation
	 */
	public void Scan(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Datatype datatype, Op op) {
		if (Rank() == 0) {
			// Copy send buffer to recv buffer
			copyBuffer(sendBuffer, sendOffset, recvBuffer, recvOffset, count, datatype);
		} else {
			// Wait message of rank-1
			Recv(recvBuffer, recvOffset, count, datatype, Rank() - 1, systemTAG);
			// Do operation
			op.Call(sendBuffer, sendOffset, recvBuffer, recvOffset, count, datatype);
		}
		// If it's not a last RANK send "recvbuf" to rank+1
		if (Rank() < Size() - 1) {
			Send(recvBuffer, recvOffset, count, datatype, Rank() + 1, systemTAG);
		}
		systemTAG++;
	}

	public IntraComm Split(int color, int key) {
		int mycolor = color;
		int mykey = key;
		int size = Size();
		int[] colorKey = new int[2];
		colorKey[0] = mycolor;
		colorKey[1] = mykey;
		int[] colorKeyTable = new int[2 * size];
		// Collect all colors and keys (AllReduce is faster)
		Allgather(colorKey, 0, 2, MPI.INT, colorKeyTable, 0, 2, MPI.INT);
		// find key of my color
		List<int[]> keyRanks = new ArrayList<int[]>();
		for (int i = 0; i < size; i++) {
			if (colorKeyTable[2 * i] == mycolor) {
				int[] keyRank = new int[2];
				keyRank[0] = colorKeyTable[(2 * i) + 1];
				keyRank[1] = i;
				keyRanks.add(keyRank);
			}
		}

		// sort by key
		List<int[]> sortedKeyRanks = new ArrayList<int[]>();
		int numKey = keyRanks.size();

		while (numKey != 0) {
			int[] keyRank = keyRanks.get(0);
			int position = 0;
			// search the least value of key
			for (int j = 1; j < numKey; j++) {
				if (keyRank[0] > (keyRanks.get(j))[0]) {
					position = j;
					keyRank = keyRanks.get(j);
				}
			}
			// add key to sortedKeyRanks
			sortedKeyRanks.add(keyRank);
			// delete it from keyRanks
			keyRanks.remove(position);

			numKey = keyRanks.size();
		}
		int memberSize = sortedKeyRanks.size();
		int[] myNewRanks = new int[memberSize];

		for (int i = 0; i < memberSize; i++) {
			myNewRanks[i] = (sortedKeyRanks.get(i))[1];
		}
		Group myNewGroup = Group().Incl(myNewRanks);
		return Create(myNewGroup);
	}
}
