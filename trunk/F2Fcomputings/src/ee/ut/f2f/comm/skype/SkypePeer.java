package ee.ut.f2f.comm.skype;

import com.skype.SkypeException;
import com.skype.User.Status;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.Peer;

/**
 * Skype specific implementation of Peer interface.
 */
class SkypePeer implements Peer
{
	SkypeCommunicationLayer communicationLayer;
	public CommunicationLayer getCommunicationLayer()
	{
		return this.communicationLayer;
	}
	
	private String ID;
	public String getID() {
		return ID;
	}

	private String displayName;
	public String getDisplayName() { return displayName; }

	
	/**
	 * @param communicationLayer
	 * @param UID
	 */
	SkypePeer(SkypeCommunicationLayer communicationLayer, String sID, String displayName) {
		this.communicationLayer = communicationLayer;
		this.ID = sID;
		this.displayName = displayName;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.Peer#isOnline()
	 */
	public boolean isOnline() {

		SkypeCommunicationLayer skypeCommunicationLayer = (SkypeCommunicationLayer)communicationLayer;

		com.skype.User user = skypeCommunicationLayer.getSkypeUser(ID);

		try {
			Status status  = user.getOnlineStatus();

			if (status.compareTo(Status.ONLINE) > 0) {
				return true;
			}
			if (status.compareTo(Status.AWAY) > 0) {
				return true;
			}

		} catch (SkypeException e) {
			return false;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.Peer#sendMessage(java.lang.Object)
	 */
	public void sendMessage(Object msgObj) throws CommunicationFailedException {
		try {
			communicationLayer.sendMessage(msgObj, ID);
		} catch (Exception e) {
			throw new CommunicationFailedException(e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return displayName;
	}

}
