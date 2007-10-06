package ee.ut.f2f.comm.skype;

/**
 * Special case for local peer.
 */
class SkypeLocalPeer extends SkypePeer
{
	/**
	 * @param communicationLayer
	 * @param UID
	 */
	SkypeLocalPeer(SkypeCommunicationLayer communicationLayer, String sID, String displayName)
	{
		super(communicationLayer, sID, displayName);
	}

	/**
	 * Allways true, basically meaning you can always send message to local peer.
	 *
	 * @see ee.ut.f2f.comm.skype.SkypePeer#isOnline()
	 */
	public boolean isOnline()
	{
		return true;
	}
}
