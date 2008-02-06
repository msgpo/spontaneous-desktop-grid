package ee.ut.f2f.core;

public interface F2FPeerPresenceListener
{
	void peerContacted(F2FPeer peer);
	void peerUnContacted(F2FPeer peer);
}
