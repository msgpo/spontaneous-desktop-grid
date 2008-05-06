package ee.ut.f2f.core.mpi;

/**
 * Collective operation for bit-wise AND
 */
public class OpBAND extends MPI_User_function {
	// Implement Call method for MAX operation
	public void Call(Object invec, int inOffset, Object inoutvec, int inoutOffset, int count, Datatype type) {
		int baseType = type.getBaseType();

		int[] dispSeq = type.getDisplacementSequence();
		int dispSeqLen = dispSeq.length;

		switch (baseType) {
		case Datatype.BYTE:
			byte[] b_invec = (byte[]) invec;
			byte[] b_inoutvec = (byte[]) inoutvec;
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < dispSeqLen; j++) {
					int disp = dispSeq[j] + (i * type.Extent());
					int currentInOffset = disp + inOffset;
					int currentInOutOffset = disp + inoutOffset;
					b_inoutvec[currentInOutOffset] &= b_invec[currentInOffset];
				}
			}
			break;

		case Datatype.CHAR:
			char[] ch_invec = (char[]) invec;
			char[] ch_inoutvec = (char[]) inoutvec;
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < dispSeqLen; j++) {
					int disp = dispSeq[j] + (i * type.Extent());
					int currentInOffset = disp + inOffset;
					int currentInOutOffset = disp + inoutOffset;
					ch_inoutvec[currentInOutOffset] &= ch_invec[currentInOffset];
				}
			}
			break;

		case Datatype.SHORT:
			short[] sh_invec = (short[]) invec;
			short[] sh_inoutvec = (short[]) inoutvec;
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < dispSeqLen; j++) {
					int disp = dispSeq[j] + (i * type.Extent());
					int currentInOffset = disp + inOffset;
					int currentInOutOffset = disp + inoutOffset;
					sh_inoutvec[currentInOutOffset] &= sh_invec[currentInOffset];
				}
			}
			break;

		case Datatype.BOOLEAN:
			// ?? We have to do ??
			break;

		case Datatype.INT:
			int[] i_invec = (int[]) invec;
			int[] i_inoutvec = (int[]) inoutvec;
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < dispSeqLen; j++) {
					int disp = dispSeq[j] + (i * type.Extent());
					int currentInOffset = disp + inOffset;
					int currentInOutOffset = disp + inoutOffset;
					i_inoutvec[currentInOutOffset] &= i_invec[currentInOffset];
				}
			}
			break;

		case Datatype.LONG:
			long[] l_invec = (long[]) invec;
			long[] l_inoutvec = (long[]) inoutvec;
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < dispSeqLen; j++) {
					int disp = dispSeq[j] + (i * type.Extent());
					int currentInOffset = disp + inOffset;
					int currentInOutOffset = disp + inoutOffset;
					l_inoutvec[currentInOutOffset] &= l_invec[currentInOffset];
				}
			}
			break;

		case Datatype.PACKED:
			// Not implement yet
			break;
		}
	}
}
