package ee.ut.f2f.util;

import ee.ut.f2f.ui.F2FComputingGUI;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;

public class  SipMsgListener implements MessageListener {

	public void messageDelivered(MessageDeliveredEvent evt) {		
		System.out.println("F2F: Message out from: " + evt.getDestinationContact().getDisplayName()
				+ " Content: " + evt.getSourceMessage().getContent());
		
	}

	public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {

	}

	public void messageReceived(MessageReceivedEvent evt) {
		System.out.println("Message received:" + evt.getSourceMessage().getContent());
		String[] msg = evt.getSourceMessage().getContent().split(";", 4);
		
		if(msg[0].equals("F2F") == true) {
			F2FComputingGUI.controller.processMessage(msg[1], msg[2], msg[3], evt.getSourceContact());
		}
	}
}
