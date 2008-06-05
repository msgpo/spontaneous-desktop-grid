package ee.ut.f2f.gatherer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.gxl.GXLDocument;
import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FMessageListener;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.PeerPresenceListener;
import ee.ut.f2f.gatherer.parameters.SystemInformation;
import ee.ut.f2f.gatherer.util.GXLCreator;
import ee.ut.f2f.util.logging.Logger;

/**
 * Core class for F2F Gatherer. It provides methods for data gathering requests and responses.
 * @author Raido Türk
 *
 */
public class DataGathering implements F2FMessageListener, PeerPresenceListener{
	
	private static final Logger logger = Logger.getLogger(DataGathering.class);
	
	private Map<UUID,GXLDocument> data = new HashMap<UUID,GXLDocument>();
	private Map<UUID,FriendsDataSituation> dataRequests = new HashMap<UUID,FriendsDataSituation>();
	private Map<Long,Long> serverRequests = new HashMap<Long,Long>(); 
	private Map<UUID,String> localPeerBandwithData = new HashMap<UUID,String>();
	private static boolean requestInProgress = false;
	private static boolean gotAllData = false;
	private static final int queryMaxTimeInMilliSecondsSeconds = 10000; // 15 seconds
	private boolean isServer = false;
	
	private static DataGathering instance = null;
	
	public DataGathering() {
		F2FComputing.addPeerPresenceListener(this);
		F2FComputing.addMessageListener(F2FDataGatheringRequestMessage.class, this);
		F2FComputing.addMessageListener(F2FDataGatheringResponseMessage.class, this);
		F2FComputing.addMessageListener(F2FBandwidthTestRequestMessage.class, this);
		F2FComputing.addMessageListener(F2FBandwidthTestResponseMessage.class, this);
	}
		
	public static DataGathering getInstance() {
		if (instance == null)
			instance = new DataGathering();
		return instance;
	}
	
	public GXLDocument gatherAllData() {
		data = new HashMap<UUID,GXLDocument>(); //TODO: apply caching to ask valid data from cache
		isServer = true;
		long startTime = new Date().getTime();
		UUID localPeerId = F2FComputing.getLocalPeerID();
		FriendsDataSituation friendsData = dataRequests.get(localPeerId);
		if (friendsData == null)
			friendsData = new FriendsDataSituation();
		dataRequests.put(localPeerId, friendsData);
		sendDataGatheringRequestToFriends(localPeerId, null,startTime);
		gatherCurrentNodeData(localPeerId, null, null);

		while(!gotAllData) {
			if(System.currentTimeMillis()-startTime > DataGathering.queryMaxTimeInMilliSecondsSeconds) //expiring time
				break;
		}

		return assembleNetworkHierarchy();
	}
	
