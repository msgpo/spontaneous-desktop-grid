package ee.ut.f2f.util.nat.traversal;


import java.util.Collection;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.exceptions.NatMessageException;

public class NatMessageProcessor {
	
	//Logger log = Logger.getLogger(InfoMessageProcessor.class);
	static private Logger log = Logger.getLogger(NatMessageProcessor.class);

	
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
				
					String localId = F2FComputing.getLocalPeer().getID().toString();
					sinf = F2FComputingGUI.controller.getStunInfoTableModel().get(localId);
					

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
			
			case NatMessage.COMMAND_TRY_CONNECT_TO :{
				String from = nmsg.getFrom();
				String to = nmsg.getTo();
				log.debug("Received tryConnectTo from [" + from + "]");
				
				
				//Content in form of 
				String content = (String) nmsg.getContent();
				
				if (content == null) throw new NullPointerException("Empty NatMessage content, shouldbe ip:port"); 
				//if (content.split(":").length < 2) throw 
				
				//IP and port to connect to
				String ip = content.split(":")[0];
				int port = Integer.parseInt(content.split(":")[1]);
				
				
				break;
			}
			
			//REPORT CASES
			case NatMessage.REPORT_STUN_INFO: {
				log.debug("Received StunInfo Report from [" + nmsg.getFrom() + "]");
				StunInfo sinf = (StunInfo) nmsg.getContent();
				if (F2FComputingGUI.controller.getStunInfoTableModel().get(sinf.getId()) == null) {
					F2FComputingGUI.controller.getStunInfoTableModel().add(sinf);
				   	log.debug("Adding " + sinf.getId() + " to StunInfoTable");
				   	Collection<F2FPeer> peers = F2FComputingGUI.controller.getFriendModel().getPeers();
				   	String displayName = null;
				   	for(F2FPeer peer : peers){
				   		if (sinf.getId().equals(peer.getID().toString())) displayName = peer.getDisplayName();
				   	}
				   	F2FComputingGUI.controller.writeNatLog("Received Stun info from [" + displayName + "]\n" + sinf.toString());
				}
				else {
					log.debug(sinf.getId() + " not added, it already exists in StunInfoTable");
				}
					
				log.debug("StunInfo " + sinf.toString());
				break;
			}
		}
		
	}
	
	public static void sendNatMessage(NatMessage nmsg){
		log.debug("Processing to send, NAT message [" + nmsg.toString() + "]");
		
		Collection<F2FPeer> peers = F2FComputingGUI.controller.getFriendModel().getPeers();
		peers.add(F2FComputing.getLocalPeer());
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
