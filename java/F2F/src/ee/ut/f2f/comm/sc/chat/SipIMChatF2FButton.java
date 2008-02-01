package ee.ut.f2f.comm.sc.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.osgi.framework.ServiceReference;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.JobSelector;

import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.ProtocolIcon;

@SuppressWarnings("serial")
class SipIMChatF2FButton
	extends JButton
	implements ActionListener
{
	private F2FMultiOperationSetMultiUserChat multiUserChat = null;
	private JobSelector jobSelector = null;
	
	SipIMChatF2FButton(F2FMultiOperationSetMultiUserChat chat)
	{
		super();
		this.multiUserChat = chat;
		ImageIcon icon = F2FMultiProtocolIcon.getImageIcon(ProtocolIcon.ICON_SIZE_16x16);
		if (icon != null)
			setIcon(icon);
		else
			setText("F2F");
		//setToolTipText("");
		//setEnabled(false);
		addActionListener(this);
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
				Collection<F2FPeer> peers = new ArrayList<F2FPeer>();
				for (F2FMultiChatRoomMember chatMember: room.getMembers())
				{
					// do not include the local peer in the peers
					if (chatMember.getContact() == null) continue;
					
					F2FPeer peer = F2FComputing.getPeer(multiUserChat.getProtocolProvider().getSipCommProvider().getF2FPeerID(chatMember.getContact()));
					if (peer == null) continue;
					peers.add(peer);
				}
				if(jobSelector != null) jobSelector.dispose();
				jobSelector = new JobSelector(peers);
			}
		}
	}
}