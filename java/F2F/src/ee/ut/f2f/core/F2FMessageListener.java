package ee.ut.f2f.core;

public interface F2FMessageListener
{
	void messageReceived(Object message, F2FPeer sender);
}
