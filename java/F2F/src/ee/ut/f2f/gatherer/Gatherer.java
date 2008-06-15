package ee.ut.f2f.gatherer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactGroup;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ee.ut.f2f.comm.sc.im.SipIMCommunicationProvider;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.PeerPresenceListener;
import ee.ut.f2f.gatherer.rmi.F2FGathererServer;
import ee.ut.f2f.util.logging.Logger;

/**
 * Main class from where F2F is initiated
 * @author Raido Türk
 *
 */
public class Gatherer implements PeerPresenceListener {
	
	private static final Logger logger = Logger.getLogger(Gatherer.class);

	private static Gatherer instance = null;
	private BundleContext bundleContext = null;

	public static Gatherer getInstance() {
		if (instance == null)
			instance = new Gatherer();
		return instance;
	}
	
	public void initialize(BundleContext bc) {
		while(F2FComputing.getLocalPeer() == null); //wait until F2FComputing framework is initialized
		
		this.bundleContext = bc;
		F2FComputing.addPeerPresenceListener(this);
		
		if (getMetaContactListService() != null)
		{
			MetaContactGroup group = getMetaContactListService().getRoot();
			for(Iterator it = group.getChildContacts(); it.hasNext();){
				MetaContact metaC = (MetaContact) it.next();
			}
			findContacts(group.getContactGroups());
			for(Iterator it = contacts.iterator(); it.hasNext(); ) {
				Contact contact = (Contact) it.next();
				//SipIMCommunicationProvider.getInstance().makeF2FTest(contact); //FIXME: hack to determine F2F capable contacts
			}
		}

		returnAllPeerAccounts();
		DataGathering.getInstance();
		
		try {
			F2FGathererServer.main(null);
		} catch (Exception e) {logger.error("error when starting RMI server", e);}
		
		
	}
	
	private Collection<Contact> contacts = new ArrayList<Contact>();
	private void findContacts(Iterator<ContactGroup> iter) {
		for(Iterator<ContactGroup> it = iter; it.hasNext(); ) {
			ContactGroup group = (ContactGroup) it.next();
			findContacts(group.subgroups());
			for(Iterator<Contact> it2 = group.contacts(); it2.hasNext(); ) {
				Contact contact = (Contact) it2.next();
				contacts.add(contact);
			}
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
	
	/**
	 * @return the bundleContext
	 */
	public BundleContext getBundleContext() {
		return bundleContext;
	}
	
	/**
	 * Returns all peer accounts
	 * @return Map of accounts
	 */
	public Map<String,List<String>> returnAllPeerAccounts() {
		Map<String,List<String>> peerAccounts = new HashMap<String,List<String>>();
		ServiceReference[] protocolProviderRefs = null; // all accounts that peer has account in
		try {
			protocolProviderRefs = getBundleContext().getServiceReferences(ProtocolProviderService.class.getName(), null);
			// listen for contacts presence status changes
			if (protocolProviderRefs != null) {
				for (int i = 0; i < protocolProviderRefs.length; i++) {
					ProtocolProviderService provider = (ProtocolProviderService) getBundleContext()
							.getService(protocolProviderRefs[i]);
					List<String> providerAccounts = (List<String>)peerAccounts.get(provider.getProtocolDisplayName());
					if(providerAccounts == null)
						providerAccounts = new ArrayList<String>();
					providerAccounts.add(provider.getAccountID().getAccountAddress());
					peerAccounts.put(provider.getProtocolDisplayName(), providerAccounts);

				}
			}
			return peerAccounts;
		} catch (InvalidSyntaxException ex) {
			logger.error("Peer accounts retrieval failed", ex);
		}
		return null;
	}
	
	/**
	 * Finds connections with given peer accounts
	 * @param requesterAccounts peer's accounts to be compared with
	 * @return list of Protocols that current peer in his/her contactlist 
	 */
	public List<String> findConnectionsWithPeer(Map<String,List<String>> requesterAccounts) {
		List<String> connections = new ArrayList<String>();
		if (getMetaContactListService() != null)
		{
			for (Map.Entry<String, List<String>> entry : requesterAccounts.entrySet()) {
				   String provider = (String)entry.getKey();
				   List<String> providerAccounts = (List<String>)entry.getValue();
				for(Contact contact :  contacts) {
					if (provider.equals(contact.getProtocolProvider().getProtocolDisplayName())
							&& providerAccounts.contains(contact.getAddress())) {
						connections.add(provider);
						break;
					}
				}
			}
		}
		return connections;
	}


	public void peerContacted(F2FPeer peer) {
		
	}

	public void peerUnContacted(F2FPeer peer) {

	}

}
