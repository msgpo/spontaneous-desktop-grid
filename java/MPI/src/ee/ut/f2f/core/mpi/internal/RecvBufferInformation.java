package ee.ut.f2f.core.mpi.internal;

public class RecvBufferInformation {
	String mid;
	int fromRank;
	int toRank;
	int tag;
	Object data;
	int sequence;

	public RecvBufferInformation(String mid, int fromRank, int toRank, Object data, int tag) {
		this.mid = new String(mid);
		this.fromRank = fromRank;
		this.toRank = toRank;
		this.data = data;
		this.tag = tag;
	}

	public String getMID() {
		return mid;
	}

	public int fromRank() {
		return fromRank;
	}

	public int toRank() {
		return toRank;
	}

	public int getTag() {
		return tag;
	}

	public Object getData() {
		return data;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
}
