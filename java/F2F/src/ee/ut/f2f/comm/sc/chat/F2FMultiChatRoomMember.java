package ee.ut.f2f.comm.sc.chat;

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class F2FMultiChatRoomMember
	implements ChatRoomMember
{
    private ChatRoom chatRoom = null;
    private String displayName = null;
    private String contactAddress = null;
    private ChatRoomMemberRole role = null;
    private Contact contact = null;
    Contact getContact() { return contact; }
    
    //TODO add reference to corresponding F2F peer
    public F2FMultiChatRoomMember(Contact contact, ChatRoom chatRoom, 
        ChatRoomMemberRole role)
    {
        this.chatRoom = chatRoom;
        this.role = role;
        this.contact = contact;
    }
    
    public F2FMultiChatRoomMember(String displayName, String contactAddress, 
    		ChatRoom chatRoom, ChatRoomMemberRole role)
    {
        this.chatRoom = chatRoom;
        this.displayName = displayName;
        this.contactAddress = contactAddress;
        this.role = role;
    }
 
    /**
     * Returns the chat room that this member is participating in.
     *
     * @return the <tt>ChatRoom</tt> instance that this member belongs to.
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    /**
     * Returns the protocol provider instance that this member has originated
     * in.
     *
     * @return the <tt>ProtocolProviderService</tt> instance that created this
     * member and its containing cht room
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return chatRoom.getParentProvider();
    }

    /**
     * Returns the contact identifier representing this contact. In protocols
     * like IRC this method would return the same as getName() but in others
     * like Jabber, this method would return a full contact id uri.
     *
     * @return a String (contact address), uniquely representing the contact
     * over the service the service being used by the associated protocol
     * provider instance/
     */
    public String getContactAddress()
    {
    	if (contact != null)
    		return contact.getAddress();
    	else
    		return contactAddress;
    }

    /**
     * Returns the name of this member as it is known in its containing
     * chatroom (aka a nickname). The name returned by this method, may
     * sometimes match the string returned by getContactID() which is actually
     * the address of  a contact in the realm of the corresponding protocol.
     *
     * @return the name of this member as it is known in the containing chat
     * room (aka a nickname).
     */
    public String getName()
    {
    	if (contact != null)
    		return contact.getDisplayName();
    	else
    		return displayName;
    }

    /**
     * Returns the role of this chat room member in its containing room.
     *
     * @return a <tt>ChatRoomMemberRole</tt> instance indicating the role
     * the this member in its containing chat room.
     */
    public ChatRoomMemberRole getRole()
    {
        return role;
    }
}
