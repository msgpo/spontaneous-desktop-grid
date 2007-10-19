/**
 * 
 */
package ee.ut.f2f.comm.sip;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;

import ee.ut.f2f.comm.CommunicationException;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.CommunicationLayer;
import ee.ut.f2f.comm.CommunicationListener;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
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
import org.p2psockets.P2PServerSocket;
import org.p2psockets.P2PNetwork;

public class SipCommunicationLayer 
	implements 	CommunicationLayer,
				ServiceListener,
				ContactPresenceStatusListener
{
	/**
	 * Name that identifies Sip communication layer.
	 */
	private static final String SIP_LAYER_ID = "F2FSipCommLayer";
	/**
	 * Name that identifies Sip communication network in P2PSockets library.
	 */
	private static final String SIP_LAYER_NETWORK_ID = "F2FSipP2PNetwork";
	/**
	 * Port name that P2PSockets library uses (is not actual port nr).
	 */
	static final int SIP_LAYER_NETWORK_PORT = 49201;
	static final String SIP_LAYER_NETWORK_PASSWORD = "1pass2word2";
	
	/**
	 * PeerID -> Peer
	 */
	private Hashtable<String, Peer> peersHash = null;
	public Collection<Peer> getPeers() { return peersHash.values(); }
	/**
	 * PeerAddress -> PeerID
	 */
	private Hashtable<String, String> addressHash = null;
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
		peersHash = new Hashtable<String, Peer>();
		addressHash = new Hashtable<String, String>();
		// init Sip
		F2FDebug.println("\t\tInitializing SIP communication layer ...");
		this.bundleContext = bc;
		// init JXTA P2PSocket library
		//F2FDebug.println("\t\tSIGN INTO NETWORK '" + SIP_LAYER_NETWORK_ID + "' ...");
		try
		{
			P2PNetwork.autoSignin(SIP_LAYER_NETWORK_ID);
		}
		catch (Exception e)
		{
			throw new CommunicationInitException("Could not initialize JXTA's P2PSockets!", e);
		}
		
		// compose local peer name and publish it
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

		this.localPeer = new SipPeer(this, localID, displayName);
		new Thread(new SipListener(localID)).start();

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
		
		Thread thread = 
			new Thread(new Runnable()
			{
				public void run()
				{
					//F2FDebug.println("\t\t checking if " + contact.getDisplayName() + " is F2F-capable ...");
					String sID = addressHash.get(contact.getAddress());
					if (sID == null)
					{
						// ask from remote peer its SIP comm layer ID
						SipIDRequester req = new SipIDRequester(contact.getAddress());
						sID = req.getSipID();
						if (sID == null)
						{
							//F2FDebug.println("\t\t " + contact.getDisplayName() + " is not F2F-capable");
							return;
						}
						synchronized (addressHash)
						{
							if (addressHash.get(contact.getAddress()) == null)
								addressHash.put(contact.getAddress(), sID);
						}
					}
					synchronized (peersHash)
					{
						Peer peer = peersHash.get(sID);
						if (peer == null)
						{
							peer = new SipPeer(SipCommunicationLayer.this, sID, contact.getDisplayName());
							peersHash.put(sID, peer);
						}
					}
				}
			});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private void removeF2FPeerIfNeeded(final Contact contact)
	{
		String sID = addressHash.get(contact.getAddress());
		if (sID != null)
		{
			synchronized (addressHash)
			{
			synchronized (peersHash)
			{
				addressHash.remove(contact.getAddress());
				peersHash.remove(sID);
			}
			}
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
    	
    	OperationSetPersistentPresence opSetPresence
        	= (OperationSetPersistentPresence)provider
        		.getOperationSet(OperationSetPersistentPresence.class);

        //add a presence status listener so that we could reorder contacts upon
        //status change.
        if(opSetPresence == null) return;

        // ... and start publishing it
        try 
        {
			new Thread(new SipIDPublisher(provider.getAccountID().getUserID(), localPeer.getID(), localPeer.getDisplayName())).start();
		}
        catch (CommunicationException e)
        {
			F2FDebug.println(""+e);
			return;
		}
        
        //F2FDebug.println("\t\t Presence ProtocolProvider added");
        opSetPresence.addContactPresenceStatusListener(this);
    }
    private void handleProviderRemoved(
            ProtocolProviderService provider)
    {
    	F2FDebug.println("\t\t handleProviderRemoved (" + provider.getProtocolName()+" : " + provider.getAccountID().getUserID() + ")");
    	OperationSetPersistentPresence opSetPresence
            = (OperationSetPersistentPresence)provider
                .getOperationSet(OperationSetPersistentPresence.class);

        //ignore if persistent presence is not supported.
        if(opSetPresence == null) return;

        F2FDebug.println("\t\t Presence ProtocolProvider removed");
        opSetPresence.removeContactPresenceStatusListener(this);
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
	
	private class SipListener implements Runnable
	{
		/*
		 * This socket listenes for incoming connections from other peers.
		 */
		private ServerSocket serverSocket;
		
		SipListener(String localUID) throws CommunicationException
		{
			try
			{
				// create a server socket
				//F2FDebug.println("\t\tCREATE SERVER SOCKET ["+ localUID + "]...");
				serverSocket = new P2PServerSocket(localUID, SIP_LAYER_NETWORK_PORT);
				//F2FDebug.println("\t\tSIP LISTENER INITIALIZED.");
			}
			catch (IOException e)
			{
				throw new CommunicationInitException("Could not create JXTA's P2PServerSocket for "+localUID+"!", e);
			}
		}
		
		public void run()
		{
			while(true)
			{
				try
				{
					// wait while someone tries to connect
					Socket socket = serverSocket.accept();
					//F2FDebug.println("\t\tACCEPTED A CONNECTION. GET INPUTSTREAM ...");
					InputStream is = socket.getInputStream();
					//F2FDebug.println("\t\tGOT INPUTSTREAM. CREATE CustomObjectInputStream ...");
					SipObjectInput oi = new SipObjectInput(is);
					
					// handshake
					//F2FDebug.println("\t\tREAD PASSWORD ...");
					String password = (String)oi.readObject();
					//F2FDebug.println("\t\tREAD PASSWORD '" + password + "'.");
					boolean bHandshake = password.equals(SIP_LAYER_NETWORK_PASSWORD);
					if (bHandshake)
					{
						// read the ID of remote peer
						String remoteID = (String)oi.readObject();
						String remoteDisplayName = (String)oi.readObject();
						// see if peer is already known
						SipPeer peer = (SipPeer)findPeerByID(remoteID);
						if (peer == null)
						{
							// create peer
							peer = new SipPeer(SipCommunicationLayer.this, remoteID, remoteDisplayName);
							peersHash.put(remoteID, peer);
						}
						// now peer should be not null, but still lets check it
						if (peer != null)
						{
							// start listening for messages from the peer
							peer.setOi(oi);
							F2FDebug.println("\t\tAccepted remote connection from '"+remoteID+"'");
						}
					}
					else
					{
						socket.close();
						F2FDebug.println("\t\tUnauthorized connection attempt. Read password was '" + password + 
								"'. Closed the connection.");
					}
				}
				catch (Exception e)
				{
					F2FDebug.println("\t\tProblems creating the p2psocket communication: " + e);
					e.printStackTrace();
				}
			}
		}
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
}