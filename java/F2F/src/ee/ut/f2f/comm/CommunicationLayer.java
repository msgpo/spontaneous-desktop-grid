package ee.ut.f2f.comm;

import java.util.Collection;

/**
 *  Provides methods for peer information, message sending and connection
 *  listener (which helps develop server functionality).
 *  <p>
 *  Usage of this package should start with creating one of CommunicationLayer
 *  interface instances (via CommunicationFactory class).
 *
 */
public interface CommunicationLayer {

	/**
	 * Returns currently available peers.
	 *
	 * @return
	 * @throws CommunicationFailedException
	 */
	Collection<Peer> getPeers() throws CommunicationFailedException;

	/**
	 * Returns currently available peer by its id.
	 *
	 * @param ID
	 * @return
	 * @throws CommunicationFailedException
	 */
	Peer findPeerByID(String ID) throws CommunicationFailedException;

	/**
	 * Registers new communication listener.
	 *
	 * @param listener
	 * @throws CommunicationFailedException
	 */
	void addListener(CommunicationListener listener);

	/**
	 * Returns Local peer instance.
	 *
	 * @return
	 */
	Peer getLocalPeer();

	/**
	 * Returns ID of the communication layer.
	 *
	 * @return ID of the communication layer
	 */
	String getID();
	
	boolean isLocalPeerID(String ID);
}
