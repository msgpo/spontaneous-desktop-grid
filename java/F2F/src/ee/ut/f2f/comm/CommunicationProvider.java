package ee.ut.f2f.comm;

import java.util.UUID;

import ee.ut.f2f.core.CommunicationFailedException;

public interface CommunicationProvider
{

	/**
	 * This value defines which communication provider is used if
	 * a peer is reachable via multiple ones at a time.
	 * The greater the weight, the quicker/more reliable the connection should be.
	 * 
	 * For instance, the lowest priority communication provider should be 
	 * the tagged IM connectin and the highest priority should be 
	 * the socket connection. 
	 * 
	 * @return The weight/priority of a communicatin provider.
	 */
	int getWeight();
	static final int SIP_IM_COMM_WEIGHT = 10;
	static final int TCP_COMM_WEIGHT = 1000;
	static final int UDP_COMM_WEIGHT = 500;
	
	void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException;
}