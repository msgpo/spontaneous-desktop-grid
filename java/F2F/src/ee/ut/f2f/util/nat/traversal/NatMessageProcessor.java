package ee.ut.f2f.util.nat.traversal;

import java.util.Collection;

import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.exceptions.NatMessageException;
import ee.ut.f2f.util.nat.traversal.threads.NatMessageSender;

public class NatMessageProcessor {
	
	final static private Logger log = Logger.getLogger(NatMessageProcessor.class);
	
	private ConnectionManager cm = null;
	
	public NatMessageProcessor(ConnectionManager cm){
		this.cm = cm;
	}
	
	@Deprecated
	public void processIncomingNatMessage(String encodedMessage){
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
	
	public void processIncomingNatMessage(NatMessage nmsg){
		processMessage(nmsg);
	}
	
	private void processMessage(NatMessage nmsg){
		if (nmsg == null){
			log.debug("Null argument NatMessage");
			throw new NullPointerException("Null argument NatMessage");
		}
		
		log.debug("Processing Incoming NAT message : [" + nmsg.toString() + "]");
		
		switch(nmsg.getType()){
			//COMMAND CASES
			case NatMessage.COMMAND_GET_STUN_INFO : {
				log.debug("Received command [getStunInfo] from [" + nmsg.getFrom() + "]");
				sendMyStunInfo(nmsg.getFrom());
				break;
			}
			
			//REPORT CASES
			case NatMessage.REPORT_STUN_INFO: {
				log.debug("Received StunInfo Report from [" + nmsg.getFrom() + "]");
				StunInfo sinf = (StunInfo) nmsg.getContent();
				//Insert
				if (F2FComputingGUI.controller.getStunInfoTableModel().get(sinf.getId()) == null) {
					F2FComputingGUI.controller.getStunInfoTableModel().add(sinf);
				   	log.debug("Adding " + sinf.getId() + " to StunInfoTable");
				   	Collection<F2FPeer> peers = F2FComputingGUI.controller.getFriendModel().getPeers();
				   	String displayName = null;
				   	for(F2FPeer peer : peers){
				   		if (sinf.getId().equals(peer.getID().toString())) displayName = peer.getDisplayName();
				   	}
				   	F2FComputingGUI.controller.writeNatLog("Received Stun info from [" + displayName + "]\n" + sinf.toString());
				   	
				   	//TODO Analyze received StunInfo (is in the same local network ?)

				   	//Add to socket communication layer
				   	cm.addToSocketCommunicationProvider(sinf);
				}
				else {
					//Update
					log.debug(sinf.getId() + " StunInfo with id [" + sinf.getId() + "] allready exist in StunInfoTable");
					log.debug("Replacing StunInfo id [" + sinf.getId() + "]");
					F2FComputingGUI.controller.getStunInfoTableModel().remove(sinf.getId());
					F2FComputingGUI.controller.getStunInfoTableModel().add(sinf);
					log.debug("Updating SocketPeer in SocketCommunication Provider by id [" + sinf.getId() + "]");
					cm.getSocketCommunicationProvider().removeFriend(sinf.getId());
					cm.getSocketCommunicationProvider().addFriend(sinf.getLocalIp(), cm.getScPort());
				}
				break;
			}
		}
		
	}
	
	public void sendNatMessage(NatMessage nmsg){
		NatMessageSender nmsgs = new NatMessageSender(nmsg);
		nmsgs.start();
	}
	
	public void sendMyStunInfo(String toReceiver){
		NatMessageSender nmsgs = new NatMessageSender(toReceiver);
		nmsgs.start();
	}

	public ConnectionManager getConnectionManager() {
		return cm;
	}

	public void setConnectionManager(ConnectionManager cm) {
		this.cm = cm;
	}
}
