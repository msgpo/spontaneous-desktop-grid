/**
 * 
 */
package ee.ut.f2f.comm.sc.im;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.UUID;

import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import ee.ut.f2f.comm.CommunicationException;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.Util;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.ProtocolNames;
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
import net.java.sip.communicator.service.gui.UIService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class SipIMCommunicationProvider 
	implements 	CommunicationProvider,
				ServiceListener,
				ContactPresenceStatusListener, MessageListener
{	
	/**
	 * Name that identifies Sip communication layer.
	 */
	private static final String SIP_LAYER_ID = "F2FSipCommLayer";
	
	/**
	 * SipPeer ID (account ID) -> <UUID, SipPeer>
	 */
	private Hashtable<String, UUIDSipPeer> sipPeers = null;
	/**
	 * UUID -> list of according SipPeer IDs
	 */
	private Hashtable<UUID, Collection<String>> idMap = null;
	
	private static SipIMCommunicationProvider siplayer = null;
	public static SipIMCommunicationProvider getInstance() { return siplayer; }
	
	public static SipIMCommunicationProvider initiateSipIMCommunicationProvider(BundleContext bc) throws CommunicationException
	{
		if (siplayer!=null) 
			throw new CommunicationInitException("SIP layer already initiated, initiateSipCommunicationProvider() was called more than once!");		
		
		// Create the F2F layer
		return (siplayer = new SipIMCommunicationProvider(bc));
	}
	
	private BundleContext bundleContext = null;
	private SipIMCommunicationProvider(BundleContext bc) throws CommunicationException
	{
		sipPeers = new Hashtable<String, UUIDSipPeer>();
		idMap = new Hashtable<UUID, Collection<String>>();
		messageCache = new Hashtable<Contact, byte[]>();
		protocols = new ArrayList<ProtocolProviderService>();
		allowedContacts = new ArrayList<Contact>();
		// init Sip
		//F2FDebug.println("\t\tInitializing SIP communication layer ...");
		this.bundleContext = bc;
		// add our button to SipCommunicator meta contact context menu 
		ServiceReference uiServiceRef = bc.getServiceReference(UIService.class.getName());
		UIService uiService = (UIService) bc.getService(uiServiceRef);       
	    if(uiService.isContainerSupported(UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
	    {   
	    	uiService.addComponent(
	            UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU,
	            new SipIMContactF2FMenuItem());
	    }
	
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
		
		// listen for contacts presence status changes
		if (protocolProviderRefs != null)
		{
			for (int i = 0; i < protocolProviderRefs.length; i++)
			{
				ProtocolProviderService provider = (ProtocolProviderService) bc
					.getService(protocolProviderRefs[i]);
				handleProviderAdded(provider);
			}
		}
		// listen for changes in used protocols
		bc.addServiceListener(this);
		// listen for changes in SC contact list
		if (getMetaContactListService() != null)
		{
			getMetaContactListService().addMetaContactListListener(new SipMetaContactListListener());
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

	public String getID()
	{
		return SIP_LAYER_ID;
	}

	private Collection<Contact> allowedContacts = null;
	void makeF2FTest(MetaContact metaContact)
	{
		Iterator contacts = metaContact.getContacts();
		while (contacts.hasNext())
		{
			Contact contact = (Contact)contacts.next();
			if (sipPeers.containsKey(contact.getAddress())) continue;
			allowedContacts.add(contact);
			addF2FPeerIfNeeded(contact);
		}
	}
	private void addF2FPeerIfNeeded(final Contact contact)
	{
		// check for F2F-capability only for online contacts
		if (!contact.getPresenceStatus().isOnline()) return;
		
		// check for F2F-capability only for those contacts that the user has allowed
		if (!allowedContacts.contains(contact)) return;
		
		// send F2F-capability test message		  
		OperationSetBasicInstantMessaging im = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
		.getOperationSet(OperationSetBasicInstantMessaging.class);
		try 
		{
			sendIMmessage(contact, new F2FTestMessage(F2FComputing.getLocalPeer().getID()));
		}
		catch (CommunicationFailedException e)
		{
			F2FDebug.println("\t\t ERROR while sending F2F_TEST_MSG to a new contact");
			e.printStackTrace();
		}
	}

	private void removeF2FPeerIfNeeded(final Contact contact)
	{
		UUIDSipPeer up = sipPeers.get(contact.getAddress());
		if (up == null) return;
		UUID peerID = up.id;
		synchronized (idMap)
		{
			idMap.get(peerID).remove(contact.getAddress());
			if (idMap.get(peerID).size() == 0)
			{
				idMap.remove(peerID);
				F2FComputing.peerUnContacted(peerID, this);
			}
		}
		synchronized (sipPeers)
		{
			sipPeers.remove(contact.getAddress());
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
	
	private Collection<ProtocolProviderService> protocols;
    private void handleProviderAdded(
            ProtocolProviderService provider)
    {
    	//F2FDebug.println("\t\t handleProviderAdded (" + provider.getProtocolName()+" : " + provider.getAccountID().getUserID() + ")");
    	protocols.add(provider);
    	
    	//add a presence status listener so that we could contact peers if they come online
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
    	protocols.remove(provider);
    	
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
	
	public void messageDelivered(MessageDeliveredEvent evt)
	{
		//F2FDebug.println("\t\t MessageDeliveredEvent");
	}

	public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
	{
		//F2FDebug.println("\t\t MessageDeliveryFailedEvent");
	}
	
	private final static String F2F_TAG_START = "<f2f>\n";
	private final static String F2F_TAG_END = "\n</f2f>";
	private final static byte F2F_MORE = 2;
	private final static byte F2F_COMPLETE = 3;
	private Hashtable<Contact, byte[]> messageCache = null;
	private static boolean isF2FMessage(String str)
	{
		if (str.length() > F2F_TAG_START.length() + F2F_TAG_END.length() &&
				str.substring(0, F2F_TAG_START.length()).equals(F2F_TAG_START) &&
				str.substring(str.length()-F2F_TAG_END.length()).equals(F2F_TAG_END))
		{
			return true;
		}
		return false;
	}
	public void messageReceived(MessageReceivedEvent evt)
	{
		// process only messages that start with F2F tag
		String str = evt.getSourceMessage().getContent();
		if (isF2FMessage(str))
		{
			byte[] data = Util.decode(str.substring(F2F_TAG_START.length(), str.length() - F2F_TAG_END.length()));
			processF2FMessage(data, evt);
		}
	}
	public static boolean isF2FMessage(EventObject evt)
	{
		String msg = null;
		if (evt instanceof MessageDeliveredEvent)
        {
            msg = ((MessageDeliveredEvent)evt).getSourceMessage().getContent();
        }
        else if (evt instanceof MessageReceivedEvent)
        {
            msg = ((MessageReceivedEvent)evt).getSourceMessage().getContent();
        }
		if (msg == null) return false;
		
		if (!isF2FMessage(msg)) return false;

		// handle MessageReceivedEvent
		if (evt instanceof MessageReceivedEvent)
		{
			byte[] data = Util.decode(msg.substring(F2F_TAG_START.length(), msg.length() -  F2F_TAG_END.length()));
			getInstance().processF2FMessage(data, (MessageReceivedEvent)evt);
		}
		
		return true;
	}
	private void processF2FMessage(byte[] data, MessageReceivedEvent evt)
	{
		//F2FDebug.println("\t\t received a F2F message");
		byte[] raw_obj = new byte[data.length-1];
		for (int i = 1; i < data.length; i++) raw_obj[i-1] = data[i];
		// cache the data and wait for more if needed
		if (data[0] == F2F_MORE)
		{
			//F2FDebug.println("\t\t cache the message");
			byte[] cache = raw_obj;
			if (messageCache.containsKey(evt.getSourceContact()))
			{
				byte[] oldCache = messageCache.get(evt.getSourceContact());
				//F2FDebug.println("\t\t\t old cache " + oldCache.length);
				cache = new byte[raw_obj.length + oldCache.length];
				for (int i = 0; i < oldCache.length; i++) cache[i] = oldCache[i];
				for (int i = 0; i < raw_obj.length; i++) cache[i + oldCache.length] = raw_obj[i];
			}
			//F2FDebug.println("\t\t\t add " + raw_obj.length);
			messageCache.put(evt.getSourceContact(), cache);
		}
		else if (data[0] == F2F_COMPLETE)
		{
			//F2FDebug.println("\t\t message has F2F_COMPLETE flag");
			// read cache if needed
			if (messageCache.containsKey(evt.getSourceContact()))
			{
				//F2FDebug.println("\t\t append cached data to the message");
				byte[] last = raw_obj;
				byte[] cache = messageCache.get(evt.getSourceContact());
				raw_obj = new byte[cache.length + last.length];
				for (int i = 0; i < cache.length; i++) raw_obj[i] = cache[i];
				for (int i = 0; i < last.length; i++) raw_obj[i + cache.length] = last[i];
				messageCache.remove(evt.getSourceContact());
			}
			try
			{
				//F2FDebug.println("\t\t unzip byte[] with length " + raw_obj.length);
				raw_obj = Util.unzip(raw_obj);
				//F2FDebug.println("\t\t deserialize byte[] with length " + raw_obj.length);
				Object message = Util.deserializeObject(raw_obj);
				boolean bIsF2Ftest = (message instanceof F2FTestMessage);
				if (sipPeers.containsKey(evt.getSourceContact().getAddress()))
				{
					// dump F2F capability test messages from known peers
					if (bIsF2Ftest)
					{
						//F2FDebug.println("\t\t received F2F_TEST message from peer " + evt.getSourceContact().getAddress());
					}
					else
					{
						//F2FDebug.println("\t\t received a F2F message from peer " + evt.getSourceContact().getAddress());
						F2FComputing.messageRecieved(message, sipPeers.get(evt.getSourceContact().getAddress()).id);
					}
				}
				else
				{
					// process F2F capability test
					if (bIsF2Ftest)
					{
						synchronized (sipPeers)
						{
							// do an additional test
							if (sipPeers.containsKey(evt.getSourceContact().getAddress()))
								return;
							// add new peer
							SipIMPeer peer = new SipIMPeer(evt.getSourceContact());
							F2FTestMessage tmsg = (F2FTestMessage) message;
							sipPeers.put(evt.getSourceContact().getAddress(), new UUIDSipPeer(tmsg.id, peer));
							synchronized (idMap)
							{
								if (!idMap.containsKey(tmsg.id))
								{
									idMap.put(tmsg.id, new ArrayList<String>());
									F2FComputing.peerContacted(tmsg.id, evt.getSourceContact().getDisplayName(), this);
								}
								idMap.get(tmsg.id).add(evt.getSourceContact().getAddress());
							}
							// send F2F capability test message back
							allowedContacts.add(evt.getSourceContact());
							addF2FPeerIfNeeded(evt.getSourceContact());
						}
					}
					else F2FDebug.println("\t\t received a F2F message from unknown peer " + evt.getSourceContact().getAddress());
				}
			}
			catch (Exception e)
			{
				F2FDebug.println("\t\t ERROR while receiving a message from " + evt.getSourceContact().getAddress());
				e.printStackTrace();
			}
		}
		else F2FDebug.println("\t\t ERROR received broken F2F message from peer " + evt.getSourceContact().getAddress());
	}
	
	private static final int MAX_MSG_LENGTH_MSN = 1050 - F2F_TAG_START.length() - F2F_TAG_END.length(); // max size of MSN message is 1050 bytes
	private static final int SLEEP_TIME_MSN = 75; // How long to wait between sending messages
	private static final int MAX_MSG_LENGTH_JABBER = Integer.MAX_VALUE;
	private static final int SLEEP_TIME_JABBER = 0;
	// seems that icq buffers messages
	// we may send message almost 2M big but small messages travel much faster
	// if messages are sent very frequently ICQ server buffers them and gives out 1 message per ~2s
	// so lets keep messages small and do not send them out quickly
	private static final int MAX_MSG_LENGTH_ICQ = 1024;
	private static final int SLEEP_TIME_ICQ = 100;
	private static final int MAX_MSG_LENGTH = 256 - F2F_TAG_START.length() - F2F_TAG_END.length(); // max size of MSN message is 1050 bytes
	private static final int SLEEP_TIME = 200; // How long to wait between sending messages
	static synchronized void sendIMmessage(Contact contact, Object msg) throws CommunicationFailedException
	{
		ProtocolProviderService protProv = contact.getProtocolProvider();
		OperationSetBasicInstantMessaging im = (OperationSetBasicInstantMessaging) protProv
			.getOperationSet(OperationSetBasicInstantMessaging.class);
		
		try
		{
			// serialize message
			byte[] raw_msg = Util.serializeObject(msg);
			//F2FDebug.println("\t\t serialized object to byte[] with length " + raw_msg.length);
			// compress message
			raw_msg = Util.zip(raw_msg);
			//F2FDebug.println("\t\t zip data to byte[] with length " + raw_msg.length);
			// set the maximum size of a message and the speed of pushing the message 
			// parts to the IM channel according to the used IM protocol
			int maxMsgLen = MAX_MSG_LENGTH;
			int sleepTime = SLEEP_TIME;
			
			if (protProv.getProtocolName().equals(ProtocolNames.MSN))
			{
				maxMsgLen = MAX_MSG_LENGTH_MSN;
				sleepTime = SLEEP_TIME_MSN;
			}
			else if (protProv.getProtocolName().equals(ProtocolNames.JABBER))
			{
				maxMsgLen = MAX_MSG_LENGTH_JABBER;
				sleepTime = SLEEP_TIME_JABBER;
			}
			else if (protProv.getProtocolName().equals(ProtocolNames.ICQ))
			{
				maxMsgLen = MAX_MSG_LENGTH_ICQ;
				sleepTime = SLEEP_TIME_ICQ;
			}
			
			// split message in parts if needed and surround each part with F2F-tags
			int sentData = 0;
			boolean bMore = false;
			do
			{
				bMore = raw_msg.length > sentData + maxMsgLen;
				byte[] data = new byte[bMore ? maxMsgLen + 1 : raw_msg.length - sentData + 1];
				data[0] = bMore ? F2F_MORE : F2F_COMPLETE;
				for (int j = 0; j < data.length - 1; j++) data [j+1] = raw_msg[j + sentData];
				Message message = im.createMessage(F2F_TAG_START+Util.encode(data)+F2F_TAG_END);
				im.sendInstantMessage(contact, message);
				sentData = sentData + data.length - 1;
				//F2FDebug.println("\t\t\t sent " + sentData);
				// give IM channel some time to send the data
				// MSN connection is closed if too much data is pushed too fast
				Thread.sleep(sleepTime);
			}
			while (bMore);
		}
		catch (Exception e)
		{
			e.printStackTrace();
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

	public boolean isLocalPeerID(String ID)
	{
		for (ProtocolProviderService protocol: protocols)
		{
			if (protocol.getAccountID().getUserID().equals(ID)) return true;
		}
		return false;
	}

	public String[] getLocalPeerIDs()
	{
		String[] ret = new String[protocols.size()];
		int i = 0;
		for (ProtocolProviderService protocol: protocols)
			ret[i++] = protocol.getAccountID().getUserID();
		return ret;
	}

	public void sendMessage(UUID destinationPeer, Object message) throws CommunicationFailedException
	{
		//TODO: prefere some IM channels to other if more than one can be used
		//	* Jabber is working very well 
		//	* MSN is not very good
		//	* ICQ is not very good
		for (String sipID: idMap.get(destinationPeer))
		{
			SipIMPeer peer = sipPeers.get(sipID).peer;
			if (peer == null) continue;
			try
			{
				peer.sendMessage(message);
				return;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				continue;
			}
		}
		throw new CommunicationFailedException();
	}
}

class UUIDSipPeer
{
	UUID id = null;
	SipIMPeer peer = null;
	UUIDSipPeer(UUID id, SipIMPeer peer)
	{
		this.id = id;
		this.peer = peer;
	}
}

class F2FTestMessage implements Serializable
{
	private static final long serialVersionUID = 731161872582048128L;
	UUID id;
	F2FTestMessage(UUID id)
	{
		this.id = id;
	}
}