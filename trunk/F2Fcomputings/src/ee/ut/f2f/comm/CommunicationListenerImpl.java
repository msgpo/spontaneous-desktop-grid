package ee.ut.f2f.comm;

/**
 * Provides base implementation for CommunicationListener to easy usage of
 * this interface.
 */
public abstract class CommunicationListenerImpl implements CommunicationListener {

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationListener#messageReceiveProgress(java.lang.String, int, ee.ut.f2f.comm.Peer)
	 */
	public void messageReceiveProgress(String id, int packetNumber, Peer fromPeer) {

	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationListener#messageReceiveStarted(java.lang.String, int, ee.ut.f2f.comm.Peer)
	 */
	public void messageReceiveStarted(String id, int packetCount, Peer fromPeer) {

	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationListener#messageRecieved(java.lang.String, java.lang.Object, ee.ut.f2f.comm.Peer)
	 */
	public void messageRecieved(Object message, Peer fromPeer) {

	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationListener#peerOffline(ee.ut.f2f.comm.Peer)
	 */
	public void peerOffline(Peer peer) {

	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationListener#peerOnline(ee.ut.f2f.comm.Peer)
	 */
	public void peerOnline(Peer peer) {

	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationListener#communicationOffline()
	 */
	public void communicationOffline() {

	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.CommunicationListener#communicationOnline()
	 */
	public void communicationOnline() {

	}

}
