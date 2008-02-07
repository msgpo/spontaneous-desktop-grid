package ee.ut.f2f.core;

public interface MessageListener
{
	void messageReceived(Object message, F2FPeer sender);
}
