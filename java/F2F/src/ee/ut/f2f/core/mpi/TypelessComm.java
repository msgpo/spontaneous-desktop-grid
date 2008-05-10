package ee.ut.f2f.core.mpi;

import ee.ut.f2f.core.mpi.common.MapRankTable;
import ee.ut.f2f.core.mpi.common.RankTable;
import ee.ut.f2f.core.mpi.internal.MessageHandler;

/**
 * @author Andres Luuk
 *
 * A class for Comm commands that does not need the type of objects sent.
 * This class has a dynamic class type asking function
 */

public class TypelessComm extends IntraComm {

	public TypelessComm(MessageHandler msgHandle, RankTable rankTable, int rank, int rankInList, int numRank, MapRankTable mapRankTable) {
		super(msgHandle, rankTable, rank, rankInList, numRank, mapRankTable);
	}

	public TypelessComm(Group group) {
		super(group);
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
	 * @param dest
	 *            rank of destination
	 * @param tag
	 *            message tag
	 */
	public int Send(Object sendBuffer, int offset, int count, int dest, int tag) {
		return Send(sendBuffer, offset, count, getDatatype(sendBuffer), dest, tag);
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
	 * @param src
	 *            rank of source
	 * @param tag
	 *            message tag
	 */
	public Request Irecv(Object recvBuffer, int offset, int count, int src, int tag) {
		return Irecv(recvBuffer, offset, count, getDatatype(recvBuffer), src, tag);
	}

	public Status Sendrecv(Object sendBuffer, int sendOffset, int sendCount, int dest, int sendTag, Object recvBuffer, int recvOffset, int recvCount, int source, int recvTag) {
		return Sendrecv(sendBuffer, sendOffset, sendCount, getDatatype(sendBuffer), dest, sendTag, recvBuffer, recvOffset, recvCount, getDatatype(recvBuffer), source, recvTag);
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
	 * @param src
	 *            rank of source
	 * @param tag
	 *            message tag
	 */

	public Status Recv(Object recvBuffer, int offset, int count, int src, int tag) {
		return Recv(recvBuffer, offset, count, getDatatype(recvBuffer), src, tag);
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
	 * @param root
	 *            MPI Rank of root node
	 */
	public void Bcast(Object buffer, int offset, int count, int root) {
		Bcast(buffer, offset, count, getDatatype(buffer), root);
	}

	/**
	 * MPI collective operation reduce from all MPI processes
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
	 * @param op
	 *            Operation used in reduce
	 * @param root
	 *            MPI rank of root node which maintain the result
	 */
	public void Reduce(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Op op, int root) {
		Reduce(sendBuffer, sendOffset, recvBuffer, recvOffset, count, getDatatype(sendBuffer, recvBuffer), op, root);
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
	 * @param op
	 *            Operation used in reduce
	 */
	public void Allreduce(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Op op) {
		Allreduce(sendBuffer, sendOffset, recvBuffer, recvOffset, count, getDatatype(sendBuffer, recvBuffer), op);
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
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param recvCount
	 *            Number of elements for receiving
	 * @param rdispls
	 *            Displacement of receive object
	 */
	public void Alltoallv(Object sendBuffer, int sendOffset, int[] sendCount, int[] sdispls, Object recvBuffer, int recvOffset, int[] recvCount, int[] rdispls) {
		Alltoallv(sendBuffer, sendOffset, sendCount, sdispls, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, rdispls, getDatatype(recvBuffer));
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
	 * @param recvBuffer
	 *            Receive object
	 * @param recvOffset
	 *            Receive object offset
	 * @param recvCount
	 *            Number of elements for receiving
	 */
	public void Alltoall(Object sendBuffer, int sendOffset, int sendCount, Object recvBuffer, int recvOffset, int recvCount) {
		Alltoall(sendBuffer, sendOffset, sendCount, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, getDatatype(recvBuffer));
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
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 * @param root
	 *            node to gather the value
	 */
	public void Gather(Object sendBuffer, int sendOffset, int sendCount, Object recvBuffer, int recvOffset, int recvCount, int root) {
		Gather(sendBuffer, sendOffset, sendCount, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, getDatatype(recvBuffer), root);
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
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            integer array (of length group size) containing the number of elements that are received from each process
	 * @param displs
	 *            integer array (of length group size). Entry i specifies the displacement relative to recvOffset of recvBuffer at which to place the incoming data from process i
	 * @param root
	 *            node to gather the value
	 */
	public void Gatherv(Object sendBuffer, int sendOffset, int sendCount, Object recvBuffer, int recvOffset, int[] recvCount, int[] displs, int root) {
		Gatherv(sendBuffer, sendOffset, sendCount, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, displs, getDatatype(recvBuffer), root);
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
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 */
	public void Allgather(Object sendBuffer, int sendOffset, int sendCount, Object recvBuffer, int recvOffset, int recvCount) {
		Allgather(sendBuffer, sendOffset, sendCount, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, getDatatype(recvBuffer));
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
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            integer array (of length group size) containing the number of elements that are received from each process
	 * @param displs
	 *            integer array (of length group size). Entry i specifies the displacement relative to recvOffset of recvBuffer at which to place the incoming data from process i
	 */
	public void Allgatherv(Object sendBuffer, int sendOffset, int sendCount, Object recvBuffer, int recvOffset, int[] recvCount, int[] displs) {
		Allgatherv(sendBuffer, sendOffset, sendCount, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, displs, getDatatype(recvBuffer));
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
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 * @param root
	 *            rank of sending process
	 */
	public void Scatter(Object sendBuffer, int sendOffset, int sendCount, Object recvBuffer, int recvOffset, int recvCount, int root) {
		Scatter(sendBuffer, sendOffset, sendCount, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, getDatatype(recvBuffer), root);
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
	 * @param recvBuffer
	 *            receive object
	 * @param recvOffset
	 *            receive object offset
	 * @param recvCount
	 *            number of elements for receiving
	 * @param root
	 *            rank of sending process
	 */
	public void Scatterv(Object sendBuffer, int sendOffset, int[] sendCount, int[] displs, Object recvBuffer, int recvOffset, int recvCount, int root) {
		Scatterv(sendBuffer, sendOffset, sendCount, displs, getDatatype(sendBuffer), recvBuffer, recvOffset, recvCount, getDatatype(recvBuffer), root);
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
	 * @param op
	 *            operation (handle)
	 */
	public void Reduce_scatter(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int[] recvCount, Op op) {
		Reduce_scatter(sendBuffer, sendOffset, recvBuffer, recvOffset, recvCount, getDatatype(sendBuffer, recvBuffer), op);
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
	 * @param op
	 *            operation
	 */
	public void Scan(Object sendBuffer, int sendOffset, Object recvBuffer, int recvOffset, int count, Op op) {
		Scan(sendBuffer, sendOffset, recvBuffer, recvOffset, count, getDatatype(sendBuffer, recvBuffer), op);
	}

	private Datatype getDatatype(Object buffer) {
		Datatype type = MPI.OBJECT;
		if (buffer instanceof String[]) {
			type = MPI.STRING;
		} else if (buffer instanceof Object[]) {
			type = MPI.OBJECT;
		} else if (buffer instanceof int[]) {
			type = MPI.INT;
		} else if (buffer instanceof byte[]) {
			type = MPI.BYTE;
		} else if (buffer instanceof char[]) {
			type = MPI.CHAR;
		} else if (buffer instanceof short[]) {
			type = MPI.SHORT;
		} else if (buffer instanceof long[]) {
			type = MPI.LONG;
		} else if (buffer instanceof float[]) {
			type = MPI.FLOAT;
		} else if (buffer instanceof double[]) {
			type = MPI.DOUBLE;
		} 
		return type;
	}

	private Datatype getDatatype(Object buffer, Object buffer2) {
		if (buffer2 != null) {
			return getDatatype(buffer2);
		} else {
			return getDatatype(buffer);
		}
	}
}