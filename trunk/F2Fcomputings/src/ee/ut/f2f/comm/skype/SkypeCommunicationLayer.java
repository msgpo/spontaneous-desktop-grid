package ee.ut.f2f.comm.skype;

import com.skype.*;

import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Random;

//import org.apache.log4j.LogManager;
//import org.apache.log4j.Logger;

/**
 * Skype specific implementation of CommunicationLayer interface.
 *
 */
public class SkypeCommunicationLayer implements CommunicationLayer {

	//private static final Logger LOG = LogManager.getLogger(SkypeCommunicationLayer.class);
	/**
	 * 11 kB, 1 kB is for protocol, Skype API allows send data as 12 kB
	 * packets so each longer message should be divided into smaller parts.
	 */
	private static final int MAX_PACKET_SIZE = 1024 * 63;

	/**
	 * Name that application is using when registering in Skype.
	 */
	private static final String SKYPE_APPLICATION_NAME = "F2Fcomputing";

	/**
	 * Name that idintifies Skype communication layer.
	 */
	private static final String SKYPE_LAYER_ID = "F2FSkypeCommLayer";
	
	/**
	 * Upper limit for message id.
	 */
	private static final int MAX_MESSAGE_ID = 20000000;

	private Application application;

	private Hashtable<String, Peer> peersHash;

	private int messageID = MAX_MESSAGE_ID; // When messageID is equal to
											// MAX_MESSAGE_ID then it is
											// recalculated

	private SkypeServerAdapter skypeServerAdapter;

	private SkypePeerManager skypePeerManager;

	private Peer localPeer;
	/**
	 * @see ee.ut.f2f.comm.CommunicationLayer#getLocalPeer()
	 */
	public Peer getLocalPeer()
	{
		return localPeer;
	}

	/**
	 * Throws CommunicationInitException when Skype API attach fails. For
	 * example when Skype is closed.
	 *
	 */
	public SkypeCommunicationLayer() throws CommunicationInitException
	{
		// enable or disable debug from Skype side
		try {
			Skype.setDebug(false);
		} catch (SkypeException e) {
			F2FDebug.println(e.toString());
		}
		Skype.setDeamon(true);
		try {
			application = Skype.addApplication(SKYPE_APPLICATION_NAME);
			localPeer = 
				new SkypeLocalPeer(
						this, 
						Skype.getProfile().getId(), 
						Skype.getProfile().getFullName());
		} catch (SkypeException e) {
			throw new CommunicationInitException("Could not start Skype communication layer! ", e);
		}

		skypeServerAdapter = new SkypeServerAdapter(this);

		application.addApplicationListener(skypeServerAdapter);

		skypePeerManager = new SkypePeerManager();

		peersHash = new Hashtable<String, Peer>();
	}

	/**
	 * Registers CommunicationListener instance for communication layer events.
	 * This allows for example to listen incoming messages.
	 *
	 * @see ee.ut.f2f.comm.CommunicationLayer#addListener(ee.ut.f2f.comm.CommunicationListener)
	 */
	public void addListener(CommunicationListener listener)
	{
		skypeServerAdapter.addListener(listener);
		skypePeerManager.addListener(listener);
	}

	/**
	 * Concept behind this method is that there should be only one Peer instance
	 * for each corresponding ID.
	 * <p>
	 * sID is taken as key in hash table and when key exists then Peer instance
	 * is returned from hashtable. When key does not exists then new Peer
	 * instance is created, added into hashtable and returned.
	 */
	private Peer createOrFindPeer(String sID, String displayName) {

		Peer peer = peersHash.get(sID);

		if (peer == null)
		{
			peer = new SkypePeer(this, sID, displayName);
			peersHash.put(sID, peer);
		}

		return peer;
	}

	/**
	 * Returns currently available peer by ID.
	 * For every sID only one Peer instance is returned (created once)
	 * during connection lifetime.
	 *
	 * @see ee.ut.f2f.comm.CommunicationLayer#findPeerByID(java.lang.String)
	 */
	public Peer findPeerByID(String sID) throws CommunicationFailedException
	{
		Friend[] friends;

		try
		{
			friends = application.getAllConnectableFriends();
		}
		catch (SkypeException e)
		{
			throw new CommunicationFailedException(e);
		}

		for (Friend friend : friends)
		{
			if (friend.getId().equals(sID))
			{
				try
				{
					return createOrFindPeer(sID, friend.getFullName());
				}
				catch (SkypeException e)
				{
					throw new CommunicationFailedException(e);
				}
			}
		}

		return null;
	}
	
	/**
	 * Helper method that helps generate message id's.
	 *
	 * @return
	 */
	private int calculateNextMessageID() {

		if (messageID >= MAX_MESSAGE_ID) {
			messageID = calculateFirstMessageID();
		} else {
			messageID++;
		}

		return messageID;
	}

