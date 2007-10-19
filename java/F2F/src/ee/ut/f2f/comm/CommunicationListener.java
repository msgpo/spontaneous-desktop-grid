package ee.ut.f2f.comm;

/**
 * CommunicationListener interface provides access to CommunicationLayer
 * generated events.
 *
 */
public interface CommunicationListener {

	/**
	 * Fired when complete message is received.
	 *
	 * @param id
	 * @param message
	 * @param fromPeer
	 */
	void messageRecieved(Object message, Peer fromPeer);

	/**
	 * Fired when receiving message that is divided into multiple pieces is
	 * started.
	 * <p>
	 * Toggether with messageReceiveProgress allows to estimate message receiving
	 * process.
	 *
	 * @param id
	 * @param packetCount
	 * @param fromPeer
	 */
	void messageReceiveStarted(String id, int packetCount, Peer fromPeer);

	/**
	 * Fired when receiving one piece from message that is divided into
	 * multiple pieces is received.
	 * <p>
	 * Toggether with messageReceiveStarted allows to estimate message receiving
	 * process.
	 *
	 * @param id
	 * @param packetNumber
	 * @param fromPeer
	 */
	void messageReceiveProgress(String id, int packetNumber, Peer fromPeer);

	/**
	 * Fired when specific peer comes online.
	 */
	void peerOnline(Peer peer);

	/**
	 * Fired when specific peer goes offline.
	 */
	void peerOffline(Peer peer);

	/**
	 * Fired when communication comes online.
	 */
	void communicationOnline();

	/**
	 * Fired when communication goes offline.
	 */
	void communicationOffline();

}
