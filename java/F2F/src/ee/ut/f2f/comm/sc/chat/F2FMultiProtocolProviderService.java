package ee.ut.f2f.comm.sc.chat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ee.ut.f2f.comm.sc.im.SipIMCommunicationProvider;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolIcon;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;

public class F2FMultiProtocolProviderService
	implements ProtocolProviderService
{
    /**
     * The operation sets that our F2FMulti provider supports.
     */
    private Hashtable<String, OperationSet> supportedOperationSets  = new Hashtable<String, OperationSet>();

    /**
     * The identifier of the account.
     */
    private AccountID accountID = null;
    
    /**
     * The icon corresponding to the F2F multi protocol.
     */
    private F2FMultiProtocolIcon f2fIcon = new F2FMultiProtocolIcon();

    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private List<RegistrationStateChangeListener> registrationListeners = new ArrayList<RegistrationStateChangeListener>();
    
    private SipIMCommunicationProvider sipCommProvider = null;
    SipIMCommunicationProvider getSipCommProvider() { return sipCommProvider; }
    
    private F2FMultiOperationSetMultiUserChat multiUserChat = null;
    
    /**
     * Creates an instance of this mockprovider with a <tt>supportedOperationSet-s</tt>
     * map set to contain a single persistent presence operation set.
     * @param factory 
     *
     * @param userName an almost ignorable string (any value is accepted) that
     * should be used when constructing account id's
     */
    public F2FMultiProtocolProviderService(SipIMCommunicationProvider sipCommProvider)
    {
    	//accountID = new F2FMultiAccountID(F2FComputing.getLocalPeer().getID().toString());
    	accountID = new F2FMultiAccountID("");
    	this.sipCommProvider = sipCommProvider;
    	multiUserChat = new F2FMultiOperationSetMultiUserChat(this); 
        this.supportedOperationSets.put(
                OperationSetMultiUserChat.class.getName(),
                multiUserChat);
    }

    /**
     * Removes the specified registration state change listener so that it does
     * not receive any further notifications upon changes of the
     * RegistrationState of this provider.
     *
     * @param listener the listener to register for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            registrationListeners.remove(listener);
        }
    }

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     *
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            if (!registrationListeners.contains(listener))
                registrationListeners.add(listener);
        }
    }

    /**
     * F2FMulti implementation of the corresponding ProtocolProviderService method.
     *
     * @return a String describing this mock protocol.
     */
    public String getProtocolName()
    {
        return ProtocolNames.F2F;
    }

    /**
     * Returns an array containing all operation sets supported by the
     * current implementation.
     *
     * @return a java.util.Map containing instance of all supported
     *   operation sets mapped against their class names (e.g.
     *   OperationSetPresence.class.getName()) .
     */
    public Map<String, OperationSet> getSupportedOperationSets()
    {
        return this.supportedOperationSets;
    }

    /**
     * Returns the operation set corresponding to the specified class or null
     * if this operation set is not supported by the provider implementation.
     *
     * @param opsetClass the <tt>Class</tt>  of the operation set that we're
     * looking for.
     * @return returns an OperationSet of the specified <tt>Class</tt> if the
     * undelying implementation supports it or null otherwise.
     */
    public OperationSet getOperationSet(Class opsetClass)
    {
        return getSupportedOperationSets()
            .get(opsetClass.getName());
    }

    private RegistrationState regState = RegistrationState.UNREGISTERED;
    /**
     * F2FMulti implementation of the corresponding ProtocolProviderService method.
     *
     * @return always true.
     */
    public boolean isRegistered()
    {
        return regState == RegistrationState.REGISTERED;
    }

    /**
     * F2FMulti implementation of the corresponding ProtocolProviderService method.
     *
     * @return a Registered RegistrationState.
     */
    public RegistrationState getRegistrationState()
    {
        return regState;
    }

    /**
     * F2FMulti implementation of the corresponding ProtocolProviderService method.
     *
     * @param authority a dummy param
     */
    public void register(SecurityAuthority authority)
    {
    	//TODO: enable F2F
    	RegistrationState oldState = getRegistrationState();
    	regState = RegistrationState.REGISTERED;
        fireRegistrationStateChanged(
            oldState,
            RegistrationState.REGISTERED,
            RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
    }

    /**
     * F2FMulti implementation of the corresponding ProtocolProviderService method.
     */
    public void unregister()
    {
    	//TODO: disable F2F
    	RegistrationState oldState = getRegistrationState();
    	regState = RegistrationState.UNREGISTERED;
        fireRegistrationStateChanged(
        	oldState,
            RegistrationState.UNREGISTERED,
            RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
    }

    /**
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new states and notifies all currently registered listeners.
     *
     * @param oldState the state that the provider had before the change
     * occurred
     * @param newState the state that the provider is currently in.
     * @param reasonCode a value corresponding to one of the REASON_XXX fields
     * of the RegistrationStateChangeEvent class, indicating the reason for
     * this state transition.
     * @param reason a String further explaining the reason code or null if
     * no such explanation is necessary.
     */
    private void fireRegistrationStateChanged( RegistrationState oldState,
                                               RegistrationState newState,
                                               int               reasonCode,
                                               String            reason)
    {
        RegistrationStateChangeEvent event =
            new RegistrationStateChangeEvent(
                            this, oldState, newState, reasonCode, reason);

        Iterator<RegistrationStateChangeListener> listeners = null;
        synchronized (registrationListeners)
        {
            listeners = new ArrayList<RegistrationStateChangeListener>(registrationListeners).iterator();
        }

        while (listeners.hasNext())
        {
            RegistrationStateChangeListener listener
                = (RegistrationStateChangeListener) listeners.next();

            listener.registrationStateChanged(event);
        }
    }

    /**
     * F2FMulti implementation of the corresponding ProtocolProviderService method.
     */
    public void shutdown()
    {
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented by
     * this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * F2FMulti implementation of the corresponding ProtocolProviderService method.
     * 
     * We have no icon corresponding to this protocol provider at the moment.
     */
    public ProtocolIcon getProtocolIcon()
    {
        return f2fIcon;
    }
}