	private GXLDocument assembleNetworkHierarchy() {
		GXLDocument rootDocument = GXLCreator.createGXLDocumentWithoutNodes();
		rootDocument = GXLCreator.assembleNodeToRootDocument(rootDocument, data);
		return rootDocument;
	}
	
	
	private GXLDocument gatherCurrentNodeData(UUID requester, Map<String,List<String>> requesterAccounts, F2FDataGatheringRequestMessage msg) {
		requestInProgress = true;
		UUID localPeerId = F2FComputing.getLocalPeerID();
		int friends = gatherFriendsBandwidthData(localPeerId);
		while(localPeerBandwithData.size() != friends){
			
		}
		
		SystemInformation systemInfo = SystemInformation.getInstance(System.getProperty("os.name").toLowerCase());
		GXLDocument doc = systemInfo.gatherInformation(localPeerId.toString());
		List<String> connections = new ArrayList<String>();
		if(requesterAccounts != null) {
			connections = Gatherer.getInstance().findConnectionsWithPeer(requesterAccounts);
		}
		doc = GXLCreator.addConnectionsAndBandwidthsToGraph(localPeerId, doc, localPeerBandwithData, msg == null ? null : msg.getNextPeerUUIDInReturnHierarchy(), connections);		
		data.put(localPeerId, doc);
		
		if(!localPeerId.equals(requester)) { //if requester wasn't current peer, then send to requester
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				ObjectOutputStream oos = new ObjectOutputStream(out);
				doc.write(oos);
				F2FDataGatheringResponseMessage response = new F2FDataGatheringResponseMessage(
						requester, localPeerId, out.toByteArray(), msg.getCurrentRequestHierarchy());
				F2FComputing.getPeer(msg.getNextPeerUUIDInReturnHierarchy()).sendMessage(response);
			} catch (Exception e) {
				logger.error("Exception ", e);
			}
		}
		return doc;
	}
	
	/**
	 * Sends bandwith test request messages to all known F2F friends.
	 * @return number of peers this bandwith test request was sent
	 */
	private int gatherFriendsBandwidthData(UUID localPeer) {
		int reqSent = 0;
			for (final F2FPeer peer : F2FComputing.getPeers()) {
				if(!localPeer.equals(peer.getID())) {
					F2FBandwidthTestRequestMessage msg = new F2FBandwidthTestRequestMessage(peer);
					msg.setRequestDateSent(new Date());
					try {
						peer.sendMessage(msg);
						reqSent++;
					} catch (CommunicationFailedException e) {
						logger.error("Failed sending bandwidth test to peer: "+peer, e);
					}
				}
			}
		return reqSent;
	}
	
	/**
	 * Avoids an error described in: http://lists.xml.org/archives/xml-dev/200405/msg00149.html
	 * @param ois
	 * @return
	 * @throws Exception
	 */
	private synchronized GXLDocument getNewGXLDocument(ObjectInputStream ois) throws Exception{
		return new GXLDocument(ois);
	}
	
	/**
	 * 
	 * @param data
	 * @param requester
	 * @param sender
	 * @return
	 */
	private GXLDocument extractFriendData(F2FDataGatheringResponseMessage message, F2FPeer sender) {
		GXLDocument doc = null;
		try {
			//put data from friends into request map
			FriendsDataSituation friendsData = dataRequests.get(message.getRequesterPeerId());
			if(friendsData == null)
				friendsData = new FriendsDataSituation();
			Timestamp nodeSent = friendsData.dataSent.get(message.getResponseID());
			//if(nodeSent == null) {

					try {
						InputStream in = new ByteArrayInputStream((byte[]) message.getContent());
						ObjectInputStream ois = new ObjectInputStream(in);
						try {
							doc = getNewGXLDocument(ois); //without synchronization faster
						} catch (org.xml.sax.SAXException e) {
							//use synchronized method, if multiples threads trying to access DocumentBuilder
							e.printStackTrace();
							doc = getNewGXLDocument(ois); 
						}
						if(!F2FComputing.getLocalPeerID().equals(message.getRequesterPeerId())) {//if root request is not localpeer
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(out);
							doc.write(oos);
							F2FDataGatheringResponseMessage response = new F2FDataGatheringResponseMessage(
									message.getRequesterPeerId(), message.getResponseID(), out.toByteArray(), message.getCurrentRequestHierarchy());
							F2FComputing.getPeer(message.getNextPeerUUIDInReturnHierarchy()).sendMessage(response);
						}
					} catch (CommunicationFailedException e) {
						logger.error("Failed sending gathered data to peer: "+message.getRequesterPeerId(), e);
					} catch (Exception e) {
						e.printStackTrace();
					}
								
				friendsData.dataSent.put(message.getResponseID(), new Timestamp(System.currentTimeMillis()));
				dataRequests.put(message.getRequesterPeerId(), friendsData);
				GXLDocument existingNodeInfo;
				synchronized(this.data) {
					existingNodeInfo = this.data.get(message.getResponseID());
					if(existingNodeInfo != null){
						existingNodeInfo = GXLCreator.extractConnectionsIntoExistingDoc(existingNodeInfo, doc);
						this.data.put(message.getResponseID(), existingNodeInfo);
						//add connection data to node 
					} else {
							this.data.put(message.getResponseID(), doc); //cache also friend data in current node
					}
				}
			//}			
		} catch (Exception e) {
			
		}
		return doc;
	}
	
	private void checkCacheValidStatus() {
		//TODO: on new gathering request check cache valid status 
	}
	
	private boolean isCacheStillValid(UUID requesterId, UUID id) {
		FriendsDataSituation friendsData = dataRequests.get(requesterId);
		if (friendsData == null)
			return false;
		Timestamp nodeSent = friendsData.dataSent.get(id);
		if(nodeSent == null)
			return false;
		long currentTime = System.currentTimeMillis();
		//if(currentTime-nodeSent.getTime() >)  than valid cache time
			//return false;

		return true;
	}
	
	public void peerContacted(F2FPeer peer) {
		System.out.println("found peer: "+peer.getDisplayName());
		if(requestInProgress) { // if only request in progress then send data gathering request to peer
			System.out.println("sending data gathering request to peer");
			Map<String,List<String>> localPeerAccounts = localPeerProvidersToFriend();
			
			//FIXME: replace with real request root id
			//FIXME: replace with real start time
			long serverRequestStartTime = 0;
			
			F2FDataGatheringRequestMessage request = 
				new F2FDataGatheringRequestMessage(F2FComputing.getLocalPeerID(), localPeerAccounts, null, serverRequestStartTime);
			request.addPeerToRequestHierarchy(F2FComputing.getLocalPeerID());
			try {
				FriendsDataSituation friendsData = dataRequests.get(F2FComputing.getLocalPeerID());
				if (friendsData == null)
					friendsData = new FriendsDataSituation();
				friendsData.friends++;
				dataRequests.put(F2FComputing.getLocalPeerID(), friendsData);
				peer.sendMessage(request);
				F2FBandwidthTestRequestMessage msg = new F2FBandwidthTestRequestMessage(peer);
				msg.setRequestDateSent(new Date());
				peer.sendMessage(msg);
			} catch (CommunicationFailedException e) {
				logger.error("Communication failed with peer: "+peer.getDisplayName(), e);
			}
		}
	}

	public void peerUnContacted(F2FPeer peer) {

	}
	
	public void messageReceived(Object message, F2FPeer sender) {
		Date dateReceived = new Date();
		if(message instanceof F2FBandwidthTestRequestMessage) {
			F2FBandwidthTestRequestMessage msg = (F2FBandwidthTestRequestMessage) message;
			F2FBandwidthTestResponseMessage response = new F2FBandwidthTestResponseMessage(sender);
			response.setRequestDateSent(msg.getRequestDateSent());
			response.setFriendDateReceived(dateReceived);
			try {
				response.setFriendDateSent(new Date());
				sender.sendMessage(response);
			} catch (CommunicationFailedException e) {
				logger.error("Failed sending bandwidth test request msg to peer: "+sender.getDisplayName(),e);
			}
		} else if(message instanceof F2FBandwidthTestResponseMessage) {
			F2FBandwidthTestResponseMessage msg = (F2FBandwidthTestResponseMessage) message;
			msg.setResponseDateReceived(new Date());
			long difference = msg.getResponseDateReceived().getTime()-msg.getRequestDateSent().getTime(); // in ms
			if(difference == 0)
				difference = 1;
			long bytesPerSecond = F2FGathererMessage.BANDWITH_TEST_MSG_SIZE * 2 * 1000 / difference; //bytes/sec
			synchronized(localPeerBandwithData) {
				localPeerBandwithData.put(sender.getID(), String.valueOf(bytesPerSecond));
			}
		} else if (message instanceof F2FDataGatheringRequestMessage) {
			F2FDataGatheringRequestMessage msg = (F2FDataGatheringRequestMessage) message;
			Long serverTime = serverRequests.get(msg.getServerRequestStartTime());
			if(serverTime == null) {
				serverTime = msg.getServerRequestStartTime();
				serverRequests.put(serverTime, serverTime);
				FriendsDataSituation friendsData = dataRequests.get(msg.getRequesterPeerId());
				if (friendsData == null)
					friendsData = new FriendsDataSituation();
				int friendsCount = sendDataGatheringRequestToFriends(msg.getRequesterPeerId(), msg.getCurrentRequestHierarchy(), msg.getServerRequestStartTime());
				friendsData.friends = friendsCount;
				dataRequests.put(msg.getRequesterPeerId(), friendsData);
			} else {
				//FIXME: another data request came in during the same server request so we won't send request to friends to avoid looping
			}
			gatherCurrentNodeData(msg.getRequesterPeerId(), msg.getProviderAccounts(), msg);
		} else if (message instanceof F2FDataGatheringResponseMessage) {
			F2FDataGatheringResponseMessage msg = (F2FDataGatheringResponseMessage) message;
			System.out.println("got response from "+msg.getResponseID()+" through "+sender.getID());
			msg.removeLastPeerFromHierarchy(); //remove peer from which msg was received
			extractFriendData(msg, sender);
		}
	}
	
	/**
	 * Sends data gathering requests to all current peer friends, except peer, who requested it
	 * @param requester Initial peer, that needs friends data
	 * @return number of peers this data gathering request was sent
	 */
	private int sendDataGatheringRequestToFriends(UUID requester, List<UUID> requestHierarchy, long serverRequestStartTime) {
		int reqSent = 0;
		Map<String,List<String>> localPeerAccounts = localPeerProvidersToFriend();
			for (final F2FPeer peer : F2FComputing.getPeers()) {
				if(peer.getID().compareTo(requester) != 0 && 
						!F2FComputing.getLocalPeerID().equals(peer.getID())) { //exclude data requester from friends list to request data
					if(requestHierarchy == null) //if root peer
						requestHierarchy = new ArrayList<UUID>();
					
					if(!requestHierarchy.contains(peer.getID())) {//avoid endless looping
						F2FDataGatheringRequestMessage request = new F2FDataGatheringRequestMessage(requester, localPeerAccounts, requestHierarchy, serverRequestStartTime);
						request.addPeerToRequestHierarchy(F2FComputing.getLocalPeerID());
						try {
							//System.out.println("sending Data gathering request to friend: "+peer.getID().toString());
							peer.sendMessage(request);
							reqSent++;
						} catch (CommunicationFailedException e) {
							logger.error("Communication failed with peer: "+peer.getDisplayName(), e);
						}
					}
				}
			}
		return reqSent;
	}
	
	private Map<String,List<String>> localPeerProvidersToFriend() {
		return Gatherer.getInstance().returnAllPeerAccounts();
	}
	
	
	private class FriendsDataSituation {
		private Map<UUID,Timestamp> dataSent = new HashMap<UUID,Timestamp>();
		private int friends = 0;
		private boolean isAllDataReceived() {
			return dataSent.size() == friends;
		}
		
		
	}	
}
