package ee.ut.f2f.comm.sc.chat;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.util.F2FDebug;

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomConfigurationForm;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ChatRoomLocalUserRoleListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPropertyChangeListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberRoleListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeListener;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;

public class F2FMultiChatRoom
	implements ChatRoom
{
    private F2FMultiProtocolProviderService provider;
    
    private F2FMultiOperationSetMultiUserChat parentOpSet = null;
    
    private String name;

    private String subject;

    private String nickname;
    
    private Contact owner;
    Contact getOwner() { return owner; }

    private boolean joined = true;

    private Vector<F2FMultiChatRoomMember> members = new Vector<F2FMultiChatRoomMember>();

    /**
     * Currently registered member presence listeners.
     */
    private Vector<ChatRoomMemberPresenceListener> memberPresenceListeners = new Vector<ChatRoomMemberPresenceListener>();
    
    /**
     * Currently registered local user role listeners.
     */
    private Vector<ChatRoomLocalUserRoleListener> localUserRoleListeners = new Vector<ChatRoomLocalUserRoleListener>();
    
    /**
     * Currently registered member role listeners.
     */
    private Vector<ChatRoomMemberRoleListener> memberRoleListeners = new Vector<ChatRoomMemberRoleListener>();
    
    /**
     * Currently registered property change listeners.
     */
    private Vector<ChatRoomPropertyChangeListener> propertyChangeListeners = new Vector<ChatRoomPropertyChangeListener>();

    /**
     * Currently registered message listeners.
     */
    private Vector<ChatRoomMessageListener> messageListeners = new Vector<ChatRoomMessageListener>();
    
    public F2FMultiChatRoom(
    		F2FMultiProtocolProviderService provider, 
    		F2FMultiOperationSetMultiUserChat parentOpSet, 
    		String roomName,
    		Contact owner)
    {
        this.provider = provider;
        this.name = roomName;
        this.parentOpSet = parentOpSet;
        this.owner = owner;
        if (owner != null)
        {// the room was created after accepting an invitation
        	// add the owner of the room
        	F2FMultiChatRoomMember memberOwner = 
        		new F2FMultiChatRoomMember(owner, this, ChatRoomMemberRole.OWNER);
            addChatRoomMember(memberOwner);
        }
        else
        {// the room was created by the user, he is the owner, add him
        	join();
        }
    }

     /**
     * Returns the name of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this <tt>ChatRoom</tt>.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     */
    public void join()
    {
        joinAs(null, null);
    }

    /**
     * Joins this chat room so that the user would start receiving events and
     * messages for it. The method uses the nickname of the local user and the
     * specified password in order to enter the chatroom.
     *
     * @param password the password to use when authenticating on the chatroom.
     */
    public void join(byte[] password)
    {
        joinAs(null, password);
    }

    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it. If the chatroom already
     * contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     */
    public void joinAs(String nickname)
    {
        joinAs(nickname, null);
    }

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     * room.
     */
    public void joinAs(String nickname, byte[] password)
    {
        if(nickname == null)
            nickname = "me";
        
        this.nickname = nickname;
        
        F2FMultiChatRoomMember newMember = 
            new F2FMultiChatRoomMember(nickname, null, this, ChatRoomMemberRole.MEMBER);
        
        addChatRoomMember(newMember);
        
        LocalUserChatRoomPresenceChangeEvent evt = 
                new LocalUserChatRoomPresenceChangeEvent(
                        parentOpSet, 
                        this,
                        LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                        null);
        parentOpSet.fireLocalUserChatRoomPresenceChangeEvent(evt);
    }
    
    void addChatRoomMember(F2FMultiChatRoomMember member)
    {
    	synchronized (members)
    	{
    		if (!members.contains(member))
    	    	members.add(member);
		}
    }
    
    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     * otherwise.
     */
    public boolean isJoined()
    {
        return joined;
    }

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave()
    {
        
    }

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomChangelistener</tt> to this room.
     * {@link #addPropertyChangeListener(ChatRoomPropertyChangeListener)}
     * <p>
     * To change the room's subject use {@link #setSubject(String)}.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined the
     * room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or
     * the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException
     */
    public void setSubject(String subject)
        throws OperationFailedException
    {
        this.subject = subject;
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local chat room.
     */
    public String getUserNickname()
    {
        return nickname;
    }

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the setting the new nickname changes
     * for some reason.
     */
    public void setNickname(String nickname)
       throws OperationFailedException
    {
        this.nickname = nickname;
    }

    /**
     * Adds a listener that will be notified of changes in our participation in
     * the room such as us being kicked, join, left...
     * 
     * @param listener a member participation listener.
     */
    public void addMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
    	synchronized (memberPresenceListeners)
    	{
    		if(!memberPresenceListeners.contains(listener))
    			memberPresenceListeners.add(listener);
    	}
    }

    /**
     * Removes a listener that was being notified of changes in the
     * participation of other chat room participants such as users being kicked,
     * join, left.
     * 
     * @param listener a member participation listener.
     */
    public void removeMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
    	synchronized (memberPresenceListeners)
    	{
    		memberPresenceListeners.remove(listener);
    	}
    }

    /**
     * Notifies all <tt>ChatRoomMemberPresenceListener</tt>s that a ChatRoomMember has
     * joined or left this <tt>ChatRoom</tt>.
     *
     * @param evt the <tt>ChatRoomMemberPresenceChangeEvent</tt> that has happened 
     */
    public void fireMemberPresenceEvent(ChatRoomMemberPresenceChangeEvent evt)
    {
        Vector<ChatRoomMemberPresenceListener> listeners = null;
        synchronized (memberPresenceListeners)
        {
            listeners = new Vector<ChatRoomMemberPresenceListener>(memberPresenceListeners);
        }
        for (ChatRoomMemberPresenceListener listener: listeners)
        	listener.memberPresenceChanged(evt);
    }
    
    /**
     * Adds a listener that will be notified of changes in our role in the room
     * such as us being granded operator.
     * 
     * @param listener a local user role listener.
     */
    public void addLocalUserRoleListener(ChatRoomLocalUserRoleListener listener)
    {
        if(!localUserRoleListeners.contains(listener))
            localUserRoleListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in our role in this
     * chat room such as us being granded operator.
     * 
     * @param listener a local user role listener.
     */
    public void removelocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener)
    {
        localUserRoleListeners.remove(listener);
    }

    /**
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granded operator.
     * 
     * @param listener a member role listener.
     */
    public void addMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        if(!memberRoleListeners.contains(listener))
            memberRoleListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes of a member role in
     * this chat room such as us being granded operator.
     * 
     * @param listener a member role listener.
     */
    public void removeMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        memberRoleListeners.remove(listener);
    }

    /**
     * Adds a listener that will be notified of changes in the property of the
     * room such as the subject being change or the room state being changed.
     * 
     * @param listener a property change listener.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        if(!propertyChangeListeners.contains(listener))
            propertyChangeListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in the property of
     * the chat room such as the subject being change or the room state being
     * changed.
     * 
     * @param listener a property change listener.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        propertyChangeListeners.remove(listener);
    }

    private static int F2F_TEST_TIMEOUT = 20;// seconds to wait for F2F-capability test
    /**
     * Invites another user to this room.
     * <p>
     * If the room is password-protected, the invitee will receive a password to
     * use to join the room. If the room is members-only, the the invitee may
     * be added to the member list.
     *
     * @param userAddress the address of the user to invite to the room.(one
     * may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell the
     * the user why they are being invited.
     */
    public void invite(String userAddress, String reason)
    {
    	// we do not allow an ordinary member to invite other.
    	// only owner of the chat room can invite
    	if (owner != null)
    	{
    		//TODO: inform user that only Owner can invite people
			F2FDebug.println("only owner can add members to the chat room");
    		return;
    	}
    	final String sUserAddress = userAddress;
    	final String sReason = reason;
    	new Thread()
    	{
    		public void run()
    		{
    			// find the proper contact
    			Contact contact = provider.getSipCommProvider().findContact(sUserAddress);
    			if (contact == null)
    			{
    				//TODO: inform user that such contact was not found
    				F2FDebug.println("contact " + sUserAddress + " not found");
    				return;
    			}
    			if (!contact.getPresenceStatus().isOnline())
    			{
    				//TODO: inform user the contact is not online
    				F2FDebug.println("contact " + sUserAddress + " is not online");
    				return;
    			}
    			provider.getSipCommProvider().makeF2FTest(contact);
    			// wait until F2F-capability test is done
    			long start = System.currentTimeMillis();
    			while (true)
    			{
    				// if F2F-capability test was successful, continue
    				if (provider.getSipCommProvider().isKnownContact(contact)) break;
    				try {
						Thread.sleep(500);
					} catch (InterruptedException e) { }
					if (System.currentTimeMillis() - start > 1000 * F2F_TEST_TIMEOUT)
					{
						//TODO: inform the user, that F2F-capability test failed
						F2FDebug.println("contact " + sUserAddress + " F2F-capability test failed");
						return;
					}
    			}
    			
    			// send invitation to join
    			F2FMultiInvitation invitation = new F2FMultiInvitation(F2FMultiChatRoom.this, 
    					contact.getProtocolProvider().getAccountID().getAccountAddress(), sReason);
    			try
    			{
    				parentOpSet.sendF2FMultiChatMessage(invitation, contact);
				}
    			catch (CommunicationFailedException e)
				{
					//TODO: inform the user, that invitation could not be sent
					F2FDebug.println("could not send INVITATION to the contact " + contact.getDisplayName() + ": " + e);
				}
    		}
    	}.start();
    }

    /**
     * Returns a <tt>List</tt> of <tt>ChatRoomMember</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>ChatRoomMember</tt> instances
     * corresponding to all room members.
     */
    public List<F2FMultiChatRoomMember> getMembers()
    {
    	synchronized (members)
    	{
			return new Vector<F2FMultiChatRoomMember>(members);
		}
    }

    /**
     * Returns the number of participants that are currently in this chat room.
     * @return int the number of <tt>Contact</tt>s, currently participating in
     * this room.
     */
    public int getMembersCount()
    {
        return getMembers().size();
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     * @param listener a <tt>MessageListener</tt> that would be notified every
     * time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener)
    {
        if(!messageListeners.contains(listener))
            messageListeners.add(listener);
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener)
    {
        messageListeners.remove(listener);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new F2FMultiChatMessage(this, new String(content), contentType, 
            contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return new F2FMultiChatMessage(this, messageText);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(Message msg)
        throws OperationFailedException
    {
    	// check if the message is a F2FMultiChatMessage
    	if (msg instanceof F2FMultiChatMessage);
    	else 
    	{
    		F2FDebug.println("can not send a message that is not a F2FMultiChatMessage: " + msg);
    		return;
    	}
    	if (owner == null)
    	{// sender is the owner of the chat room
    		// send the message to all other members 
    		for (F2FMultiChatRoomMember member: getMembers())
    		{
    			// do not send the message to ourselves
    			if (member.getContact() == null) continue;
    			
    			F2FMultiChatMessage message = (F2FMultiChatMessage)msg;
    			message.setSourceAddress(member.getContact().getProtocolProvider().getAccountID().getAccountAddress());
    			try
    			{
					parentOpSet.sendF2FMultiChatMessage(message, member.getContact());
				}
    			catch (CommunicationFailedException e)
    			{
    				F2FDebug.println("could not send CHAT message to " + member.getName() + ": " + e);
				}
    		}
    	}
    	else
    	{// sender is just a member
    		// send the message to the chat room owner (he forwards it)
    		try
			{
				parentOpSet.sendF2FMultiChatMessage(msg, owner);
			}
			catch (CommunicationFailedException e)
			{
				F2FDebug.println("could not send CHAT message to chat owner " + owner.getDisplayName() + ": " + e);
			}
    	}
    	    	
    	// show the message as our own message
        ChatRoomMessageDeliveredEvent evt = 
            new ChatRoomMessageDeliveredEvent(
                    this,
                    new Date(),
                    msg,
                    ChatRoomMessageDeliveredEvent
                        .CONVERSATION_MESSAGE_DELIVERED);
        
        for (ChatRoomMessageListener listener: messageListeners)
            listener.messageDelivered(evt);
    }

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this room.
     */
    public ProtocolProviderService getParentProvider()
    {
        return provider;
    }

    /**
     * Returns an Iterator over a set of ban masks for this chat room. The ban
     * mask defines a group of users that will be banned. The ban list is a list
     * of all such ban masks defined for this chat room.
     * 
     * @return an Iterator over a set of ban masks for this chat room
     */
    public Iterator getBanList()
    {
        return new Vector().iterator();
    }
    
    /**
     * Method for showing received messages.
     *
     * @param msg the message that was received.
     */
    void deliverMessage(F2FMultiChatMessage msg)
    {
    	F2FMultiChatRoomMember fromMember = null;
    	for (F2FMultiChatRoomMember member: getMembers())
    		if (msg.getSourceAddress().equals(member.getContactAddress()))
    		{
    			fromMember = member;
    			break;
    		}
    	
    	if(fromMember == null)
    	{
    		F2FDebug.println("received a message from an unknown contact\n"
    				+ "\tcontact: " + msg.getSourceAddress()
    				+ "\n\tmessage: " + msg);
            return;
    	}
        
    	if (owner == null)
    	{
    		// forward the message to other contacts
    		for (F2FMultiChatRoomMember member: getMembers())
    		{
    			// do not send the message to ourselves
    			if (member.getContact() == null) continue;
    			
    			// do not send the message back to the originator
    			if (member.getContactAddress().equals(msg.getSourceAddress()))
    				continue;
    			
    			try
    			{
					parentOpSet.sendF2FMultiChatMessage(msg, member.getContact());
				}
    			catch (CommunicationFailedException e)
    			{
    				F2FDebug.println("could not send CHAT message to " + member.getName() + ": " + e);
				}
    		}
    	}
    	
        ChatRoomMessageReceivedEvent evt = 
            new ChatRoomMessageReceivedEvent(
                    this,
                    fromMember,
                    new Date(),
                    msg,
                    ChatRoomMessageReceivedEvent
                        .CONVERSATION_MESSAGE_RECEIVED);
        
        for (ChatRoomMessageListener listener: messageListeners)
	        listener.messageReceived(evt);
    }

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the setting the new nickname changes
     * for some reason.
     */
    public void setUserNickname(String nickname) throws OperationFailedException
    {
        this.nickname = nickname;
    }

    /**
     * Returns the identifier of this <tt>ChatRoom</tt>. The identifier of the
     * chat room would have the following syntax:
     * [chatRoomName]@[chatRoomServer]@[accountID]
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>ChatRoom</tt>.
     */
    public String getIdentifier()
    {
    	return name;
    }

    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {   
    }

    public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {   
    }

    public ChatRoomConfigurationForm getConfigurationForm()
        throws OperationFailedException
    {
        return null;
    }

    public void addMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener)
    {
    }

    public void removeMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener)
    {   
    }
    
    public boolean isSystem()
    {
        return false;
    }
}
