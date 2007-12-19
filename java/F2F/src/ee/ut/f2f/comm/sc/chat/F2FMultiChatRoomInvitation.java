package ee.ut.f2f.comm.sc.chat;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Jabber implementation of the <tt>ChatRoomInvitation</tt> interface.
 * 
 * @author Yana Stamcheva
 */
class F2FMultiChatRoomInvitation
    implements ChatRoomInvitation
{
    private ChatRoom chatRoom;
    
    private String inviter;
    
    private String reason;
    
    private byte[] password;
    
    /**
     * Creates an invitation for the given <tt>targetChatRoom</tt>, from the
     * given <tt>inviter</tt>.
     * 
     * @param targetChatRoom the <tt>ChatRoom</tt> for which the invitation is
     * @param inviter the <tt>ChatRoomMember</tt>, which sent the invitation
     * @param reason the reason of the invitation
     */
    public F2FMultiChatRoomInvitation(ChatRoom targetChatRoom,
                                        String inviter,
                                        String reason,
                                        byte[] password)
    {
        this.chatRoom = targetChatRoom;
        this.inviter = inviter;
        this.reason = reason;
        this.password = password;
    }
    
    public ChatRoom getTargetChatRoom()
    {
        return chatRoom;
    }

    public String getInviter()
    {
        return inviter;
    }

    public String getReason()
    {
        return reason;
    }

    public byte[] getChatRoomPassword()
    {
        return password;
    }
}
