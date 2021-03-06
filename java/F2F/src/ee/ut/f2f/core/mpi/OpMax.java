package ee.ut.f2f.core.mpi;

/**
 * Collective operation for maximum
 */
public class OpMax extends MPI_User_function {
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
					if (b_invec[currentInOffset] > b_inoutvec[currentInOutOffset])
						b_inoutvec[currentInOutOffset] = b_invec[currentInOffset];
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
					if (ch_invec[currentInOffset] > ch_inoutvec[currentInOutOffset])
						ch_inoutvec[currentInOutOffset] = ch_invec[currentInOffset];
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
					if (sh_invec[currentInOffset] > sh_inoutvec[currentInOutOffset])
						sh_inoutvec[currentInOutOffset] = sh_invec[currentInOffset];
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
					if (i_invec[currentInOffset] > i_inoutvec[currentInOutOffset])
						i_inoutvec[currentInOutOffset] = i_invec[currentInOffset];
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
					if (l_invec[currentInOffset] > l_inoutvec[currentInOutOffset])
						l_inoutvec[currentInOutOffset] = l_invec[currentInOffset];
				}
			}
			break;

		case Datatype.FLOAT:
			float[] f_invec = (float[]) invec;
			float[] f_inoutvec = (float[]) inoutvec;
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < dispSeqLen; j++) {
					int disp = dispSeq[j] + (i * type.Extent());
					int currentInOffset = disp + inOffset;
					int currentInOutOffset = disp + inoutOffset;
					if (f_invec[currentInOffset] > f_inoutvec[currentInOutOffset])
						f_inoutvec[currentInOutOffset] = f_invec[currentInOffset];
				}
			}
			break;

		case Datatype.DOUBLE:
			double[] d_invec = (double[]) invec;
			double[] d_inoutvec = (double[]) inoutvec;
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < dispSeqLen; j++) {
					int disp = dispSeq[j] + (i * type.Extent());
					int currentInOffset = disp + inOffset;
					int currentInOutOffset = disp + inoutOffset;
					if (d_invec[currentInOffset] > d_inoutvec[currentInOutOffset])
						d_inoutvec[currentInOutOffset] = d_invec[currentInOffset];
				}
			}
			break;

		case Datatype.PACKED:
			// Not implement yet
			break;
		}
	}
}
