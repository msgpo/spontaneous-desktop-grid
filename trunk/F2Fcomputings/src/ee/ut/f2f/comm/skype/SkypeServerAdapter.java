package ee.ut.f2f.comm.skype;

import java.util.Vector;

import com.skype.ApplicationAdapter;
import com.skype.SkypeException;
import com.skype.Stream;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

/**
 * Holds CommunicationListener instances and forwards events to them,
 * listens to Skype API connected event and registers new incoming streams,
 * holds CommunicationLayer instance needed in stream listener to allow
 * message replies and resolving of peer ID into Peer instance.
 *
 */
class SkypeServerAdapter extends ApplicationAdapter
{
	CommunicationLayer communicationLayer;

	Vector<CommunicationListener> communicationListeners;

	/**
	 * Creates new SkypeServerAdapter and registers CommunicationLayer instance
	 * for futere use by stream listeners.
	 *
	 * @param communicationLayer
	 */
	SkypeServerAdapter(CommunicationLayer communicationLayer) {

		this.communicationLayer = communicationLayer;

		communicationListeners = new Vector<CommunicationListener>();
	}

	/**
	 * Packet level method that allows access to related CommunicationLayer
	 * instance.
	 *
	 * @return
	 */
	CommunicationLayer getCommunicationLayer() {
		return communicationLayer;
	}

	/**
	 * Registers new listener.
	 *
	 * @param listener
	 */
	void addListener(CommunicationListener listener) {
		communicationListeners.add(listener);
	}

	/**
	 * Skype API new connection event. Is fired when new remote connection is received.
	 * When connection is opened, registers new stream listener.
	 *
	 * @see com.skype.ApplicationAdapter#connected(com.skype.Stream)
	 */
	public void connected(Stream stream) throws SkypeException
	{
		SkypeStreamAdapter streamAdapter;
		try
		{
			streamAdapter = new SkypeStreamAdapter(getCommunicationLayer().findPeerByID(stream.getFriend().getId()), this);
			stream.addStreamListener(streamAdapter);
		}
		catch (CommunicationFailedException e)
		{
			F2FDebug.println("New connection in Skype communication layer from unknown user! " + e);
		}
	}

	/**
	 * Forwards messageReceiveProgress event.
	 */
	void messageReceiveProgress(String id, int packetNumber, Peer fromPeer) {
		for (CommunicationListener listener : communicationListeners ) {
			listener.messageReceiveProgress(id, packetNumber, fromPeer);
		}

	}

	/**
	 * Forwards messageReceiveStarted event.
	 */
	void messageReceiveStarted(String id, int packetCount, Peer fromPeer) {
		for (CommunicationListener listener : communicationListeners ) {
			listener.messageReceiveStarted(id, packetCount, fromPeer);
		}
	}


	/**
	 * Forwards messageRecieved event.
	 */
	void messageRecieved(Object message, Peer fromPeer) {
		for (CommunicationListener listener : communicationListeners ) {
			listener.messageRecieved(message, fromPeer);
		}
	}


}
