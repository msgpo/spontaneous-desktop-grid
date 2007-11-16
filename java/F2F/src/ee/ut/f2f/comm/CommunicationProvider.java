package ee.ut.f2f.comm;

import java.util.UUID;

public interface CommunicationProvider
{
	void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException;
}