	/**
	 * Meaning of this method is to lessen probability that different instances
	 * will get same message id at the same time (this will mess up reassembling
	 * messages when long messages are divided into smaller parts).
	 * <p>
	 * To accomplish that, beginning of message id sequence is taken randomly.
	 *
	 * @return
	 */
	private int calculateFirstMessageID() {

		Random randomNumberGenerator = new Random();

		return randomNumberGenerator.nextInt(MAX_MESSAGE_ID);

	}

	/**
	 * Package level method to help getting instances of User interface when
	 * they are listed in Skype contact list (even when offline).
	 *
	 * @param UID
	 * @return
	 */
	com.skype.User getSkypeUser(String UID) {
		return Skype.getUser(UID);
	}

	/**
	 * Creates and returns stream (connection with ability to send data) to
	 * specified Skype user.
	 * <p>
	 * To do that all connectable friends are asked from Skype API and array
	 * of Skype API Friend object is created when there userID does match one
	 * of them.
	 * <p>
	 * This allows to get connection to list of friends (to one friend in list here).
	 * <p>
	 * When everything is successful then Stream object is returned otherwise
	 * CommunicationFailedException is raised.
	 *
	 * @param userID
	 * @return
	 * @throws SkypeException 
	 * @throws CommunicationFailedException
	 */
	private Stream getStream(String userID) throws SkypeException
	{
		application.addApplicationListener(new ApplicationAdapter());

		Friend[] friends;
		friends = application.getAllConnectableFriends();
		Friend[] connectFriends = new Friend[friends.length];

		int i = 0;
		for (Friend friend : friends) {
			if (friend.getId().equals(userID))
				connectFriends[i++] = friend;
		}

		Stream[] streams;
		streams = application.connect(connectFriends);
		if (streams.length > 0) return streams[0];
		else return null;
	}

	/**
	 * Returns currently available peers in mean of their connectiability.
	 * For every ID only one Peer instance is returned (created once)
	 * during connection lifetime.
	 *
	 * @see ee.ut.f2f.comm.CommunicationLayer#getPeers()
	 */
	public Collection<Peer> getPeers() throws CommunicationFailedException
	{
		Friend[] friends;

		try {
			friends = application.getAllConnectableFriends();
		} catch (SkypeException e) {
			throw new CommunicationFailedException(e);
		}

		Peer[] peers = new Peer[friends.length];
		int i = 0;

		for (Friend friend : friends)
		{
			try {
				Peer peer = createOrFindPeer(friend.getId(), friend.getFullName());
				peers[i++] = peer;
			} catch (SkypeException e) {
				throw new CommunicationFailedException(e);
			}
		}

		return Arrays.asList(peers);
	}

	/**
	 * Sends message to specific peer presented by peer id. Every other
	 * sendMessage method is using this one.
	 * <p>
	 * There is two different paths in this method. When message is sent to
	 * local peer then it is directly forwarded to listener instances.
	 * <p>
	 * When message is sent to a remote peer then stream is asked from Skype
	 * API, message is converted into packet (into serialized and encoded
	 * String representation) and depending on packet size, it is sent as once
	 * or by pieces.
	 * <p>
	 * When sent by pieces, piece count is sent first and then pieces one by one.
	 * <p>
	 * Please note that session/connection is not closed after sending message in this method.
	 * This is caused by fact that closing connection before receiving end gets complete
	 * message will cause send/receive failure.
	 * @throws IOException 
	 * @throws SkypeException 
	 */
	void sendMessage(Object msgObj, String sID)
			throws CommunicationFailedException, IOException, SkypeException
	{
		SkypeMessage message = new SkypeMessage(1, msgObj);

		if (sID.equals(localPeer.getID()))
		{
			skypeServerAdapter.messageRecieved(msgObj, localPeer);
		}
		else
		{
			int maxSizeInBytes = MAX_PACKET_SIZE;
			Stream stream = getStream(sID);
			String msgPacket = message.createPacket();
			int messageSize = msgPacket.length();
			if (messageSize < maxSizeInBytes) stream.write("0;" + msgPacket); // Send in one piece
			else
			{
				int msgId = calculateNextMessageID();
				int msgCount = calculateMessagesCount(maxSizeInBytes, messageSize);

				stream.write("1;" + msgId + ";" + msgCount); // Send message start indicator
				for (int i = 0, j = 0, count = 0; i < messageSize; count++)
				{
					if (i + maxSizeInBytes > messageSize) j = messageSize;
					else j = i + maxSizeInBytes;
					stream.write("2;" + msgId + ";" + count + ";"
							+ msgPacket.substring(i, j)); // Send message pieces
					i = j;
					F2FDebug.println("\t\tSKYPE debug: id=" + msgId + ", cnt="
							+ count + " , msgSize=" + messageSize);
				}
			}
		}
	}

	/**
	 * Calculates messages needed to send one package.
	 *
	 * @param maxSizeInBytes
	 * @param messageSize
	 * @return
	 */
	private int calculateMessagesCount(int maxSizeInBytes, int messageSize) {
		return (int) Math.ceil((double) messageSize
				/ (double) maxSizeInBytes);
	}

	public String getID()
	{
		return SKYPE_LAYER_ID;
	}

}
