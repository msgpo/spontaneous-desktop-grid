package ee.ut.f2f.util;

import ee.ut.f2f.comm.sip.SipCommunicationLayer;
import ee.ut.f2f.ui.F2FComputingGUI;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;

public class  SipMsgListener implements MessageListener {

	static SipMsgListener listener;
	
	public static SipMsgListener getInstance() {
		if (SipMsgListener.listener == null)
			SipMsgListener.listener = new SipMsgListener();
		
		return SipMsgListener.listener;
	}
	
	
	public void messageDelivered(MessageDeliveredEvent evt) {		
		System.out.println("F2F: Message delivered to " + evt.getDestinationContact().getDisplayName()
				+ " Content: " + evt.getSourceMessage().getContent());
		
	}

	public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {

	}

	public void messageReceived(MessageReceivedEvent evt) {
		String[] msg = evt.getSourceMessage().getContent().split(";", 4);
		
		if(msg[0].equals("F2F") == true) {
			SipCommunicationLayer.getInstance().processMessage(msg[1], msg[2], msg[3], evt.getSourceContact());
		}
	}
}
