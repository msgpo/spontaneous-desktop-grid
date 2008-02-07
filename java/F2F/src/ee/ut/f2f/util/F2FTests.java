//package ee.ut.f2f.util;
//
//import java.util.Iterator;
//
//import javax.swing.JOptionPane;
//
//import net.java.sip.communicator.service.contactlist.MetaContact;
//import net.java.sip.communicator.service.contactlist.MetaContactGroup;
//import net.java.sip.communicator.service.contactlist.MetaContactListService;
//import net.java.sip.communicator.service.protocol.Contact;
//import net.java.sip.communicator.service.protocol.Message;
//import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
//
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.ServiceReference;
//
//import ee.ut.f2f.comm.CommunicationFailedException;
//import ee.ut.f2f.core.F2FComputing;
//import ee.ut.f2f.core.F2FPeer;
//import ee.ut.f2f.ui.ChatMessage;
//
//public class F2FTests {
//	
//	private static BundleContext bundleContext = null;
//
//	public static void doTests() {
//		// This is some space for writing some spikes and tests
//		//sendMessage();
//		
//		//testMaxMessageSize();
//		
//		//testAskForCPU();
//	}
//
//	public static void setBundleContext(BundleContext bc) {
//		bundleContext = bc;
//	}
//
//	static void sendMessage() {
//        ServiceReference clistReference = bundleContext
//        	.getServiceReference(MetaContactListService.class.getName());
//
//        MetaContactListService metaCListService = (MetaContactListService) bundleContext
//            .getService(clistReference);
//        
//        
//        Iterator metaContactsInRoot = metaCListService.getRoot().getChildContacts();
//		while (metaContactsInRoot.hasNext())
//		{
//			MetaContact mc = (MetaContact)metaContactsInRoot.next();			
//			System.out.print(mc.getDisplayName());
//			System.out.println(" - " + mc.getMetaUID());
//		}
//		Iterator subGroups = metaCListService.getRoot().getSubgroups();
//		while (subGroups.hasNext()) {
//			MetaContactGroup mcg = (MetaContactGroup)subGroups.next();
//	        Iterator metaContactsInGroup = mcg.getChildContacts();
//			while (metaContactsInGroup.hasNext())
//			{
//				MetaContact mc = (MetaContact)metaContactsInGroup.next();
//				System.out.print(mc.getDisplayName());
//				System.out.println(" - " + mc.getMetaUID());
//			}
//		}
//		
//		MetaContact mc = metaCListService.findMetaContactByMetaUID("119315350401331541880");
//		Contact contact = mc.getDefaultContact();
//		OperationSetBasicInstantMessaging im
//            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
//                .getOperationSet(OperationSetBasicInstantMessaging.class);
//		Message msg = im.createMessage("Hello f2f-world!");	
//		im.sendInstantMessage(contact, msg);
//		im.addMessageListener(new F2FTestsMessageListener() );
//		
//	}
//
//	/**
//	 * This method tests how big messages can be sent in one piece
//	 */
//	static void testMaxMessageSize()
//	{
//		String msg1 = "1";
//		for (int i = 1; i < 1048576; i*= 2)
//			msg1 += msg1;
//		//String msg2 = msg1.substring(0, msg1.length()/10);
//		//while (msg1.length() < 1677718)
//		//	msg1 += msg2; 
//		String msg = msg1;
//		F2FPeer peer;
//		try {
//			peer = F2FComputing.getPeers().iterator().next();
//			while (true)
//			{
//				ChatMessage fmsg = new ChatMessage(msg);
//				peer.sendMessage(fmsg);
//				F2FDebug.println("sent string of length " + msg.length());
//				//msg += msg2;
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		} catch (CommunicationFailedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	static void testAskForCPU()
//	{
//		int n = JOptionPane.showConfirmDialog(
//                null, "Would you like green eggs and ham?",
//                "", JOptionPane.YES_NO_OPTION);
//		F2FDebug.println("" + n + " " + (n == JOptionPane.YES_OPTION ? "yes" : "no"));
//	}
//}
