package ee.ut.f2f.gatherer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ee.ut.f2f.core.F2FPeer;

/**
 * 
 * @author Raido Türk
 *
 */
public class F2FGathererMessage implements IF2FMessageTiming, Serializable {
	
	final static int BANDWITH_TEST_MSG_SIZE = 65535;
	
	public enum Type {
		GATHER_DATA_REQUEST,
		GATHER_DATA_RESPONSE,
		BANDWITH_TEST_REQUEST,
		BANDWITH_TEST_RESPONSE
	}
	
	protected Type type;
	Type getType() { return type; }	
	
	private UUID peerID = null;
	private UUID responseID = null;
	private Object content = null;
    private String messageUID = null;
    private long serverRequestStartTime;
    private Date requestDateSent;
    private Date responseDateReceived;
    private Date friendDateReceived;
    private Date friendDateSent;
    private Map<String,List<String>> providerAccounts = null;
    private List<UUID> requestHierarchy = new ArrayList<UUID>();
    	
	F2FGathererMessage(F2FPeer peer, Object content, Type type) {
		this.peerID = peer.getID();
		this.type = type;
		this.content = content;
		messageUID = String.valueOf(System.currentTimeMillis())
				+ String.valueOf(hashCode());
	}
	
	F2FGathererMessage(UUID peerId, UUID responseId, Object content, Type type, List<UUID> reqHierarchy) {
		this.peerID = peerId;
		this.responseID = responseId;
		this.type = type;
		this.content = content;
		if(reqHierarchy != null && reqHierarchy.size() > 0)
			this.requestHierarchy.addAll(reqHierarchy);
		messageUID = String.valueOf(System.currentTimeMillis())
				+ String.valueOf(hashCode());
	}
	
	
	F2FGathererMessage(UUID peerId, Object content, Type type, Map<String, List<String>> providerAccounts, List<UUID> reqHierarchy) {
		this.peerID = peerId;
		this.type = type;
		this.content = content;
		this.providerAccounts = providerAccounts;
		if(reqHierarchy != null && reqHierarchy.size() > 0)
			this.requestHierarchy.addAll(reqHierarchy);
		messageUID = String.valueOf(System.currentTimeMillis())
				+ String.valueOf(hashCode());
	}
	
	F2FGathererMessage(UUID peerId, Type type, Map<String, List<String>> providerAccounts, List<UUID> reqHierarchy, long serverRequestStartTime) {
		this.peerID = peerId;
		this.type = type;
		this.providerAccounts = providerAccounts;
		this.serverRequestStartTime = serverRequestStartTime;
		if(reqHierarchy != null && reqHierarchy.size() > 0)
			this.requestHierarchy.addAll(reqHierarchy);
		messageUID = String.valueOf(System.currentTimeMillis())
				+ String.valueOf(hashCode());
	}
	
	
	public UUID getNextPeerUUIDInReturnHierarchy() {
		if(requestHierarchy.size() > 0)
			return requestHierarchy.get(requestHierarchy.size()-1);
		return null;
	}
	
	public void removeLastPeerFromHierarchy() {
		if(requestHierarchy.size() > 0)
			requestHierarchy.remove(requestHierarchy.size()-1);
	}
	
	public List<UUID> getCurrentRequestHierarchy() {
		return requestHierarchy;
	}
	
	public void addPeerToRequestHierarchy(UUID peer) {
		requestHierarchy.add(peer);
	}
	
    /**
	 * Returns the content of this message if representable in text form or null
	 * if this message does not contain text data.
	 * 
	 * @return a String containing the content of this message or null if the
	 *         message does not contain data representable in text form.
	 */
    public Object getContent() {
    	return content;
    }

    /**
     * Returns a unique identifier of this message.
     * @return a String that uniquely represents this message in the scope of
     * this protocol.
     */
    public String getMessageUID() {
    	return messageUID;
    }

	/**
	 * @return the requestDateSent
	 */
	public Date getRequestDateSent() {
		return requestDateSent;
	}

	/**
	 * @param requestDateSent the requestDateSent to set
	 */
	public void setRequestDateSent(Date requestDateSent) {
		this.requestDateSent = requestDateSent;
	}

	/**
	 * @return the responseDateReceived
	 */
	public Date getResponseDateReceived() {
		return responseDateReceived;
	}

	/**
	 * @param responseDateReceived the responseDateReceived to set
	 */
	public void setResponseDateReceived(Date responseDateReceived) {
		this.responseDateReceived = responseDateReceived;
	}
	
	/**
	 * @return the friendDateReceived
	 */
	public Date getFriendDateReceived() {
		return friendDateReceived;
	}

	/**
	 * @param friendDateReceived the friendDateReceived to set
	 */
	public void setFriendDateReceived(Date friendDateReceived) {
		this.friendDateReceived = friendDateReceived;
	}

	/**
	 * @return the friendDateSent
	 */
	public Date getFriendDateSent() {
		return friendDateSent;
	}

	/**
	 * @param friendDateSent the friendDateSent to set
	 */
	public void setFriendDateSent(Date friendDateSent) {
		this.friendDateSent = friendDateSent;
	}

	/**
	 * @return the requesterPeer
	 */
	public UUID getRequesterPeerId() {
		return peerID;
	}

	/**
	 * @param requesterPeer the requesterPeer to set
	 */
	public void setRequesterPeerId(UUID peerId) {
		this.peerID = peerId;
	}

	public UUID getResponseID() {
		return responseID;
	}

	public void setResponseID(UUID responseID) {
		this.responseID = responseID;
	}

	/**
	 * @return the providerAccounts
	 */
	public Map<String, List<String>> getProviderAccounts() {
		return providerAccounts;
	}


	/**
	 * @param providerAccounts the providerAccounts to set
	 */
	public void setProviderAccounts(Map<String, List<String>> providerAccounts) {
		this.providerAccounts = providerAccounts;
	}

	public long getServerRequestStartTime() {
		return serverRequestStartTime;
	}

	public void setServerRequestStartTime(long serverRequestStartTime) {
		this.serverRequestStartTime = serverRequestStartTime;
	}

}

class F2FDataGatheringRequestMessage extends F2FGathererMessage{
	
	F2FDataGatheringRequestMessage(UUID peerId, Map<String,List<String>> providerAccounts, List<UUID> reqHierarchy, long serverRequestStartTime) {
		super(peerId, Type.GATHER_DATA_REQUEST, providerAccounts, reqHierarchy, serverRequestStartTime);
	}
	
}

class F2FDataGatheringResponseMessage extends F2FGathererMessage{
	
	F2FDataGatheringResponseMessage(UUID peerId, UUID responseId, Object data, List<UUID> reqHierarchy) {
		super(peerId, responseId, data, Type.GATHER_DATA_RESPONSE, reqHierarchy);
	}
	
}

class F2FBandwidthTestRequestMessage extends F2FGathererMessage{
	
	F2FBandwidthTestRequestMessage(F2FPeer peer) {
		super(peer,new byte[BANDWITH_TEST_MSG_SIZE], Type.BANDWITH_TEST_REQUEST);
	}
	
}

class F2FBandwidthTestResponseMessage extends F2FGathererMessage{
	
	F2FBandwidthTestResponseMessage(F2FPeer peer) {
		super(peer,new byte[BANDWITH_TEST_MSG_SIZE], Type.BANDWITH_TEST_RESPONSE);
	}
	
}
