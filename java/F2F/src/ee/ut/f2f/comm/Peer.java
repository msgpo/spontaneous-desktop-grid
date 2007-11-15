package ee.ut.f2f.comm;

/**
 * Provides basic peer information and helps send message to corresponding peer.
 *
 */
public interface Peer
{
	/**
	 * ID = peer ID.
	 *
	 * @return
	 */
	String getID();
	
	/**
	 * @return The name that should be shown in GUI.
	 */
	String getDisplayName();

	/**
	 * Sends message to peer.
	 *
	 * @param message
	 * @throws CommunicationFailedException
	 */
	void sendMessage(Object message) throws CommunicationFailedException;

	CommunicationLayer getCommunicationLayer();
}
