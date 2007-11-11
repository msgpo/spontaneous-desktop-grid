package ee.ut.f2f.util.nat.traversal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.comm.sip.SipCommunicationLayer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FMessage;

//import org.apache.log4j.Logger;

//import ee.ut.f2f.ui.F2FComputingGUI;

public class NatMessageProcessor {
	
	//Logger log = Logger.getLogger(InfoMessageProcessor.class);
	static private NatLogger log = new NatLogger(NatMessageProcessor.class);

	
	//Type codes
	public static final int COMMAND_GET_STUN_INFO = 601;
	public static final int REPORT_STUN_INFO = 61;
	public static final int REPORT_BROKEN_MESSAGE = 60;
	
	
	
	
	
	
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
		log.debug("Processing Incoming NAT message : [" + nmsg.toString() + "]");
		/*
		switch(nmsg.getType()){
			case COMMAND : {
				switch(((Integer) nmsg.getContent()).intValue()){
					case GET_STUN_INFO: //@TODO Get Stun Info
				}
			}
			
			case REPORT :  {
				switch(nmsg.getContentType()){
					case STUN_INFO: /@TODO Process stun info
				}
			}
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
			
			F2FMessage f2fmsg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, encoded);
			log.debug("Incapsulating  NAT message into F2F message : [" + encoded + "]");
			log.debug("Sending encoded message [" + f2fmsg.toString() + "]");
			try {
				peer.sendMessage(f2fmsg);
			} catch (CommunicationFailedException e) {
				log.error("Unable to send f2f message [" + f2fmsg.toString() + "]", e);
				//@TODO communication failed handling
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
