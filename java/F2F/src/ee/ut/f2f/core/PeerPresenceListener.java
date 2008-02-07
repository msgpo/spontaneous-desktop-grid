package ee.ut.f2f.core;

public interface PeerPresenceListener
{
	void peerContacted(F2FPeer peer);
	void peerUnContacted(F2FPeer peer);
}
