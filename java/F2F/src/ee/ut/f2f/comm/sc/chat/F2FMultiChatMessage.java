package ee.ut.f2f.comm.sc.chat;

import java.io.Serializable;
import java.util.Map;

import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;

public class F2FMultiChatMessage
	implements Message, Serializable
{
	private static final long serialVersionUID = 1L;
	
	public enum Type
	{
		INVITATION,
		INVITATION_ANSWER,
		MEMBER_JOINED,
		MEMBERS,
		MESSAGE
	}
	protected Type type;
	Type getType() { return type; }

	private String textContent = null;

    private String contentType = null;

    private String contentEncoding = null;

    private String messageUID = null;

    private String subject = null;
    
    private String roomName = null;
	String getRoomName() { return roomName; }
    
	private String sourceAddress = null;
	String getSourceAddress() { return sourceAddress; }
	void setSourceAddress(String sourceAddress)
	{
		this.sourceAddress = sourceAddress;	
	}

	F2FMultiChatMessage(F2FMultiChatRoom chatRoom,
    		String content,
    		String contentType,
    		String contentEncoding,
    		String subject)
    {
    	type = Type.MESSAGE;
        this.textContent = content;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.subject = subject;
        this.roomName = chatRoom.getName();
        setSourceAddress(chatRoom);

        //generate the uid
        this.messageUID = String.valueOf( System.currentTimeMillis())
                          + String.valueOf(hashCode());
    }
    
	F2FMultiChatMessage(F2FMultiChatRoom chatRoom, 
    		String content)
    {
    	type = Type.MESSAGE;
        this.textContent = content;
        this.contentType = 
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE;
        this.contentEncoding = 
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING;
        this.subject = null;
        this.roomName = chatRoom.getName();
        setSourceAddress(chatRoom);

        //generate the uid
        this.messageUID = String.valueOf( System.currentTimeMillis())
                          + String.valueOf(hashCode());
    }
    
    private void setSourceAddress(F2FMultiChatRoom chatRoom)
    {
    	if (chatRoom.getOwner() != null)
    		sourceAddress = chatRoom.getOwner().getProtocolProvider().getAccountID().getAccountAddress();
    }

	/**
     * Returns the content of this message if representable in text form or null
     * if this message does not contain text data.
     * @return a String containing the content of this message or null if the
     * message does not contain data representable in text form.
     */
    public String getContent()
    {
        return textContent;
    }

    /**
     * Returns the MIME type for the message content.
     * @return a String containing the mime type of the message contant.
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Returns the MIME content encoding of this message.
     * @return a String indicating the MIME encoding of this message.
     */
    public String getEncoding()
    {
        return contentEncoding;
    }

    /**
     * Returns a unique identifier of this message.
     * @return a String that uniquely represents this message in the scope of
     * this protocol.
     */
    public String getMessageUID()
    {
        return messageUID;
    }

    /**
     * Get the raw/binary content of an instant message.
     * @return a byte[] array containing message bytes.
     */
    public byte[] getRawData()
    {
        return getContent().getBytes();
    }

    /**
     * Returns the size of the content stored in this message.
     * @return an int indicating the number of bytes that this message contains.
     */
    public int getSize()
    {
        return getContent().length();
    }

    /**
     * Returns the subject of this message or null if the message contains no
     * subject.
     * @return the subject of this message or null if the message contains no
     * subject.
     */
    public String getSubject()
    {
        return subject;
    }
}

class F2FMultiInvitation
	extends F2FMultiChatMessage
{
    private static final long serialVersionUID = 1622192432370483593L;
	F2FMultiInvitation(F2FMultiChatRoom chatRoom, String sourceAddress, String reason)
	{
		super(chatRoom, reason);
		type = Type.INVITATION;
		setSourceAddress(sourceAddress);
	}
	String getReason() { return getContent(); }
}

class F2FMultiInvitationAnswer
	extends F2FMultiChatMessage
{
	private static final long serialVersionUID = -6829222123748756017L;
	
	private boolean accepted;
	F2FMultiInvitationAnswer(F2FMultiChatRoom chatRoom, boolean accepted, String reason)
	{
		super(chatRoom, reason);
    	type = Type.INVITATION_ANSWER;
		this.accepted = accepted;
	}
	boolean hasAccepted() { return accepted; }
	String getReason() { return getContent(); }
}

class F2FMultiChatMessageMemberJoined
	extends F2FMultiChatMessage
{
	private static final long serialVersionUID = 5494138695516628053L;
	
	F2FMultiChatMessageMemberJoined(F2FMultiChatRoom chatRoom,
		String memberDisplayName, 
		String memberContactAdderss)
	{
		super(chatRoom, memberDisplayName, memberContactAdderss, null, null);
		type = Type.MEMBER_JOINED;
	}
	String getMemberDisplayName() { return getContent(); }
	String getMemberContactAddress() { return getContentType(); }
}

class F2FMultiChatMessageMembers
	extends F2FMultiChatMessage
{
	private static final long serialVersionUID = -2543175001655383006L;

	Map<String, String> members = null;
	/**
	 * @param chatRoom The chat room where given members belong
	 * @param members ContactAddress -> DisplayName
	 */
	F2FMultiChatMessageMembers(F2FMultiChatRoom chatRoom, Map<String, String> members)
	{
		super(chatRoom, null);
		type = Type.MEMBERS;
		this.members = members;
	}
	Map<String, String> getMembers() { return members; }
}