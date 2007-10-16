package ee.ut.f2f.comm.skype;

import java.util.Vector;

import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

class SkypePeerManager extends Thread
{
	CommunicationLayer communicationLayer;

	Vector<CommunicationListener> communicationListeners;


	SkypePeerManager() {
		communicationListeners = new Vector<CommunicationListener>();

		this.start();
	}

	public synchronized  void addListener(CommunicationListener listener) {
		communicationListeners.add(listener);
	}

	public void run() {

		while (true) {
			try {

				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	void peerOffline(Peer peer) {
		F2FDebug.println("\t\tSKYPE peer " + peer.getID() + " went offline.");
		for (CommunicationListener listener : communicationListeners ) {
			listener.peerOffline(peer);
		}
	}

	void peerOnline(Peer peer) {
		F2FDebug.println("\t\tSKYPE User " + peer.getID() + " came online.");
		for (CommunicationListener listener : communicationListeners ) {
			listener.peerOnline(peer);
		}
	}

}
