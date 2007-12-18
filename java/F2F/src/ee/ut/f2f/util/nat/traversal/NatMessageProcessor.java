package ee.ut.f2f.util.nat.traversal;

import java.util.Collection;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.threads.NatMessageSender;

public class NatMessageProcessor {
	
	final static private Logger log = Logger.getLogger(NatMessageProcessor.class);
	
	private ConnectionManager cm = null;
	
	public NatMessageProcessor(ConnectionManager cm){
		this.cm = cm;
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
			case NatMessage.COMMAND_IS_F2FPEER_IN_LIST : {
				log.debug("Received command [isPeerInList] from [" + nmsg.getFrom() + "]");
				F2FComputingGUI.forceSynchronization();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					log.error("Waiting for synchoniztion: interrupted sleep",e);
				}
				F2FPeer f2fPeer = F2FComputingGUI.controller.getFriendModel().getF2FPeerById(nmsg.getFrom());
				if(f2fPeer != null){
					log.debug("F2FPeer [" + f2fPeer.getDisplayName() + "] is in the list, sending report");
					nmsg = new NatMessage(F2FComputing.getLocalPeer().getID().toString(),
													 f2fPeer.getID().toString(),
													 NatMessage.REPORT_F2FPEER_IS_IN_LIST,
													 null);
					sendNatMessage(nmsg);
				} else {
					log.debug("F2FPeer [" + nmsg.getFrom() + "] is not in F2FList");
				}
				break;
			} 
			case NatMessage.COMMAND_TRY_CONNECT_TO : {
				log.debug("Received command [tryConnectTo] from peer [" + nmsg.getFrom() + "]");
				cm.getTCPTester(nmsg.getFrom()).tryConnectTo((Integer) nmsg.getContent());
				break;
			}
			
			//REPORT CASES
			case NatMessage.REPORT_F2FPEER_IS_IN_LIST : {
				log.debug("F2FPeer [" + nmsg.getFrom() + "] reports I'am in his list");
				//Send stun info request
				nmsg = new NatMessage(F2FComputing.getLocalPeer().getID().toString(),
									  nmsg.getFrom(),
									  NatMessage.COMMAND_GET_STUN_INFO,
									  null);
				sendNatMessage(nmsg);
				break;
			}
			case NatMessage.REPORT_STUN_INFO: {
				log.debug("Received StunInfo Report from [" + nmsg.getFrom() + "]");
				StunInfo sinf = (StunInfo) nmsg.getContent();
				//Insert
				//TODO Check if f2fpeer online but not added to F2Ffriends list
				
				if (F2FComputingGUI.controller.getStunInfoTableModel().get(sinf.getId()) == null) {
					F2FComputingGUI.controller.getStunInfoTableModel().add(sinf);
				   	log.debug("Adding " + sinf.getId() + " to StunInfoTable");
				   	Collection<F2FPeer> peers = F2FComputingGUI.controller.getFriendModel().getPeers();
				   	String displayName = null;
				   	for(F2FPeer peer : peers){
				   		if (sinf.getId().equals(peer.getID().toString())) displayName = peer.getDisplayName();
				   	}
				   	F2FComputingGUI.controller.writeNatLog("Received Stun info from [" + displayName + "]\n" + sinf.toString());
				   	
				   	// Test can use TCP?
				   	cm.initiateTCPTester(sinf.getId());
				}
				else {
					//Update
					/*
					log.debug(sinf.getId() + " StunInfo with id [" + sinf.getId() + "] allready exist in StunInfoTable");
					log.debug("Replacing StunInfo id [" + sinf.getId() + "]");
					F2FComputingGUI.controller.getStunInfoTableModel().remove(sinf.getId());
					F2FComputingGUI.controller.getStunInfoTableModel().add(sinf);
					log.debug("Updating SocketPeer in SocketCommunication Provider by id [" + sinf.getId() + "]");
					cm.getSocketCommunicationProvider().removeFriend(sinf.getId());
					cm.getSocketCommunicationProvider().addFriend(sinf.getLocalIp(), cm.getScPort());
					*/
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
