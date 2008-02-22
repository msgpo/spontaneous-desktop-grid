package ee.ut.f2f.core.mpi.message;

public class DataMessage extends BasicMessage {
	private static final long serialVersionUID = 2000011L;
	Object data;
	String mid;
	int fromRank;
	int toRank;
	int tag;
	int sequence;

	public DataMessage(String mid, int from, int to, int tag, int sequence) {
		this.mid = new String(mid);
		fromRank = from;
		toRank = to;
		this.tag = tag;
		this.sequence = sequence;
	}

	public String getMID() {
		return mid;
	}

	public int getFromRank() {
		return fromRank;
	}

	public int getToRank() {
		return toRank;
	}

	public int getTag() {
		return tag;
	}

	public Object getData() {
		return data;
	}

	public void addData(long buffer) {
		long[] arrayBuffer = new long[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(long[] buffer) {
		data = buffer;
	}

	public void addData(int buffer) {
		int[] arrayBuffer = new int[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(int[] buffer) {
		data = buffer;
	}

	public void addData(short buffer) {
		short[] arrayBuffer = new short[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(short[] buffer) {
		data = buffer;
	}

	public void addData(double buffer) {
		double[] arrayBuffer = new double[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(double[] buffer) {
		data = buffer;
	}

	public void addData(float buffer) {
		float[] arrayBuffer = new float[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(float[] buffer) {
		data = buffer;
	}

	public void addData(char buffer) {
		char[] arrayBuffer = new char[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(char[] buffer) {
		data = buffer;
	}

	public void addData(byte buffer) {
		byte[] arrayBuffer = new byte[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(byte[] buffer) {
		data = buffer;
	}

	public void addData(String[] buffer) {
		data = buffer;
	}

	public void addData(String buffer) {
		String[] arrayBuffer = new String[1];
		arrayBuffer[0] = buffer;
		addData(arrayBuffer);
	}

	public void addData(Object buffer) {
		data = buffer;
	}

	public Object getObjectData() {
		return data;
	}

	public byte[] getByteData() {
		return (byte[]) data;
	}

	public int[] getIntData() {
		return (int[]) data;
	}

	public long[] getLongData() {
		return (long[]) data;
	}

	public short[] getShortData() {
		return (short[]) data;
	}

	public double[] getDoubleData() {
		return (double[]) data;
	}

	public float[] getFloatData() {
		return (float[]) data;
	}

	public char[] getCharData() {
		return (char[]) data;
	}

	public String[] getStringData() {
		return (String[]) data;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public String toString() {
		return "from = " + fromRank + " to = " + toRank + " taf = " + tag + " sequence = " + sequence;
	}
}
