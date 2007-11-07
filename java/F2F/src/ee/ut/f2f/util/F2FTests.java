package ee.ut.f2f.util;

import java.util.Iterator;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.event.MessageListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.core.F2FComputing;

public class F2FTests {
	
	private static BundleContext bundleContext = null;

	public static void doTests() {
		// This is some space for writing some spikes and tests
		//sendMessage();
		
		testMaxMessageSize();
	}

	public static void setBundleContext(BundleContext bc) {
		bundleContext = bc;
	}

	static void sendMessage() {
        ServiceReference clistReference = bundleContext
        	.getServiceReference(MetaContactListService.class.getName());

        MetaContactListService metaCListService = (MetaContactListService) bundleContext
            .getService(clistReference);
        
        
        Iterator metaContactsInRoot = metaCListService.getRoot().getChildContacts();
		while (metaContactsInRoot.hasNext())
		{
			MetaContact mc = (MetaContact)metaContactsInRoot.next();			
			System.out.print(mc.getDisplayName());
			System.out.println(" - " + mc.getMetaUID());
		}
		Iterator subGroups = metaCListService.getRoot().getSubgroups();
		while (subGroups.hasNext()) {
			MetaContactGroup mcg = (MetaContactGroup)subGroups.next();
	        Iterator metaContactsInGroup = mcg.getChildContacts();
			while (metaContactsInGroup.hasNext())
			{
				MetaContact mc = (MetaContact)metaContactsInGroup.next();
				System.out.print(mc.getDisplayName());
				System.out.println(" - " + mc.getMetaUID());
			}
		}
		
		MetaContact mc = metaCListService.findMetaContactByMetaUID("119315350401331541880");
		Contact contact = mc.getDefaultContact();
		OperationSetBasicInstantMessaging im
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);
		Message msg = im.createMessage("Hello f2f-world!");	
		im.sendInstantMessage(contact, msg);
		im.addMessageListener(new F2FTestsMessageListener() );
		
	}

	static void testMaxMessageSize()
	{
		String msg = "1";
		Peer peer;
		try {
			peer = F2FComputing.getPeers().iterator().next();
			while (true)
			{
				F2FMessage fmsg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, msg);
				peer.sendMessage(fmsg);
				msg += msg;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (CommunicationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
