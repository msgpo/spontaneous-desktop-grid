package ee.ut.f2f.comm.sc.chat;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.osgi.framework.ServiceReference;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.MessageListener;
import ee.ut.f2f.util.F2FDebug;

import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomInvitation;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationNotSupportedException;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.event.ChatRoomInvitationListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomInvitationReceivedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomInvitationRejectedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomInvitationRejectionListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceListener;

public class F2FMultiOperationSetMultiUserChat
	implements OperationSetMultiUserChat, MessageListener
{
	private static final String OWNER_CONTACT_ADDRESS = "F2FMulti Owner Contact Address";
    /**
     * The protocol provider service implementation.
     */
    private F2FMultiProtocolProviderService provider = null;
    F2FMultiProtocolProviderService getProtocolProvider() { return provider; }
    
    private Hashtable<String, F2FMultiChatRoom> existingChatRooms = new Hashtable<String, F2FMultiChatRoom>();
    private List<F2FMultiChatRoom> joinedChatRooms = new Vector<F2FMultiChatRoom>();
    
    /**
     * Currently registered invitation listeners.
     */
    private Vector<ChatRoomInvitationListener> invitationListeners = new Vector<ChatRoomInvitationListener>();
    
    /**
     * Currently registered invitation reject listeners.
     */
    private Vector<ChatRoomInvitationRejectionListener> invitationRejectListeners = new Vector<ChatRoomInvitationRejectionListener>();
    
    /**
     * Currently registered local user chat room presence listeners.
     */
    private Vector<LocalUserChatRoomPresenceListener> localUserChatRoomPresenceListeners = new Vector<LocalUserChatRoomPresenceListener>();
    
    private SipIMChatF2FButton chatButton = null;
    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     */
    public F2FMultiOperationSetMultiUserChat(F2FMultiProtocolProviderService provider)
    {
        this.provider = provider;
        addChatF2FButton();
        F2FComputing.addMessageListener(F2FMultiChatMessage.class, this);
    }
    
    private void addChatF2FButton()
    {
    	this.chatButton = new SipIMChatF2FButton(this);
		new Thread(new Runnable()
		{
			public void run()
			{
				while (true)
				{
					try
					{
						ServiceReference uiServiceRef = 
							F2FMultiOperationSetMultiUserChat.this.provider.getSipCommProvider()
								.getBundleContext().getServiceReference(UIService.class.getName());
						if (uiServiceRef == null)
						{
							Thread.sleep(1000);
							continue;
						}
						UIService uiService = (UIService) 
							F2FMultiOperationSetMultiUserChat.this.provider.getSipCommProvider()
								.getBundleContext().getService(uiServiceRef);
						if (uiService == null)
						{
							Thread.sleep(1000);
							continue;
						}
					    if(uiService.isContainerSupported(UIService.CONTAINER_CHAT_TOOL_BAR))
					    	uiService.addComponent(
					    		UIService.CONTAINER_CHAT_TOOL_BAR,
					    		F2FMultiOperationSetMultiUserChat.this.chatButton);
					    return;
					}
					catch (IllegalStateException e)
					{
						F2FDebug.println(e.toString());
						return;
					}
					catch (InterruptedException e1)
					{
						continue;
					}
					catch (Exception e)
					{
						e.printStackTrace();
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e1) {}
						continue;
					}
				}
			}
		}).start();
	}

	/**
     * Returns the <tt>List</tt> of <tt>String</tt>s indicating chat rooms
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @return a <tt>java.util.List</tt> of the name <tt>String</tt>s for chat
     * rooms that are currently available on the server that this protocol
     * provider is connected to.
     */
    public List<F2FMultiChatRoom> getExistingChatRooms()
    {
    	synchronized(existingChatRooms)
    	{
    		return new Vector<F2FMultiChatRoom>(existingChatRooms.values());
    	}
    }
    
    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using a
     * given connection.
     */
    public List getCurrentlyJoinedChatRooms()
    {
        return joinedChatRooms;
    }
    
    /**
     * Returns a list of the chat rooms that <tt>chatRoomMember</tt> has joined
     * and is currently active in.
     *
     * @param chatRoomMember the chatRoomMember whose current ChatRooms we will
     * be querying.
     * @return a list of the chat rooms that <tt>chatRoomMember</tt> has
     * joined and is currently active in.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public List<ChatRoom> getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember) 
        throws OperationFailedException, 
               OperationNotSupportedException
    {
        List<ChatRoom> result = new Vector<ChatRoom>();
        
        for (ChatRoom room: joinedChatRooms)
            if(room.getMembers().contains(chatRoomMember))
                result.add(room);
        
        return result;
    }
    
    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to. When the method returns the room the
     * local user will not have joined it and thus will not receive messages on
     * it until the <tt>ChatRoom.join()</tt> method is called.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     * created.
     * @throws OperationFailedException if the room couldn't be created for some
     * reason (e.g. room already exists; user already joined to an existant
     * room or user has no permissions to create a chat room).
     * @throws OperationNotSupportedException if chat room creation is not
     * supported by this server
     *
     * @return the newly created <tt>ChatRoom</tt> named <tt>roomName</tt>.
     */
    public F2FMultiChatRoom createChatRoom(String roomName, Hashtable roomProperties)
    {
    	synchronized (existingChatRooms)
    	{
    		if (existingChatRooms.containsKey(roomName))
    			return existingChatRooms.get(roomName);
    		Contact owner = null;
    		if (roomProperties != null && roomProperties.containsKey(OWNER_CONTACT_ADDRESS))
    		{
    			Object o = roomProperties.get(OWNER_CONTACT_ADDRESS);
    			if (o instanceof Contact)
    				owner = (Contact)roomProperties.get(OWNER_CONTACT_ADDRESS);
    		}
    		F2FMultiChatRoom room = new F2FMultiChatRoom(provider, this, roomName, owner);
    	    existingChatRooms.put(roomName, room);
            return room;
    	}
    }
    
    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt> or null if no
     * such room exists.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> or null if no such
     * room exists on the server that this provider is currently connected to.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public F2FMultiChatRoom findRoom(String roomName) 
    {
    	synchronized (existingChatRooms)
    	{
            if(existingChatRooms.containsKey(roomName))
            	return existingChatRooms.get(roomName);
    	}
        
    	return null;
    }
    
    /**
     * Adds a listener to invitation notifications. The listener will be fired
     * anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationRejectionListener(
        ChatRoomInvitationRejectionListener listener)
    {
    	synchronized (invitationRejectListeners)
    	{
    		if(!invitationRejectListeners.contains(listener))
    			invitationRejectListeners.add(listener);
    	}
    }
    
    /**
     * Removes the given listener from the list of invitation listeners
     * registered to receive events every time an invitation has been rejected.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationRejectionListener(
        ChatRoomInvitationRejectionListener listener)
    {
    	synchronized (invitationRejectListeners)
    	{
    		invitationRejectListeners.remove(listener);
    	}
    }
    
    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the invitation we are rejecting.
     * @param reason the reason for rejecting.
     */
    public void rejectInvitation(ChatRoomInvitation invitation, String reason)
    {
    	//TODO: inform the sender that I do not want to join the chat room
    	
        // this code is actually doing nothing now, because
    	// MultiUserChatManager does nothing on this event.
    	// but it may do one day
    	ChatRoomInvitationRejectedEvent evt = 
            new ChatRoomInvitationRejectedEvent(
                    this,
                    invitation.getTargetChatRoom(),
                    provider.getAccountID().getUserID(),
                    invitation.getReason(),
                    new Date());
    	Vector<ChatRoomInvitationRejectionListener> listeners = null;
    	synchronized (invitationRejectListeners)
    	{
    		listeners = new Vector<ChatRoomInvitationRejectionListener>(invitationRejectListeners);
    	}
        for (ChatRoomInvitationRejectionListener listener: listeners)
        	listener.invitationRejected(evt);
    }
    
    /**
     * Adds a listener to invitation notifications. The listener will be fired
     * anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationListener(ChatRoomInvitationListener listener)
    {
    	synchronized (invitationListeners)
    	{
	        if(!invitationListeners.contains(listener))
	            invitationListeners.add(listener);
    	}
    }
    
    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationListener(ChatRoomInvitationListener listener)
    {
    	synchronized (invitationListeners)
    	{
    		invitationListeners.remove(listener);
    	}
    }
	
    private void onInvitation(F2FMultiChatMessage msg)
	{
    	F2FMultiInvitation invitation = (F2FMultiInvitation)msg;
		Contact contact = provider.getSipCommProvider().findContact(invitation.getSourceAddress());
		if (contact == null)
		{
			F2FDebug.println("received a MULTI CHAT invitation from unknown contact " + invitation.getSourceAddress());
			return;
		}
		//F2FDebug.println("received a MULTI CHAT invitation from " + invitation.getInvitor()
		//		+ " to join " + invitation.getRoomName());
		Hashtable<String, Contact> properties = new Hashtable<String, Contact>();
		properties.put(OWNER_CONTACT_ADDRESS, contact);
		ChatRoom chatRoom = createChatRoom(invitation.getRoomName(), properties);
		
		// show the invitation
		String inviter = contact.getDisplayName() + " (" + invitation.getSourceAddress() + ")";
		F2FMultiChatRoomInvitation chatRoomInvitation
        	= new F2FMultiChatRoomInvitation(chatRoom, inviter, invitation.getReason(), null);
	    ChatRoomInvitationReceivedEvent evt
	        = new ChatRoomInvitationReceivedEvent(this, chatRoomInvitation,
	            new Date(System.currentTimeMillis()));
	    Vector<ChatRoomInvitationListener> listeners = null;
	    synchronized (invitationListeners)
	    {
	        listeners = new Vector<ChatRoomInvitationListener>(invitationListeners);
	    }
	    for (ChatRoomInvitationListener listener: listeners)
		    listener.invitationReceived(evt);
	}
    
    /**
     * Returns true if <tt>contact</tt> supports multi user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     * we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports chatrooms.
     */
    public boolean isMultiChatSupportedByContact(Contact contact)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Adds a listener that will be notified of changes in our participation in
     * a chat room such as us being kicked, joined, left.
     *
     * @param listener a local user participation listener.
     */
    public void addPresenceListener(
        LocalUserChatRoomPresenceListener listener)
    {
    	synchronized (localUserChatRoomPresenceListeners)
    	{
    		if(!localUserChatRoomPresenceListeners.contains(listener))
    			localUserChatRoomPresenceListeners.add(listener);
    	}
    }

    /**
     * Removes a listener that was being notified of changes in our
     * participation in a room such as us being kicked, joined, left.
     * 
     * @param listener a local user participation listener.
     */
    public void removePresenceListener(
        LocalUserChatRoomPresenceListener listener)
    {
    	synchronized (localUserChatRoomPresenceListeners)
    	{
    		localUserChatRoomPresenceListeners.remove(listener);
    	}
    }
    
    void fireLocalUserChatRoomPresenceChangeEvent(
        LocalUserChatRoomPresenceChangeEvent evt)
    {
    	if (evt.getEventType() == LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED)
    	{
    		// if we are not the owner of the room, inform the owner that we accepted his invitation
    		Contact owner = ((F2FMultiChatRoom)evt.getChatRoom()).getOwner();
    		if (owner != null)
    		{
    			F2FMultiInvitationAnswer answer = new F2FMultiInvitationAnswer((F2FMultiChatRoom)evt.getChatRoom(), true, null);
    			try
    			{
    				sendF2FMultiChatMessage(answer, owner);
				}
    			catch (CommunicationFailedException e)
				{
					//TODO: inform the user, that answer could not be sent
					F2FDebug.println("could not send INVITATION ANSWER to the contact " + owner.getDisplayName() + ": " + e);
				}
    		}
    	}
    	
    	// change the local chat window
    	Vector<LocalUserChatRoomPresenceListener> listeners = null;
    	synchronized (localUserChatRoomPresenceListeners)
    	{
    		listeners = new Vector<LocalUserChatRoomPresenceListener>(localUserChatRoomPresenceListeners);
    	}
    	for (LocalUserChatRoomPresenceListener listener: listeners)
        	listener.localUserPresenceChanged(evt);
    }
    
    void sendF2FMultiChatMessage(Message msg, Contact contact) throws CommunicationFailedException
    {
    	if (contact == null) return;
    	F2FPeer peer = F2FComputing.getPeer(provider.getSipCommProvider().getF2FPeerID(contact));
    	if (peer == null) return;
    	peer.sendMessage(msg);
    }

	public void messageReceived(Object message, F2FPeer sender)
	{
		if (message instanceof F2FMultiChatMessage)
		{
			F2FMultiChatMessage chatMsg = (F2FMultiChatMessage)message;
			if (chatMsg.getType() == F2FMultiChatMessage.Type.MESSAGE)
			{
				onChatMessage(chatMsg);
			}
			else if (chatMsg.getType() == F2FMultiChatMessage.Type.INVITATION)
			{
				onInvitation(chatMsg);
			}
			else if (chatMsg.getType() == F2FMultiChatMessage.Type.INVITATION_ANSWER)
			{
				if (((F2FMultiInvitationAnswer)chatMsg).hasAccepted())
					onInvitationAccepted(chatMsg);
				else
					onInvitationRejected(chatMsg);
			}
			else if (chatMsg.getType() == F2FMultiChatMessage.Type.MEMBER_JOINED)
			{
				onMemberJoined(chatMsg);
			}
			else if (chatMsg.getType() == F2FMultiChatMessage.Type.MEMBERS)
			{
				onMembers(chatMsg);
			}
		}
		else
		{
			F2FDebug.println("received a MULTI chat message that is not F2FMultiMessage: " + message);
		}
	}

	private void onMembers(F2FMultiChatMessage msg)
	{
		F2FMultiChatRoom room = findRoom(msg.getRoomName());
		if (room == null)
		{
			F2FDebug.println("MEMBERS has been sent for an unknown room: " + msg.getRoomName());
		}
		F2FMultiChatMessageMembers joinedMsg = (F2FMultiChatMessageMembers)msg;

		// add the members to the chat
		for (Map.Entry<String, String> entry: joinedMsg.getMembers().entrySet())
		{
			F2FMultiChatRoomMember member = 
	            new F2FMultiChatRoomMember(entry.getValue(), entry.getKey(), room, ChatRoomMemberRole.MEMBER);
			room.addChatRoomMember(member);
			ChatRoomMemberPresenceChangeEvent evt = 
				new ChatRoomMemberPresenceChangeEvent(
					room, member, ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
			room.fireMemberPresenceEvent(evt);
		}
	}

	private void onMemberJoined(F2FMultiChatMessage msg)
	{
		F2FMultiChatRoom room = findRoom(msg.getRoomName());
		if (room == null)
		{
			F2FDebug.println("MEMBER JOINED has been sent for an unknown room: " + msg.getRoomName());
		}
		F2FMultiChatMessageMemberJoined joinedMsg = (F2FMultiChatMessageMemberJoined)msg;

		// add the new member to the chat
		F2FMultiChatRoomMember member = 
            new F2FMultiChatRoomMember(joinedMsg.getMemberDisplayName(), joinedMsg.getMemberContactAddress(), room, ChatRoomMemberRole.MEMBER);
		room.addChatRoomMember(member);
		ChatRoomMemberPresenceChangeEvent evt = 
			new ChatRoomMemberPresenceChangeEvent(
				room, member, ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
		room.fireMemberPresenceEvent(evt);
	}

	private void onChatMessage(F2FMultiChatMessage msg)
	{
		F2FMultiChatRoom room = findRoom(msg.getRoomName());
		if (room == null)
		{
			F2FDebug.println("returned chat message for unknown chat room: " + msg.getRoomName());
			return;
		}
		room.deliverMessage(msg);
	}

	private void onInvitationAccepted(F2FMultiChatMessage msg)
	{
		F2FMultiChatRoom room = findRoom(msg.getRoomName());
		if (room == null)
		{
			F2FDebug.println("contact " + msg.getSourceAddress() + " has accepted INVITATION of an unknown chat room " + msg.getRoomName());
		}
		Contact contact = provider.getSipCommProvider().findContact(msg.getSourceAddress());

		// add the new member to the chat
		F2FMultiChatRoomMember newMember = 
            new F2FMultiChatRoomMember(contact, room, ChatRoomMemberRole.MEMBER);
		room.addChatRoomMember(newMember);
		
		// inform other members too, that a new member has joined the chat room
		// and inform the new member of other members
		F2FMultiChatMessageMemberJoined joinedMsg = new F2FMultiChatMessageMemberJoined(room, contact.getDisplayName(), contact.getAddress());
		for (F2FMultiChatRoomMember member: room.getMembers())
		{
			// do not send the message to ourselves
			if (member.getContact() == null) continue;
			
			// inform the new member of other members
			if (member.equals(newMember))
			{
				Hashtable<String, String> members = new Hashtable<String, String>();
				for (F2FMultiChatRoomMember m: room.getMembers())
					if (m.getContact() != null && !m.equals(newMember))
						members.put(m.getContactAddress(), m.getName());
				
				// if nothing to inform, do not send the message 
				if (members.size() < 1) continue;
				
				F2FMultiChatMessageMembers membersMsg = new F2FMultiChatMessageMembers(room, members);
				try
				{
					sendF2FMultiChatMessage(membersMsg, member.getContact());
				}
				catch (CommunicationFailedException e)
				{
					F2FDebug.println("could not send MEMBERS message to " + member.getName() + ": " + e);
				}
				continue;
			}
			
			try
			{
				sendF2FMultiChatMessage(joinedMsg, member.getContact());
			}
			catch (CommunicationFailedException e)
			{
				F2FDebug.println("could not send MEMBER_JOINED message to " + member.getName() + ": " + e);
			}
		}
		ChatRoomMemberPresenceChangeEvent evt = 
			new ChatRoomMemberPresenceChangeEvent(
				room, newMember, ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
		room.fireMemberPresenceEvent(evt);
	}

	private void onInvitationRejected(F2FMultiChatMessage msg)
	{
		// TODO Auto-generated method stub
	}
}
