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

public class F2FTests {
	
	private static BundleContext bundleContext = null;

	public static void doTests() {
		// This is some space for writing some spikes and tests
		sendMessage();
		
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
		
		MetaContact mc = metaCListService.findMetaContactByMetaUID("119306544976325845065");
		Contact contact = mc.getDefaultContact();
		OperationSetBasicInstantMessaging im
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);
		Message msg = im.createMessage("Hello f2f-world!");
		im.sendInstantMessage(contact, msg);
		
		im.addMessageListener(new F2FTestsMessageListener() );
	}


}
