package ee.ut.f2f.comm;

@SuppressWarnings("serial")
public class CommunicationFailedException extends CommunicationException 
{
	public CommunicationFailedException() {
		super();
	}

	public CommunicationFailedException(String msg) {
		this(new Exception(msg));
	}

	public CommunicationFailedException(Exception e) {
		super(e);
	}
}
