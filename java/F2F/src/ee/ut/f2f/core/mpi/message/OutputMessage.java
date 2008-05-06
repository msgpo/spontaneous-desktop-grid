package ee.ut.f2f.core.mpi.message;

import java.io.Serializable;

public class OutputMessage extends BasicMessage implements Serializable{
	private static final long serialVersionUID = 1000003L;
	String output;

	public OutputMessage(String output) {
		this.output = new String(output);
	}

	public String getOutput() {
		return output;
	}
}
