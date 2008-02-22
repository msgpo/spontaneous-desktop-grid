package ee.ut.f2f.core.mpi;

/**
 * MPI Collective Operation
 */
public class Op {
	/**
	 * No operation
	 */
	public final static int NULL = 0;

	/**
	 * Maximum operation
	 */
	public final static int MAX = 1;

	/**
	 * Minimum operation
	 */
	public final static int MIN = 2;

	/**
	 * Summation operation
	 */
	public final static int SUM = 3;

	/**
	 * Multiplication operation
	 */
	public final static int PROD = 4;

	/**
	 * Bit-wise AND operation
	 */
	public final static int BAND = 6;
	public final static int MINLOC = 11;
	public final static int MAXLOC = 12;

	private MPI_User_function func = null;
	private boolean commute = false;

	/**
	 * Default constructor for MPI (internal use)
	 * 
	 * @param Type
	 *            NULL, MAX, MIN, SUM, or PRUD operation
	 */
	public Op(int Type) {
		switch (Type) {
		case NULL:
			func = new OpNull();
			commute = true;
			break;
		case MAX:
			func = new OpMax();
			commute = true;
			break;
		case MIN:
			func = new OpMin();
			commute = true;
			break;
		case SUM:
			func = new OpSum();
			commute = true;
			break;
		case PROD:
			func = new OpProd();
			commute = true;
			break;
		case BAND:
			func = new OpBAND();
			commute = true;
			break;
		case MAXLOC:
			func = new OpMAXLOC();
			commute = true;
			break;
		case MINLOC:
			func = new OpMINLOC();
			commute = true;
			break;
		// To DO More
		default:
			func = new OpNull();
			commute = true;
			break;
		}

	}

	/**
	 * Constructor for the user-definition operation
	 * 
	 * @param func
	 *            user-define function inherited from MPI_User_function
	 * @param commute
	 *            commutation operation
	 */
	public Op(MPI_User_function func, boolean commute) {
		this.func = func;
		this.commute = commute; // Commute False = A op B is not the same as B op A
	}

	/**
	 * Invoke operation (internal use)
	 * 
	 * @param in
	 *            Input object
	 * @param inOffset
	 *            Input offset
	 * @param inout
	 *            Input and Output object
	 * @param inoutOffset
	 *            Input and output offset
	 * @param count
	 *            number of elements
	 * @param type
	 *            MPI datatype
	 */
	public void Call(Object in, int inOffset, Object inout, int inoutOffset, int count, Datatype type) {
		func.Call(in, inOffset, inout, inoutOffset, count, type);
	}

	/**
	 * Check if operation is commutative
	 * 
	 * @return true if commutative, otherwise false
	 */
	public boolean isCommute() {
		return commute;
	}
}
