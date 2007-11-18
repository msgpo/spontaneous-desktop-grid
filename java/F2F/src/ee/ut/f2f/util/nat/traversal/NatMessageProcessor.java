package ee.ut.f2f.util.nat.traversal;


import java.util.Collection;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.nat.traversal.exceptions.ConnectionManagerException;
import ee.ut.f2f.util.nat.traversal.exceptions.NatMessageException;
import ee.ut.f2f.util.nat.traversal.exceptions.NetworkDiscoveryException;

public class NatMessageProcessor {
	
	//Logger log = Logger.getLogger(InfoMessageProcessor.class);
	static private NatLogger log = new NatLogger(NatMessageProcessor.class);

	
	public static void processIncomingNatMessage(String encodedMessage){
		log.debug("Received NAT encoded message, length [" + encodedMessage.length() + "]");
		
		//remove /NAT>/ prefix
		encodedMessage = (encodedMessage.startsWith("/NAT>/")) ? encodedMessage.substring(6) : encodedMessage;
		NatMessage nmsg = null;
		if (encodedMessage != null && !"".equals(encodedMessage)){
			try{
				nmsg = new NatMessage(encodedMessage);
			} catch (NatMessageException e) {
				log.error("Error parsing message [" + encodedMessage + "]", e);
				e.printStackTrace();
				//@TODO: report failure, request resend
			}
		} else {
			log.debug("Discarding empty NAT message");
		}

		processMessage(nmsg);

	}
	
	private static void processMessage(NatMessage nmsg){
		if (nmsg == null){
			log.debug("Null argument NatMessage");
			throw new NullPointerException("Null argument NatMessage");
		}
		
		log.debug("Processing Incoming NAT message : [" + nmsg.toString() + "]");
		
		switch(nmsg.getType()){
			//COMMAND CASES
			case NatMessage.COMMAND_GET_STUN_INFO : {
				String from = nmsg.getFrom();
				String to = nmsg.getTo();
				log.debug("Received getStunInfo from [" + from + "]");
			    
				StunInfo sinf = null;
				try{
					sinf = ConnectionManager.startNetworkDiscovery("stun.xten.net", 3478);
				} catch (NetworkDiscoveryException e){
					//TODO workaround if could not get the stun info from server
					log.error("Could not get the stun info from server", e);
					e.printStackTrace();
				} catch (ConnectionManagerException e){
					//TODO another exceptions
					log.error("Error getting stun info", e);
					e.printStackTrace();
				}
				//set Id
			    sinf.setId(nmsg.getTo());
			    log.debug("Prepared StunInfo  " + sinf.toString());
				
			    //prepare and send report
				nmsg.setTo(from);	//reverse to -> from
				nmsg.setFrom(to);	//reverse from -> to
				
				//prepare content
				nmsg.setType(NatMessage.REPORT_STUN_INFO);
				nmsg.setContent(sinf);
				log.debug("Prepared StunInfo Report Message for [" + nmsg.getTo() + "]");
				//sendOut
				sendNatMessage(nmsg);
				break;
			}
			//REPORT CASES
			case NatMessage.REPORT_STUN_INFO: {
				log.debug("Received StunInfo Report from [" + nmsg.getFrom() + "]");
				StunInfo sinf = (StunInfo) nmsg.getContent();
				log.debug("StunInfo " + sinf.toString());
				break;
			}
		}
		
	}
	
	public static void sendNatMessage(NatMessage nmsg){
		log.debug("Processing to send, NAT message [" + nmsg.toString() + "]");
		
		Collection<F2FPeer> peers = F2FComputingGUI.controller.getFriendModel().getPeers();
		F2FPeer peer = null;
		for(F2FPeer p : peers){
			log.debug("Peers by ID [" + p.getID().toString() + "]");
			if(nmsg.getTo().equals(p.getID().toString())) peer = p;
		}		
		if (peer == null){
			//TODO No peers found 
			log.debug("Not peer found in friends list by ID [" + nmsg.getTo() + "]");
			return;
		}
		
		String encoded = null;
		
		try {
			encoded = nmsg.encode();
		} catch (NatMessageException e) {
			log.error(e.getMessage(),e);
			e.printStackTrace();
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
				e.printStackTrace();
			}
		}
	}

}
