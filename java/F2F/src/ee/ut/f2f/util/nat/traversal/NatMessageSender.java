package ee.ut.f2f.util.nat.traversal;

import java.util.Collection;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;

public class NatMessageSender extends Thread implements Activity {
	
	final private static Logger log = Logger.getLogger(NatMessageSender.class);
	
	//actions
	final private static int SEND_MESSAGE = 1;
	final private static int SEND_MY_STUN_INFO = 2;
	
	private int action = -1;
	private NatMessage nmsg = null;
	private String toReceiver = null;
	
	/**
	 * Creates new NatMessageSender thread instance with NatMessage object to be sent
	 * After calling start() method of this thread, it tries to send this message
	 * @param nmsg NatMessage
	 */
	public NatMessageSender(NatMessage nmsg){
		super("NatMessageSenderThread");
		if (nmsg == null) throw new NullPointerException(this.getName() + " id [" + this.getId() + "]: null message exception");
		if (!(nmsg instanceof NatMessage)) throw new IllegalArgumentException(this.getName() + " id [" + this.getId() + 
				"]: argument should be instance of NatMessage"); 
		this.action = SEND_MESSAGE;
		this.nmsg = nmsg;
	}
	
	/**
	 * Creates new NatMessageSender thread instance with receiver's id
	 * After calling start() method this thread will try to send the StunInfo
	 * of this machine to receiver specified by String argument
	 * @param toReceiver String
	 */
	public NatMessageSender(String toReceiver){
		super("NatMessageSenderThread");
		if (toReceiver == null) throw new NullPointerException(this.getName() + " id [" + this.getId() + "]: null message exception");
		if (!(toReceiver instanceof String)) throw new IllegalArgumentException(this.getName() + " id [" + this.getId() + 
				"]: argument should be instance of String");
		this.action = SEND_MY_STUN_INFO;
	}
	
	
	
	public void run(){
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
		log.debug("Starting " + this.getName() + " ...");
		switch (action) {
			case SEND_MESSAGE:{
				log.debug(this.getName() + " id [" + this.getId() + "] action : send message");
				try {
					sendNatMessage(nmsg);
					log.debug(this.getName() + " id [" + this.getId() + "] Successfully sent message");
				} catch (CommunicationFailedException e) {
					log.error(this.getName() + " id [" + this.getId() + "] Unable to send message", e);
					ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED));
					return;
				}
				break;
			}
			case SEND_MY_STUN_INFO:{
				log.debug(this.getName() + " id [" + this.getId() + "] action : send stun info");
				try {
					sendMyStunInfo(toReceiver);
					log.debug(this.getName() + " id [" + this.getId() + "] Successfully sent StunInfo report to [" + toReceiver + "]");
				} catch (CommunicationFailedException e) {
					log.error(this.getName() + " id [" + this.getId() + "] Unable to send message", e);
					ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED));
					return;
				}
				break;
			}
		}
		log.debug("Stopping " + this.getName() + " id [" + this.getId() + "] thread");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FINISHED));
	}
	
	/*
	 * Prepare and send NatMessage
	 */
	private void sendNatMessage(NatMessage nmsg) throws  CommunicationFailedException{
		log.debug(this.getName() + " id [" + this.getId() + "] Preparing NAT message to send [" + nmsg.toString() + "]");
		Collection<F2FPeer> peers = F2FComputingGUI.controller.getFriendModel().getPeers();
		peers.add(F2FComputing.getLocalPeer());
		F2FPeer peer = null;
		for(F2FPeer p : peers){
			if(nmsg.getTo().equals(p.getID().toString())) peer = p;
		}		
		if (peer == null){
			throw new NullPointerException("No peers with [" + nmsg.getTo() + "] in contact list");
		}
			F2FMessage f2fmsg = new F2FMessage(F2FMessage.Type.NAT, null, null, null, nmsg);
			log.debug(this.getName() + " id [" + this.getId() + "] Sending NAT message [" + nmsg.toString() + "] to [" + peer.getID().toString() + "]");
			peer.sendMessage(f2fmsg);
	}
	
	/*
	 * Prepare and send StunInfo report
	 */
	private void sendMyStunInfo(String to) throws CommunicationFailedException{
		StunInfo sinf = F2FComputingGUI.natMessageProcessor.getConnectionManager().getLocalStunInfo(false);
		if (sinf == null) throw new NullPointerException(this.getName() + " id [" + this.getId() + "] StunInfo is null");
		log.debug(this.getName() + " id [" + this.getId() + "] preparing StunInfo report for [" + to + "]");
		NatMessage nmsg = new NatMessage(sinf.getId(),to,NatMessage.REPORT_STUN_INFO,sinf);
		if (nmsg == null) throw new NullPointerException(this.getName() + " id [" + this.getId() + "] NatMessage is null");
		log.debug(this.getName() + " id [" + this.getId() + "] sending StunInfo report to [" + to + "]");
		sendNatMessage(nmsg);
	}
	
	public String getActivityName() {
		return this.getName();
	}

	public Activity getParentActivity() {
		return null;
	}

}
