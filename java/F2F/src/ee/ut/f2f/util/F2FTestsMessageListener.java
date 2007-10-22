package ee.ut.f2f.util;

import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;

public class F2FTestsMessageListener implements MessageListener {

	public void messageDelivered(MessageDeliveredEvent evt) {
		System.out.println("F2F: Message out from: " + evt.getDestinationContact().getDisplayName()
				+ " Content: " + evt.getSourceMessage().getContent());
		
	}

	public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {

	}

	public void messageReceived(MessageReceivedEvent evt) {
		System.out.println("F2F: Message in from: " + evt.getSourceContact().getDisplayName()
				+ " Content: " + evt.getSourceMessage().getContent());

	}

}
