package ee.ut.f2f.util.nat.traversal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FMessage;

public class NatMessageProcessor {
	
	//Logger log = Logger.getLogger(InfoMessageProcessor.class);
	static private NatLogger log = new NatLogger(NatMessageProcessor.class);

	
	public static void processIncomingNatMessage(String encodedMessage){
		log.debug("Received NAT encoded message, length [" + encodedMessage.length() + "]");
		
		//remove /NAT>/ prefix
		encodedMessage = (encodedMessage.startsWith("/NAT>/")) ? encodedMessage.substring(6) : encodedMessage;
		//log.debug("Prefix removed [" + encodedMessage + "]");
		//remove  /NAT>:contact/ suffix
		String to = null;
		try{
			if (encodedMessage != null){
				//String[] temp = encodedMessage.split("/NAT>/");
				//log.debug("Splitted size [" + temp.length + "]");
				to = encodedMessage.split("/NAT>/")[1];
				encodedMessage = encodedMessage.split("/NAT>/")[0];
			}
		} catch (ArrayIndexOutOfBoundsException e){
			encodedMessage = null;
		}
		
		//log.debug("Suffix removed, from [" + from + "], encoded [" + encodedMessage + "]");
		
		if (encodedMessage != null && !"".equals(encodedMessage)){
			try{
				NatMessage nmsg = new NatMessage(encodedMessage);
				nmsg.setTo(to);
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
		
		switch(nmsg.getType()){
			//COMMAND CASES
			case NatMessage.COMMAND_GET_STUN_INFO : {
				log.debug("Receved getStunInfo from [" + nmsg.getFrom() + "]");
			    StunInfo sinf = ConnectionManager.getStunInfo();
			    log.debug("Prepared StunInfo  " + sinf.toString());
				//preparesendback
				String from = nmsg.getTo();
				nmsg.setTo(nmsg.getFrom());
				nmsg.setFrom(from);
				//prepare content
				nmsg.setType(NatMessage.REPORT_STUN_INFO);
				nmsg.setContent(sinf);
				log.debug("Prepared StunInfo Report for [" + nmsg.getTo() + "]");
				//sendOut
				sendNatMessage(nmsg);
			}
			//REPORT CASES
			case NatMessage.REPORT_STUN_INFO: {
				log.debug("Received StunInfo Report from [" + nmsg.getFrom() + "]");
				StunInfo sinf = (StunInfo) nmsg.getContent();
				log.debug("StunInfo " + sinf.toString());
			}
		}
		
	}
	
	public static void sendNatMessage(NatMessage nmsg){
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
