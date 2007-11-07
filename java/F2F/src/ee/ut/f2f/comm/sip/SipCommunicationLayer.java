/**
 * 
 */
package ee.ut.f2f.comm.sip;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;

import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import ee.ut.f2f.comm.CommunicationException;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.Util;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactlist.event.MetaContactEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactGroupEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;
import net.java.sip.communicator.service.contactlist.event.MetaContactMovedEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactRenamedEvent;
import net.java.sip.communicator.service.contactlist.event.ProtoContactEvent;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class SipCommunicationLayer 
	implements 	CommunicationLayer,
				ServiceListener,
				ContactPresenceStatusListener, MessageListener
{
	private Collection<Peer> m_chatPeers;
	private Contact m_chatHost;
	
	/**
	 * Name that identifies Sip communication layer.
	 */
	private static final String SIP_LAYER_ID = "F2FSipCommLayer";
	
	/**
	 * PeerID (account ID) -> Peer
	 */
	private Hashtable<String, Peer> peersHash = null;
	public Collection<Peer> getPeers() { return peersHash.values(); }
	private SipPeer localPeer = null;
	private Collection<CommunicationListener> listeners = new ArrayList<CommunicationListener>();

	private static SipCommunicationLayer siplayer = null;
	public static SipCommunicationLayer getInstance() { return siplayer; }
	
	public static SipCommunicationLayer initiateSipCommunicationLayer(BundleContext bc) throws CommunicationException
	{
		if (siplayer!=null) 
			throw new CommunicationInitException("SIP layer already initiated, initiateSipCommunicationLayer() was called more than once!");		
		
		// Create the F2F layer
		return (siplayer = new SipCommunicationLayer(bc));
	}
	
	private BundleContext bundleContext = null;
	private SipCommunicationLayer(BundleContext bc) throws CommunicationException
	{
		m_chatPeers = new ArrayList<Peer>();
		peersHash = new Hashtable<String, Peer>();
		// init Sip
		F2FDebug.println("\t\tInitializing SIP communication layer ...");
		this.bundleContext = bc;
		
		// compose local SipPeer
		// all other SipPeers have account ID as PeerID
		String localID = "";
		String displayName = "";
		// get the protocols that peer has account in
		ServiceReference[] protocolProviderRefs = null;
		try
		{
			protocolProviderRefs = bc.getServiceReferences(
				ProtocolProviderService.class.getName(),null);
		}
		catch (InvalidSyntaxException ex)
		{
			// this shouldn't happen since we're providing no parameter string
			// but let's log just in case.
			F2FDebug.println("\t\tError while retrieving service refs" + ex);
			return;
		}
		// in case we found any
		if (protocolProviderRefs != null)
		{
			// compose local peer name ...
			//NB! this name may not be surrounded with '[' and ']' because JXTA does not allow this!!!
			localID += "{F2F:";
			for (int i = 0; i < protocolProviderRefs.length; i++)
			{
				ProtocolProviderService provider = (ProtocolProviderService) bc
					.getService(protocolProviderRefs[i]);
				localID += "<"+provider.getProtocolName()+":"+ provider.getAccountID().getUserID()+">";
				if (displayName == null || displayName.isEmpty())
				{
					displayName = (String)provider.getAccountID().getAccountProperties().get(ProtocolProviderFactory.DISPLAY_NAME);
				}
			}
			localID += "}";
		}
		else localID = "{F2F:" + new Random(System.currentTimeMillis()).nextInt() + "}";
		F2FDebug.println("\t\tlocal peerID is: " + localID);
		this.localPeer = new SipPeer(localID, displayName);
		
		// check for other peers
		if (protocolProviderRefs != null)
		{
			for (int i = 0; i < protocolProviderRefs.length; i++)
			{
				ProtocolProviderService provider = (ProtocolProviderService) bc
					.getService(protocolProviderRefs[i]);
				handleProviderAdded(provider);
			}
		}
		bc.addServiceListener(this);
		if (getMetaContactListService() != null)
		{
			getMetaContactListService().addMetaContactListListener(new SipMetaContactListListener());
			addF2FPeersFromMetaContactGroup(getMetaContactListService().getRoot());
		}
	}
	
	private MetaContactListService metaCListService = null;
	private MetaContactListService getMetaContactListService()
	{
		if (metaCListService == null)
		{
			ServiceReference clistReference = bundleContext
				.getServiceReference(MetaContactListService.class.getName());
			
			metaCListService = (MetaContactListService) bundleContext
				.getService(clistReference);
		}
		
		return metaCListService;
	}

	public void addListener(CommunicationListener listener)
	{
		listeners.add(listener);
	}

	public Peer findPeerByID(String sID) throws CommunicationFailedException
	{
		return peersHash.get(sID);
	}

	Collection<CommunicationListener> getListeners()
	{
		return listeners;
	}

	public Peer getLocalPeer()
	{
		return localPeer;
	}

	public String getID()
	{
		return SIP_LAYER_ID;
	}

	private void addF2FPeerIfNeeded(final Contact contact)
	{
		// check for F2F-capability only for online contacts
		if (!contact.getPresenceStatus().isOnline()) return;
		
		// send F2F-capability test message		  
		OperationSetBasicInstantMessaging im = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
		.getOperationSet(OperationSetBasicInstantMessaging.class);
		try 
		{
			sendIMmessage(im, contact, F2F_TEST_MSG);
		}
		catch (CommunicationFailedException e)
		{
			F2FDebug.println("\t\t ERROR while sending F2F_TEST_MSG to a new contact");
			e.printStackTrace();
		}
	}

	private void removeF2FPeerIfNeeded(final Contact contact)
	{
		synchronized (peersHash)
		{
			peersHash.remove(contact.getAddress());
		}
	}

	private void addF2FPeersFromMetaContact(MetaContact metaContact)
	{
		Iterator contacts = metaContact.getContacts();
		while (contacts.hasNext())
			addF2FPeerIfNeeded((Contact)contacts.next());
	}

	private void removeF2FPeersFromMetaContact(MetaContact metaContact)
	{
		Iterator contacts = metaContact.getContacts();
		while (contacts.hasNext())
			removeF2FPeerIfNeeded((Contact)contacts.next());
	}

	private void addF2FPeersFromMetaContactGroup(final MetaContactGroup group)
	{
		Iterator metaContacts = group.getChildContacts();
		while (metaContacts.hasNext())
		{
			addF2FPeersFromMetaContact((MetaContact)metaContacts.next());
		}
		Iterator subGroups = group.getSubgroups();
		while (subGroups.hasNext())
			addF2FPeersFromMetaContactGroup((MetaContactGroup)subGroups.next());
	}

	private void removeF2FPeersFromMetaContactGroup(final MetaContactGroup group)
	{
		Iterator metaContacts = group.getChildContacts();
		while (metaContacts.hasNext())
		{
			removeF2FPeersFromMetaContact((MetaContact)metaContacts.next());
		}
		Iterator subGroups = group.getSubgroups();
		while (subGroups.hasNext())
			removeF2FPeersFromMetaContactGroup((MetaContactGroup)subGroups.next());
	}
	
	public void serviceChanged(ServiceEvent event)
	{
        Object sService = bundleContext.getService(event
                .getServiceReference());
        
        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService)) return;
        
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            // if we have the PROVIDER_MASK property set, make sure that this
            // provider has it and if not ignore it.
            String providerMask = System
                .getProperty(MetaContactListService.PROVIDER_MASK_PROPERTY);
            if (providerMask != null
                && providerMask.trim().length() > 0)
            {
                String servRefMask = (String) event
                    .getServiceReference()
                    .getProperty(
                        MetaContactListService.PROVIDER_MASK_PROPERTY);

                if (servRefMask == null
                    || !servRefMask.equals(providerMask))
                {
                    return;
                }
            }
            this.handleProviderAdded( (ProtocolProviderService) sService);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {

            ProtocolProviderService provider =
                (ProtocolProviderService)sService;
            //first check if the event really means that the accounts is
            //uninstalled (or is it just stopped ... e.g. we could be shutting
            //down) ... before that however, we'd need to get a reference to
            //the service.
            ProtocolProviderFactory sourceFactory = null;

            ServiceReference[] allBundleServices
                = event.getServiceReference().getBundle()
                    .getRegisteredServices();

            for (int i = 0; i < allBundleServices.length; i++)
            {
                Object service = bundleContext.getService(allBundleServices[i]);
                if(service instanceof ProtocolProviderFactory)
                {
                    sourceFactory = (ProtocolProviderFactory) service;
                    break;
                }
            }

            if(sourceFactory == null)
            {
                //strange ... we must be shutting down. just bail
                return;
            }

            if(sourceFactory.getRegisteredAccounts().contains(
                provider.getAccountID()))
            {
                //the account is still installed. we don't need to do anything.
                return;
            }

            this.handleProviderRemoved( (ProtocolProviderService) sService);
        }	
	}
	
    private void handleProviderAdded(
            ProtocolProviderService provider)
    {
    	//F2FDebug.println("\t\t handleProviderAdded (" + provider.getProtocolName()+" : " + provider.getAccountID().getUserID() + ")");

    	//add a presence status listener so that we could reorder contacts upon status change.
    	OperationSetPersistentPresence opSetPresence
    	= (OperationSetPersistentPresence)provider
    		.getOperationSet(OperationSetPersistentPresence.class);
    	if(opSetPresence != null) opSetPresence.addContactPresenceStatusListener(this);
    	
        // start listening for IM messages
    	OperationSetBasicInstantMessaging opSetMessaging
    	= (OperationSetBasicInstantMessaging)provider
    		.getOperationSet(OperationSetBasicInstantMessaging.class);
    	if(opSetMessaging != null) opSetMessaging.addMessageListener(this);

        //F2FDebug.println("\t\t ProtocolProvider added");
    }
    private void handleProviderRemoved(
            ProtocolProviderService provider)
    {
    	//F2FDebug.println("\t\t handleProviderRemoved (" + provider.getProtocolName()+" : " + provider.getAccountID().getUserID() + ")");

    	// remove the presence status listener
    	OperationSetPersistentPresence opSetPresence
            = (OperationSetPersistentPresence)provider
                .getOperationSet(OperationSetPersistentPresence.class);
        if(opSetPresence != null) opSetPresence.removeContactPresenceStatusListener(this);
        
        // stop listening for IM messages
    	OperationSetBasicInstantMessaging opSetMessaging
    	= (OperationSetBasicInstantMessaging)provider
    		.getOperationSet(OperationSetBasicInstantMessaging.class);
    	if(opSetMessaging != null) opSetMessaging.removeMessageListener(this);

        //F2FDebug.println("\t\t ProtocolProvider removed");
    }

	public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent event)
	{
		//F2FDebug.println("\t\t user " + event.getSourceContact().getDisplayName() + "("+event.getSourceContact().getAddress() +")"+ " status changed");
		if (event.getNewStatus().isOnline() && !event.getOldStatus().isOnline())
		{
			// a user came online
			// check if he/she is F2F-capable and add to GUI if needed
	        //F2FDebug.println("\t\t the user came online");
	        addF2FPeerIfNeeded(event.getSourceContact());
			return;
		}
		if (event.getOldStatus().isOnline() && !event.getNewStatus().isOnline())
		{
			// a user went offline
			// check if he/she was F2F-capable and remove if needed from GUI
	        //F2FDebug.println("\t\t the user went offline");
	        removeF2FPeerIfNeeded(event.getSourceContact());
			return;
		}
        //F2FDebug.println("\t\t StatusChanged - new status: " + event.getNewStatus().getStatus() + ", old status: " + event.getOldStatus().getStatus());
	}
		
	private class SipMetaContactListListener implements MetaContactListListener
	{
		public void protoContactAdded(ProtoContactEvent event) {
			//F2FDebug.println("\t\t protoContactAdded");
			addF2FPeerIfNeeded(event.getProtoContact());
		}
		
		public void metaContactAdded(MetaContactEvent event) {
			//F2FDebug.println("\t\t metaContactAdded");
			addF2FPeersFromMetaContact(event.getSourceMetaContact());
		}
		
		public void metaContactGroupAdded(MetaContactGroupEvent event) {
			//F2FDebug.println("\t\t metaContactGroupAdded");
			addF2FPeersFromMetaContactGroup(event.getSourceMetaContactGroup());
		}

		public void protoContactRemoved(ProtoContactEvent event) {
			//F2FDebug.println("\t\t protoContactRemoved");
			removeF2FPeerIfNeeded(event.getProtoContact());
		}

		public void metaContactRemoved(MetaContactEvent event) {
			//F2FDebug.println("\t\t metaContactRemoved");
			removeF2FPeersFromMetaContact(event.getSourceMetaContact());
		}

		public void metaContactGroupRemoved(MetaContactGroupEvent event) {
			//F2FDebug.println("\t\t childContactsReordered");
			removeF2FPeersFromMetaContactGroup(event.getSourceMetaContactGroup());
		}

		public void metaContactRenamed(MetaContactRenamedEvent event) {
			//F2FDebug.println("\t\t metaContactRenamed");
			//todo: rename display name in F2F if needed
		}

		public void protoContactMoved(ProtoContactEvent event) {
			//F2FDebug.println("\t\t protoContactMoved");
		}

		public void metaContactMoved(MetaContactMovedEvent event) {
			//F2FDebug.println("\t\t metaContactMoved");
		}
		
		public void childContactsReordered(MetaContactGroupEvent event) {
			//F2FDebug.println("\t\t childContactsReordered");
		}

		public void metaContactGroupModified(MetaContactGroupEvent event) {
			//F2FDebug.println("\t\t metaContactGroupModified");
		}
	}


	public void processMessage(String key, String from, String msg, Contact sourceContact) {
		System.out.println("message received'"
				+ msg
				+ "' from host peer '" + sourceContact.getDisplayName()
				+ "' from contact '" + from);
		
		if(m_chatPeers.size() > 0) { 
					
			for (Peer peer : m_chatPeers) {
				try {
					peer.sendMessage("F2F;"+key+";"+from+";"+msg);
				} 
				catch (CommunicationFailedException cfe) {					
					System.out.println("Sending message '"
							+ msg
							+ "' to the peer '" + peer.getDisplayName()
							+ "' failed with '" + cfe.getMessage() + "'");
				}				
			}
		}
		
		if(m_chatHost == null) {
			m_chatHost = sourceContact;
		}
		
		F2FComputingGUI.controller.writeMessage(from, msg);
	}
	
	
	public void onSendMessage(Collection<Peer> selectedFriends, String message) {
		String k = "123";
		
		if (m_chatPeers.size() == 0 && m_chatHost == null) {
			m_chatPeers.addAll(selectedFriends);					
			System.out.println("Generating chat id " + k);
		}
		
		String myName = getLocalPeer().getID();
		if (m_chatPeers.size() > 0) {					
			for (Peer peer : m_chatPeers) {
				System.out.println("Sending message to peer: " + peer.getDisplayName());
				try {
					peer.sendMessage("F2F;"+k+";" + myName + ";"+message);
				} 
				catch (CommunicationFailedException cfe) {					
					System.out.println("Sending message '"
							+ message
							+ "' to the peer '" + peer.getDisplayName()
							+ "' failed with '" + cfe.getMessage() + "'");
				}				
			}
			F2FComputingGUI.controller.writeMessage("me", message);

		}
		
		else if (m_chatHost != null) {
			Contact c = m_chatHost;
			OperationSetBasicInstantMessaging m_im;
		    m_im = (OperationSetBasicInstantMessaging) c.getProtocolProvider().getOperationSet(OperationSetBasicInstantMessaging.class);
			Message msg = m_im.createMessage("F2F;"+k+";" + myName + ";"+message);	
			m_im.sendInstantMessage(c, msg);
			System.out.println("Sending message to host: " + c.getDisplayName());
		}		
	}

	public void messageDelivered(MessageDeliveredEvent evt)
	{
		F2FDebug.println("\t\t MessageDeliveredEvent");
	}

	public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
	{
		F2FDebug.println("\t\t MessageDeliveryFailedEvent");
	}

	private final static byte F2F_TAG = 2;
	private final static Byte F2F_TEST_MSG = F2F_TAG;
	public void messageReceived(MessageReceivedEvent evt)
	{
		F2FDebug.println("\t\t MessageReceivedEvent");
		byte[] data = Util.decode(evt.getSourceMessage().getContent());
		// process only messages that start with F2F tag
		if (data.length > 1 && data[0] == F2F_TAG)
		{
			F2FDebug.println("\t\t received a F2F message");
			byte[] raw_obj = new byte[data.length-1];
			for (int i = 1; i < data.length; i++) raw_obj[i-1] = data[i]; 
			try
			{
				Object message = Util.deserializeObject(raw_obj);
				boolean bIsF2Ftest = F2F_TEST_MSG.equals(message);
				if (peersHash.containsKey(evt.getSourceContact().getAddress()))
				{
					// dump F2F capability test messages from known peers
					if (bIsF2Ftest)
						F2FDebug.println("\t\t received F2F_TEST message from peer " + evt.getSourceContact().getAddress());
					else
					{
						F2FDebug.println("\t\t received a F2F message from peer " + evt.getSourceContact().getAddress());
						for(CommunicationListener listener: getListeners())
						{
							listener.messageRecieved(message, peersHash.get(evt.getSourceContact().getAddress()));
						}
					}
				}
				else
				{
					// process F2F capability test
					if (bIsF2Ftest)
					{
						synchronized (peersHash)
						{
							// add new peer
							SipPeer peer = new SipPeer(evt.getSourceContact()); 
							peersHash.put(evt.getSourceContact().getAddress(), peer);
							// send F2F capability test message back
							peer.sendMessage(message);
						}
					}
					else F2FDebug.println("\t\t received a F2F message from unknown peer " + evt.getSourceContact().getAddress());
				}
			}
			catch (Exception e)
			{
				F2FDebug.println("\t\t ERROR while receiving a message!");
				e.printStackTrace();
			}
		}
	}
	
	static void sendIMmessage(OperationSetBasicInstantMessaging im, Contact contact, Object msg) throws CommunicationFailedException
	{
		try
		{
			// serialize message and add F2F tag to it
			byte[] raw_msg = Util.serializeObject(msg);
			byte[] data = new byte[raw_msg.length + 1];
			data[0] = F2F_TAG;
			for (int i = 0; i < raw_msg.length; i++) data [i+1] = raw_msg[i];
			im.sendInstantMessage(contact, im.createMessage(Util.encode(data)));
		}
		catch (Exception e)
		{
			F2FDebug.println("\t\t ERROR while sending a message to contact " + contact.getDisplayName());
			throw new CommunicationFailedException(e);
		}
	}
	
	public static void jstunTest()
	{
		final String SIP_STUN_SERVER_NAME = "iphone-stun.freenet.de";
		final int SIP_STUN_SERVER_PORT = 3478;
		// start for NAT/firewall traversal
		Enumeration<NetworkInterface> ifaces;
		try 
		{
			ifaces = NetworkInterface.getNetworkInterfaces();
		}
		catch (SocketException e)
		{
			e.printStackTrace();
			return;
		}
		while (ifaces.hasMoreElements())
		{
			NetworkInterface iface = ifaces.nextElement();
			Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
			while (iaddresses.hasMoreElements())
			{
				InetAddress iaddress = iaddresses.nextElement();
				if (!iaddress.isLoopbackAddress() && !iaddress.isLinkLocalAddress()) {
					DiscoveryTest test = new DiscoveryTest(iaddress, SIP_STUN_SERVER_NAME, SIP_STUN_SERVER_PORT);
					DiscoveryInfo di = null;
					try {
						di = test.test();
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
					if (di != null) F2FDebug.println(di.toString()); 
				}
			}
		}
	}
}