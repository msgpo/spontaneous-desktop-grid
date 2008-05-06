package ee.ut.f2f.core.mpi;

/**
 * Primitive datatypes
 */
public class Datatype {
	/**
	 * Primitive datatype (NULL)
	 */
	public final static int NULL = 0;
	/**
	 * Primitive datatype (Byte)
	 */
	public final static int BYTE = 1;

	/**
	 * Primitive datatype (Char)
	 */
	public final static int CHAR = 2;

	/**
	 * Primitive datatype (Short)
	 */
	public final static int SHORT = 3;

	/**
	 * Primitive datatype (Boolean)
	 */
	public final static int BOOLEAN = 4;

	/**
	 * Primitive datatype (Integer)
	 */
	public final static int INT = 5;

	/**
	 * Primitive datatype (Long)
	 */
	public final static int LONG = 6;

	/**
	 * Primitive datatype (Float)
	 */
	public final static int FLOAT = 7;

	/**
	 * Primitive datatype (Double)
	 */
	public final static int DOUBLE = 8;

	/**
	 * Primitive datatype (not available yet)
	 */
	public final static int PACKED = 9; // Not available

	/**
	 * Primitive datatype (String)
	 */
	public final static int STRING = 10;

	public final static int OBJECT = 11;
	// public final static int OBJECT = 12;

	/**
	 * Size (in byte) of each datatype
	 */
	public final static int[] typeSize = { 0, 1, 2, 2, 1, 4, 8, 4, 8, 0, 1, 1 };

	int[] dispSeq;
	int baseType;
	int baseSize;

	/**
	 * Default constructor
	 */
	public Datatype(int type) {
		dispSeq = new int[1];
		dispSeq[0] = 0;
		baseType = type;
		baseSize = typeSize[type];
	}

	/**
	 * Internal use
	 */
	public Datatype(int type, int[] dispSeq) {
		int dispSeqLen = dispSeq.length;
		this.dispSeq = new int[dispSeqLen];
		for (int i = 0; i < dispSeqLen; i++) {
			this.dispSeq[i] = dispSeq[i];
		}
		baseType = type;
		baseSize = typeSize[type];
	}

	/**
	 * The lower bound of a datatype
	 * 
	 * @return displacement of lower bound from origin
	 */
	public int Lb() {
		int dispSeqLen = dispSeq.length;
		int lb = dispSeq[0];

		for (int i = 1; i < dispSeqLen; i++) {
			if (dispSeq[i] < lb)
				lb = dispSeq[i];
		}

		return lb;
	}

	/**
	 * The upper bound of a datatype
	 * 
	 * @return displacement of upper bound from origin
	 */
	public int Ub() {
		int dispSeqLen = dispSeq.length;
		int ub = 0;
		int tmpUb;

		for (int i = 0; i < dispSeqLen; i++) {
			tmpUb = dispSeq[i] + 1;
			if (tmpUb > ub) {
				ub = tmpUb;
			}
		}
		return ub;
	}

	/**
	 * Returns the extent of a datatype
	 * 
	 * @return datatype extent
	 */
	public int Extent() {
		return Ub() - Lb();
	}

	// Same Get_extent : In java there is no pad
	/**
	 * Returns the size of a datatype
	 * 
	 * @return datatype size
	 */
	public int Size() {
		return Ub() - Lb();
	}

	/**
	 * Create a contiguous datatype
	 * 
	 * @param count
	 *            number of elements
	 * @return contiguous datatype
	 */
	public Datatype Contiguous(int count) {
		int dispSeqLen = dispSeq.length;
		int[] newDispSeq = new int[count * dispSeqLen];
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < dispSeqLen; j++) {
				// TODO FIX HERE:
				// In Java we can't cast void and operation with pointer
				// so dispSeq is always 1
				// newDispSeq[(i*dispSeqLen)+j] = dispSeq[j]+(i * Extent());
				newDispSeq[(i * dispSeqLen) + j] = dispSeq[j] + i;
			}
		}
		return new Datatype(baseType, newDispSeq);
	}
	
	/**
	 * Returns the sequence of displacement of data
	 * 
	 * @return sequence of displacement
	 */
	public int[] getDisplacementSequence() {
		return dispSeq;
	}

	/**
	 * Returns the base type of datatype
	 * 
	 * @return base type
	 */
	public int getBaseType() {
		return baseType;
	}

	/**
	 * Returns the base size of datatype
	 * 
	 * @return size of base type
	 */
	public int getBaseSize() {
		return baseSize;
	}
}
