package ee.ut.f2f.core.mpi.internal;

/**
 * The status of a message commmunication
 */
public class IStatus {
	protected int src; // MPI_SOURCE
	protected int tag; // MPI_TAG
	protected int length; // MPI_LENGTH

	/**
	 * Internal use
	 */
	public IStatus() {
	}

	/**
	 * Internal use
	 */
	public IStatus(int src, int tag, int length) {
		this.length = length;
		setStatus(src, tag);
	}

	/**
	 * Internal use
	 */
	public void setStatus(int src, int tag) {
		this.src = src;
		this.tag = tag;
	}

	/**
	 * Get source (rank of emitter) of message
	 */
	public int MPI_SOURCE() {
		return (src);
	}

	/**
	 * Get tag of message
	 */
	public int MPI_TAG() {
		return (tag);
	}
}
