package ee.ut.f2f.util.nat.traversal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.comm.sip.SipCommunicationLayer;
import ee.ut.f2f.ui.F2FComputingGUI;

//import org.apache.log4j.Logger;

//import ee.ut.f2f.ui.F2FComputingGUI;

public class NatMessageProcessor {
	
	//Logger log = Logger.getLogger(InfoMessageProcessor.class);
	static private NatLogger log = new NatLogger(NatMessageProcessor.class);
	
	//Command codes
	static final int GET_STUN_INFO = 6001;
	
	//Content types
	static final int STUN_INFO = 601;
	static final int TEXT = 602;
	
	//Types
	static final int COMMAND = 61;
	static final int REPORT = 62;
	
	
	
	
	public static void processIncomingNatMessage(String encodedMessage){
		log.debug("Received NAT encoded message, length [" + encodedMessage.length() + "]");
		if (encodedMessage != null && !"".equals(encodedMessage)){
			try{
				NatMessage nmsg = new NatMessage((encodedMessage.startsWith("/NAT>/")) ? encodedMessage.substring(6) : encodedMessage);
				processMessage(nmsg);
			} catch (NatMessageException e) {
				log.error("Error parsing message [" + encodedMessage + "]", e);
				//@TODO: report failure, request resend
			}
		} else {
			log.debug("Discarding empty NAT message");
		}
	}
	
	private static void processMessage(NatMessage nmsg) {
		log.debug("Processing NAT message : [" + nmsg.toString() + "]");

		
		
		/*
		switch(nmsg.getType()){
			case COMMAND : //TODO execute
			case REPORT :  //TODO analize	
		}
		*/
	}
	
	public static void sendNatMessage(NatMessage nmsg){
		Peer localPeer = SipCommunicationLayer.getInstance().getLocalPeer(); 
		nmsg.setFrom(localPeer.getID());
		log.debug("Processing to send, NAT message [" + nmsg.toString() + "]");
		
		Collection<Peer> peers = F2FComputingGUI.controller.getFriendModel().getPeers();
		Peer peer = null;
		for(Peer p : peers){
			if(nmsg.getTo().equals(p.getID())) peer = p;
		}		
		
		String encoded = null;
		
		try {
			encoded = nmsg.encode();
		} catch (NatMessageException e) {
			log.error(e.getMessage(),e);
		}
		
		if(encoded != null){
			encoded = "/NAT>/" + encoded;
			log.debug("Sending encoded message [" + encoded + "]");
			try {
				peer.sendMessage(encoded);
			} catch (CommunicationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	/**
	 * @deprecated
	 * We do not need to parse
	 * We do not exchange parameter=arg anymore, we exchange NatMessage objects
	 * @param params
	 * @return
	 * @throws NatMessageException
	 */
	@Deprecated
	public Map<String,String> parseParams(String params) throws NatMessageException{
		Hashtable<String,String> map = new Hashtable<String,String>();
		log.debug("Params : [" + params + "]");
		String [] pairs = params.split("&");
		log.debug("Params pairs " + Arrays.toString(pairs));
		for (String s : pairs){
			String parameter = null;
			String args = null;
			try{
				parameter = s.split("=")[0];
				args = s.split("=")[1];
			} catch (ArrayIndexOutOfBoundsException e){
				log.debug("[" + s + "] contains null values");
			}
			if (parameter != null && args != null){
				if(!map.containsKey(parameter)) map.put(parameter, args);
				else {
					log.debug("contains parameters of the same type, [" + parameter + "] throwing exception");
					throw new NatMessageException("Message contains parameters of the same type : [" + parameter + "]");
				}
			} else {
				log.debug("Discarding null values [" + parameter + "=" + args + "]");
			}
		}
		return map;
	}
}
