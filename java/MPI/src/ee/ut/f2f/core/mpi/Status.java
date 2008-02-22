package ee.ut.f2f.core.mpi;

/**
 * The status of a message commmunication
 */
public class Status {
	/**
	 * source rank
	 */
	public int source; // MPI_SOURCE

	/**
	 * tag number
	 */
	public int tag; // MPI_TAG

	protected int length; // MPI_LENGTH

	/**
	 * Internal use
	 */
	public Status() {
	}

	/**
	 * Internal use
	 */
	public Status(int src, int tag, int length) {
		this.length = length;
		setStatus(src, tag);
	}

	/**
	 * Internal use
	 */
	public void setStatus(int src, int tag) {
		this.source = src;
		this.tag = tag;
	}

	/**
	 * Get the number of "top-level" elements
	 * 
	 * @param type
	 *            data type
	 * 
	 * @return number of elements
	 */
	public int Get_count(Datatype type) {
		return length / type.getBaseSize();
	}

	/**
	 * Internal use
	 */
	public void setLength(int length) {
		this.length = length;
	}
}
