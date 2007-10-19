package ee.ut.f2f.util;

import java.util.Iterator;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

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
        
        Iterator metaContacts = metaCListService.getRoot().getChildContacts();
		while (metaContacts.hasNext())
		{
			MetaContact mc = (MetaContact)metaContacts.next();
			System.out.println(mc.getDisplayName());
		}
		Iterator subGroups = metaCListService.getRoot().getSubgroups();
		/*while (subGroups.hasNext()) {
			MetaContactGroup mc = (MetaContactGroup)subGroups.next();
			
		}*/
		
		/* OperationSetBasicInstantMessaging im
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);
 Message msg = im.createMessage(text);
 im.sendInstantMessage(contact, msg); */
		
	}


}
