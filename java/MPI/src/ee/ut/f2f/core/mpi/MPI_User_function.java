package ee.ut.f2f.core.mpi;

/**
 * Abstract Class to implement a user-defined operation
 */
public abstract class MPI_User_function {

	/**
	 * User-defined algorithm when operation is invoked
	 * 
	 * @param invec
	 *            Input object
	 * @param inoffset
	 *            Input offset
	 * @param inoutvec
	 *            Input and output object
	 * @param inoutoffset
	 *            Input and output offset
	 * @param count
	 *            Number of elements
	 * @param type
	 *            MPI datatype
	 */
	public abstract void Call(Object invec, int inoffset, Object inoutvec, int inoutoffset, int count, Datatype type);

}
