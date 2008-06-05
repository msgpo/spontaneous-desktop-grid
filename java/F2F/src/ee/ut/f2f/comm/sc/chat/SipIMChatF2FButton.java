package ee.ut.f2f.comm.sc.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.osgi.framework.ServiceReference;

import ee.ut.f2f.core.F2FComputing;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.PluginComponent;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.ProtocolIcon;

@SuppressWarnings("serial")
class SipIMChatF2FButton
	implements ActionListener,
    PluginComponent
{
	private F2FMultiOperationSetMultiUserChat multiUserChat = null;
	private JButton button = null;
	
	SipIMChatF2FButton(F2FMultiOperationSetMultiUserChat chat)
	{
        button = new JButton();
		this.multiUserChat = chat;
		ImageIcon icon = F2FMultiProtocolIcon.getImageIcon(ProtocolIcon.ICON_SIZE_16x16);
		if (icon != null)
            button.setIcon(icon);
		else
            button.setText("F2F");
		//setToolTipText("");
		//setEnabled(false);
        button.addActionListener(this);
	}

	public void actionPerformed(ActionEvent evt)
	{
		// find proper ChatRoom
		ServiceReference uiServiceRef = 
			multiUserChat.getProtocolProvider().getSipCommProvider()
				.getBundleContext().getServiceReference(UIService.class.getName());
		if (uiServiceRef == null)
			return;
		UIService uiService = (UIService) 
			multiUserChat.getProtocolProvider().getSipCommProvider()
				.getBundleContext().getService(uiServiceRef);
		if (uiService == null)
			return;
	    Chat chat = uiService.getCurrentChat();
		for (F2FMultiChatRoom room: multiUserChat.getExistingChatRooms())
		{
			if (uiService.getChat(room).equals(chat) && room.getOwner() == null)
			{// the owner of the room wants to start a job
				F2FComputing.startJob(room.getF2FPeers());
			}
		}
	}

    public Object getComponent()
    {
        return button;
    }

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return Container.CONTAINER_CHAT_TOOL_BAR;
    }

    public void setCurrentContact(MetaContact metaContact)
    {
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
    }

    public String getName()
    {
        return null;
    }

    public int getPositionIndex()
    {
        return -1;
    }
}