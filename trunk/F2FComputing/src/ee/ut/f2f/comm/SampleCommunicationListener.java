package ee.ut.f2f.comm;

import ee.ut.f2f.util.F2FDebug;

public class SampleCommunicationListener extends CommunicationListenerImpl
{
	//private static final Logger LOG = LogManager.getLogger(SampleCommunicationListener.class);

	public void messageReceiveProgress(String id, int packetNumber, Peer fromPeer) {
		F2FDebug.println("\t\tReceive progress: " + id + " " +  packetNumber + " " +  fromPeer.getID());
	}

	public void messageReceiveStarted(String id, int packetCount, Peer fromPeer) {
		F2FDebug.println("\t\tReceive started: " + id + " " +  packetCount + " " +  fromPeer.getID());
	}

	public void messageRecieved(Object message, Peer fromPeer) {
		F2FDebug.println("\t\tMessage received: "+  message + " " +  fromPeer.getID());
	}

}
