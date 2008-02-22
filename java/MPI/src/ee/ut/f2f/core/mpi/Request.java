package ee.ut.f2f.core.mpi;

import ee.ut.f2f.core.mpi.internal.IStatus;
import ee.ut.f2f.core.mpi.internal.MessageHandler;

/**
 * The handle of asynchronous communication
 */
public class Request {
	protected MessageHandler msgHandle = null;
	protected Object recvBuffer;
	protected int offset;
	protected int count;
	protected Datatype datatype;
	protected int src;
	protected int tag;
	protected int dst;

	/**
	 * Internal use
	 */
	public Request(MessageHandler msgHandle, Object recvBuffer, int offset, int count, Datatype datatype, int src, int tag, int dst) {
		this.msgHandle = msgHandle;
		this.recvBuffer = recvBuffer;
		this.offset = offset;
		this.count = count;
		this.datatype = datatype;
		this.src = src;
		this.tag = tag;
		this.dst = dst;
	}

	/**
	 * Blocks until all of the operations associated with the active requests in the array have completed.
	 */
	public Status[] Waitall(Request[] requests) {
		int size = requests.length;
		Status[] status = new Status[size];
		for (int i = 0; i < size; i++) {
			status[i] = requests[i].Wait();
		}
		return status;
	}

	/**
	 * Blocks until a waiting asynchronous message is received
	 */
	public Status Wait() {
		Object data;
		IStatus istatus = new IStatus(src, tag, 0); // unknown length, will be
		// set
		Status status = null;
		while (true) {
			// TODO: endles loop
			data = msgHandle.getDataFromBuffer(src, dst, tag, istatus);
			if (data != null) {
				status = new Status(istatus.MPI_SOURCE(), istatus.MPI_TAG(), 0);
				break;
			} else {
				try {
					Thread.sleep(50);
				} catch (Exception e) {
				}
			}
		}

		int length;
		if (datatype.getBaseType() == Datatype.OBJECT) {
			Comm.Object_Deserialize(recvBuffer, (byte[]) data, offset, count, datatype);
			length = 1;
		} else {
			// blocks by looping until appropriate message found in buffer
			length = copyBufferCheck(data, 0, recvBuffer, offset, count, datatype);
		}
		status.setLength(length);
		msgHandle.removeDataFromBuffer(status.source, dst, status.tag);
		return (status);
	}

	/**
	 * Test if message reception has completed. Returns a status object if the operation identified by the request is complete, or a null reference otherwise. Java binding of the MPI operation
	 * MPI_TEST. After the call, if the operation is complete (ie, if the return value of test is non-null), the request object becomes an inactive request.
	 */
	public Status Test() {
		Object data;
		Status status = null;
		IStatus istatus = new IStatus(src, tag, count);
		data = msgHandle.getDataFromBuffer(src, dst, tag, istatus);
		if (data != null) {
			byte[] b_data = (byte[]) data;
			int length = b_data.length;
			status = new Status(istatus.MPI_SOURCE(), istatus.MPI_TAG(), length);
		}
		return (status);
	}

	private int copyBufferCheck(Object srcBuffer, int srcOffset, Object dstBuffer, int dstOffset, int count, Datatype datatype) {
		int dispSeqLen = datatype.getDisplacementSequence().length;
		switch (datatype.getBaseType()) {
		case Datatype.STRING:
			String[] str_src = (String[]) srcBuffer;
			String[] str_dst = (String[]) dstBuffer;
			if (str_src.length > count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = str_src.length;
			}
			for (int i = 0; i < count; i++) {
				str_dst[i] = str_src[i];
			}
			break;

		case Datatype.BYTE:
			byte[] b_src = (byte[]) srcBuffer;
			byte[] b_dst = (byte[]) dstBuffer;
			if (b_src.length > dispSeqLen * count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = b_src.length;
			}
			System.arraycopy(b_src, srcOffset, b_dst, dstOffset, count);
			break;
		case Datatype.CHAR:
			char[] ch_src = (char[]) srcBuffer;
			char[] ch_dst = (char[]) dstBuffer;
			if (ch_src.length > dispSeqLen * count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = ch_src.length;
			}
			System.arraycopy(ch_src, srcOffset, ch_dst, dstOffset, count);
			break;
		case Datatype.SHORT:
			short[] sh_src = (short[]) srcBuffer;
			short[] sh_dst = (short[]) dstBuffer;
			if (sh_src.length > dispSeqLen * count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = sh_src.length;
			}
			System.arraycopy(sh_src, srcOffset, sh_dst, dstOffset, count);
			break;
		case Datatype.INT:
			int[] i_src = (int[]) srcBuffer;
			int[] i_dst = (int[]) dstBuffer;
			if (i_src.length > dispSeqLen * count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = i_src.length;
			}
			System.arraycopy(i_src, srcOffset, i_dst, dstOffset, count);
			break;
		case Datatype.LONG:
			long[] l_src = (long[]) srcBuffer;
			long[] l_dst = (long[]) dstBuffer;
			if (l_src.length > dispSeqLen * count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = l_src.length;
			}
			System.arraycopy(l_src, srcOffset, l_dst, dstOffset, count);
			break;
		case Datatype.FLOAT:
			float[] f_src = (float[]) srcBuffer;
			float[] f_dst = (float[]) dstBuffer;
			if (f_src.length > dispSeqLen * count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = f_src.length;
			}
			System.arraycopy(f_src, srcOffset, f_dst, dstOffset, count);
			break;
		case Datatype.DOUBLE:
			double[] d_src = (double[]) srcBuffer;
			double[] d_dst = (double[]) dstBuffer;
			if (d_src.length > dispSeqLen * count) {
				msgHandle.getTask().exit("** [Error] sending message size is bigger than recieving message size");
				msgHandle.getTask().isTerminated();
			} else {
				count = d_src.length;
			}
			System.arraycopy(d_src, srcOffset, d_dst, dstOffset, count);
			break;
		}

		return (count * datatype.getBaseSize());
	}
}
