package ee.ut.f2f.core.mpi.message;

public class OutputMessage extends BasicMessage {
	private static final long serialVersionUID = 1000003L;
	String output;

	public OutputMessage(String output) {
		this.output = new String(output);
	}

	public String getOutput() {
		return output;
	}
}
