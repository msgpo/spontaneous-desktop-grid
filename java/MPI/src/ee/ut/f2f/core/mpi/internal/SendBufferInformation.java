package ee.ut.f2f.core.mpi.internal;

import ee.ut.f2f.core.mpi.message.DataMessage;

public class SendBufferInformation {
	String mid;
	int fromRank;
	int toRank;
	int tag;
	DataMessage data;

	public SendBufferInformation(String mid, int fromRank, int toRank, DataMessage data, int tag) {
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

	public DataMessage getData() {
		return data;
	}
}
