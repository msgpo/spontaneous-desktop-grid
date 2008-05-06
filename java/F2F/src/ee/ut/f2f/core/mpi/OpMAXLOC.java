package ee.ut.f2f.core.mpi;

/**
 * Collective operation for maximum and its location
 */
public class OpMAXLOC extends MPI_User_function {
	// Implement Call method for MAX operation
	public void Call(Object invec, int inOffset, Object inoutvec, int inoutOffset, int count, Datatype type) {
		int baseType = type.getBaseType();

		int[] dispSeq = type.getDisplacementSequence();
		int dispSeqLen = dispSeq.length;

		switch (baseType) {
		case Datatype.INT:
			int[] i_invec = (int[]) invec;
			int[] i_inoutvec = (int[]) inoutvec;
			for (int i = 0; i < count; i++) {
				int disp = i * dispSeqLen;
				int currentInOffset = disp + inOffset;
				int currentInOutOffset = disp + inoutOffset;
				if (i_invec[currentInOffset] > i_inoutvec[currentInOutOffset]) {
					i_inoutvec[currentInOutOffset] = i_invec[currentInOffset];
					i_inoutvec[currentInOutOffset + 1] = i_invec[currentInOffset + 1];
				}
			}
			break;

		case Datatype.BYTE:
			byte[] b_invec = (byte[]) invec;
			byte[] b_inoutvec = (byte[]) inoutvec;
			for (int i = 0; i < count; i++) {
				int disp = i * dispSeqLen;
				int currentInOffset = disp + inOffset;
				int currentInOutOffset = disp + inoutOffset;
				if (b_invec[currentInOffset] > b_inoutvec[currentInOutOffset]) {
					b_inoutvec[currentInOutOffset] = b_invec[currentInOffset];
					b_inoutvec[currentInOutOffset + 1] = b_invec[currentInOffset + 1];
				}
			}
			break;

		case Datatype.CHAR:
			char[] ch_invec = (char[]) invec;
			char[] ch_inoutvec = (char[]) inoutvec;
			for (int i = 0; i < count; i++) {
				int disp = i * dispSeqLen;
				int currentInOffset = disp + inOffset;
				int currentInOutOffset = disp + inoutOffset;
				if (ch_invec[currentInOffset] > ch_inoutvec[currentInOutOffset]) {
					ch_inoutvec[currentInOutOffset] = ch_invec[currentInOffset];
					ch_inoutvec[currentInOutOffset + 1] = ch_invec[currentInOffset + 1];
				}
			}
			break;

		case Datatype.SHORT:
			short[] sh_invec = (short[]) invec;
			short[] sh_inoutvec = (short[]) inoutvec;
			for (int i = 0; i < count; i++) {
				int disp = i * dispSeqLen;
				int currentInOffset = disp + inOffset;
				int currentInOutOffset = disp + inoutOffset;
				if (sh_invec[currentInOffset] > sh_inoutvec[currentInOutOffset]) {
					sh_inoutvec[currentInOutOffset] = sh_invec[currentInOffset];
					sh_inoutvec[currentInOutOffset + 1] = sh_invec[currentInOffset + 1];
				}
			}
			break;

		case Datatype.BOOLEAN:
			// ?? We have to do ??
			break;

		case Datatype.LONG:
			long[] l_invec = (long[]) invec;
			long[] l_inoutvec = (long[]) inoutvec;
			for (int i = 0; i < count; i++) {
				int disp = i * dispSeqLen;
				int currentInOffset = disp + inOffset;
				int currentInOutOffset = disp + inoutOffset;
				if (l_invec[currentInOffset] > l_inoutvec[currentInOutOffset]) {
					l_inoutvec[currentInOutOffset] = l_invec[currentInOffset];
					l_inoutvec[currentInOutOffset + 1] = l_invec[currentInOffset + 1];
				}
			}
			break;

		case Datatype.FLOAT:
			float[] f_invec = (float[]) invec;
			float[] f_inoutvec = (float[]) inoutvec;
			for (int i = 0; i < count; i++) {
				int disp = i * dispSeqLen;
				int currentInOffset = disp + inOffset;
				int currentInOutOffset = disp + inoutOffset;
				if (f_invec[currentInOffset] > f_inoutvec[currentInOutOffset]) {
					f_inoutvec[currentInOutOffset] = f_invec[currentInOffset];
					f_inoutvec[currentInOutOffset + 1] = f_invec[currentInOffset + 1];
				}
			}
			break;

		case Datatype.DOUBLE:
			double[] d_invec = (double[]) invec;
			double[] d_inoutvec = (double[]) inoutvec;
			for (int i = 0; i < count; i++) {
				int disp = i * dispSeqLen;
				int currentInOffset = disp + inOffset;
				int currentInOutOffset = disp + inoutOffset;
				if (d_invec[currentInOffset] > d_inoutvec[currentInOutOffset]) {
					d_inoutvec[currentInOutOffset] = d_invec[currentInOffset];
					d_inoutvec[currentInOutOffset + 1] = d_invec[currentInOffset + 1];
				}
			}
			break;

		case Datatype.PACKED:
			// Not implement yet
			break;
		}
	}
}
