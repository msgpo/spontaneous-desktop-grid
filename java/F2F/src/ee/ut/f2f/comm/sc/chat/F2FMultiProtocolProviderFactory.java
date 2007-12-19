package ee.ut.f2f.comm.sc.chat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import ee.ut.f2f.comm.sc.im.SipIMCommunicationProvider;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class F2FMultiProtocolProviderFactory
	extends ProtocolProviderFactory
{
	/**
     * The table that we store our accounts in.
     */
    private Hashtable<AccountID, ServiceRegistration> registeredAccounts = new Hashtable<AccountID, ServiceRegistration>();
    private F2FMultiProtocolProviderService f2fProtocolProvider = null;
    
	public F2FMultiProtocolProviderFactory(SipIMCommunicationProvider sipCommProvider)
	{
		super();
		
		// initialize F2F multi protocol
		f2fProtocolProvider
			= new F2FMultiProtocolProviderService(sipCommProvider);
		
		AccountID accountID = f2fProtocolProvider.getAccountID();
        
		// this properties map is used for filtering out unneeded data in SIP Communicator 
		Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(PROTOCOL, f2fProtocolProvider.getProtocolName());
        properties.put(USER_ID, accountID.getUserID());
        
        ServiceRegistration registration
            = sipCommProvider.getBundleContext().registerService( ProtocolProviderService.class.getName(),
            		f2fProtocolProvider, properties);

        registeredAccounts.put(accountID, registration);
	}

	/**
     * Returns a copy of the list containing all accounts currently
     * registered in this protocol provider.
     *
     * @return a copy of the list containing all accounts currently installed
     * in the protocol provider.
     */
    public ArrayList<AccountID> getRegisteredAccounts()
    {
        return new ArrayList<AccountID>(registeredAccounts.keySet());
    }
	
    /**
     * Returns the ServiceReference for the protocol provider corresponding to
     * the specified accountID or null if the accountID is unknown.
     * @param accountID the accountID of the protocol provider we'd like to get
     * @return a ServiceReference object to the protocol provider with the
     * specified account id and null if the account id is unknwon to the
     * provider factory.
     */
    public ServiceReference getProviderForAccount(AccountID accountID)
    {
        ServiceRegistration registration
            = (ServiceRegistration)registeredAccounts.get(accountID);

        return (registration == null )
                    ? null
                    : registration.getReference();
    }

	///////////////
	// we do not support installation and loading of new F2FMulti accounts 
	///////////////
    public AccountID installAccount(String userID, Map accountProperties) throws IllegalArgumentException, IllegalStateException, NullPointerException {
		return null;
	}
	protected AccountID loadAccount(Map accountProperties) {
		return null;
	}
	public boolean uninstallAccount(AccountID accountID) {
		return false;
	}
}
