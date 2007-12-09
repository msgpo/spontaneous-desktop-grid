//FIXME: if we begin kicking or adding people from/to the chat and any of those
//people change their name at that moment, the operation will "miss".
//To solve this, unique IDs must be created that make sense across "the system" -
//something along the lines of chat initator creating these upon creating chat and adding people.
//TODO: closing the chat window should abort all jobs associated with it.
//FIXME: if someone changes their display name, the update won't be propagated beyond chat host - 
//other participants (who don't know the person changing their name) won't see new name.
//TODO: abort jobs in progress if the chat is closed

package ee.ut.f2f.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.model.FriendModel;
import ee.ut.f2f.util.F2FMessage;

/**
 * @author Jaan Neljandik
 * @created 19.11.2007
 */
public class GroupChatWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public static final String CHAT_TYPE_MSG = "msg"; 
	public static final String CHAT_TYPE_CTRL = "ctrl";
	public static final String CHAT_TYPE_END = "end"; 
	public static final String CHAT_OPTYPE_ADD = "+"; 
	public static final String CHAT_OPTYPE_REM = "-"; 
	
	private JTextArea typeArea;
	private JTextArea receievedMessagesTextArea;
	private JScrollPane receievedMessagesTextAreaScrollPane;
	
	private JPanel messagingPanel;
	private JList memberList;

	private JButton removeButton;
	
	private FriendModel memberModel;
	private UIController mainWindow;
	private JobSelector jobSelect;
	private String chatId;
	private boolean isCreator;
	private F2FPeer creator;	 
	private Collection<F2FPeer> selectedMembers = new ArrayList<F2FPeer>();
	 
	public GroupChatWindow(Collection<F2FPeer> members, UIController mainWnd, String chatId, boolean isCreator){
		this.mainWindow = mainWnd;
		this.setSize(new Dimension(400, 300));
		this.setLocationRelativeTo(null);
		this.isCreator = isCreator;
		this.setTitle("F2FChat");
		
		// This id should never contain strings or delimiters used in message structure. 
		// Refer to MESSAGE_STRUCTURE
		this.chatId = chatId != null ? chatId : String.valueOf(System.currentTimeMillis());
		
		// Messaging panel elements.
		messagingPanel = new JPanel();
		messagingPanel.setLayout(new BorderLayout());
		messagingPanel.setPreferredSize(new Dimension(400, 300));
		
		receievedMessagesTextArea = new JTextArea();
		receievedMessagesTextArea.setEditable(false);
		receievedMessagesTextArea.setLineWrap(true);
		
		receievedMessagesTextAreaScrollPane = new JScrollPane(receievedMessagesTextArea); 
		receievedMessagesTextAreaScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		receievedMessagesTextAreaScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		typeArea = new JTextArea();
		typeArea.addKeyListener(new KeyListener() {
			boolean shiftDown = false;
			
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
					shiftDown = true;
				}
			}
			public void keyTyped(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if(!shiftDown) {
						sendButtonPressed();
					}
					else {
						typeArea.setText(typeArea.getText() + "\n");
					}
				}				
				else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
					shiftDown = false;
				}
			}			
		});
		typeArea.setLineWrap(true);
		JScrollPane typeAreaScrollPane = new JScrollPane(typeArea); 
		typeAreaScrollPane.setPreferredSize(new Dimension(310, 100));
		typeAreaScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		typeAreaScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		memberModel = new FriendModel();
		memberList = new JList(memberModel);
		
		if (isCreator){
			if(members.contains(F2FComputing.getLocalPeer())) {
				members.remove(F2FComputing.getLocalPeer());
			}
			
			memberModel.add(F2FComputing.getLocalPeer());
			addMembers(members);
		}
		else {
			memberModel.add(F2FComputing.getLocalPeer());
		}
		
		memberList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		memberList.addListSelectionListener(new MembersListListener());
		memberList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(memberList);
		listScroller.setPreferredSize(new Dimension(100, 500));
		listScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		messagingPanel.add(listScroller, BorderLayout.EAST);
		
		
		JButton addButton = new JButton("Add...");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addButtonPressed();
			}
		});		
		
		JButton sendMessageButton = new JButton("Send");
		sendMessageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendButtonPressed(); 
			}
		});
		
		removeButton = new JButton("Kick");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeButtonPressed();
			}
		});
		removeButton.setEnabled(false);
		
		JButton startJobButton  = new JButton("Job...");
		startJobButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(jobSelect != null) {
					jobSelect.dispose();
				}
				jobSelect = new JobSelector(mainWindow);
			}
		}); 
		
		messagingPanel.add(receievedMessagesTextAreaScrollPane, BorderLayout.CENTER);		
		JPanel southPanel = new JPanel(new GridBagLayout());
		messagingPanel.add(southPanel, BorderLayout.SOUTH);
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 4;
		
		southPanel.add(typeAreaScrollPane, c);
		
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 0;
		southPanel.add(sendMessageButton, c);
		
		if (isCreator) {
			c.gridx = 1;
			c.gridy = 1;
			southPanel.add(addButton, c);
			
			c.gridx = 1;
			c.gridy = 2;
			southPanel.add(removeButton, c);
			
			c.gridx = 1;
			c.gridy = 3;
			southPanel.add(startJobButton, c);
		}
		
		this.setContentPane(messagingPanel);
		this.setVisible(true);
		
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		WindowListener windowListener = new WindowAdapter() {
			public void windowClosing(WindowEvent w) {
				int response = JOptionPane.showConfirmDialog(null, "Closing the window will make you leave the chat. Continue?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(response == JOptionPane.YES_OPTION) {
					onWindowClose();
				}
			}
		};
		
		this.addWindowListener( windowListener );
	}
	
	public void onWindowClose() {
		if(isCreator) {
			String kickMessage = CHAT_TYPE_END + ";" + chatId + ";" + CHAT_OPTYPE_REM;
			
			for (F2FPeer peer : memberModel.getPeers()) {
				memberModel.remove(peer);
				
				try	{
					if(!peer.getID().equals(F2FComputing.getLocalPeer().getID())) {
						peer.sendMessage(new F2FMessage(F2FMessage.Type.CHAT, null, null, null, kickMessage));
					}
				}
				catch (CommunicationFailedException cfe) {
					mainWindow.error("Sending message failed: "	+ cfe.getMessage());
				}
			}
		}
		else {
			String notifyMessage = CHAT_TYPE_CTRL + ";" + chatId + ";" + CHAT_OPTYPE_REM + ";";
			
			try	{
				creator.sendMessage(new F2FMessage(F2FMessage.Type.CHAT, null, null, null, notifyMessage));
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message failed: "	+ cfe.getMessage());
			}
		}
		
		mainWindow.killChat(chatId);
		this.dispose();
	}
	
	public void writeMessage(String from, String msg) {
		if(msg.trim().length() > 0) {
			receievedMessagesTextArea.append("\n");
			receievedMessagesTextArea.append(from + ": " + msg);
			receievedMessagesTextAreaScrollPane.getVerticalScrollBar().setValue(receievedMessagesTextAreaScrollPane.getVerticalScrollBar().getMaximum());
		}
	} 
	
	public static String findMsgType(String message) {
		return message.split(";", 2)[0];
	}
	
	public static String findChatId(String message) {
		return message.split(";", 3)[1];
	}
	
	public static String findRestOfMsg(String message) {
		return message.split(";", 3)[2];
	}
	
	public void chatMessageReceived(String message, F2FPeer sender) {
		//Message structure: sender;text
		String[] msgParts = message.split(";", 2); 
		
		String from = msgParts[0];
		String messageText = msgParts[1];
		
		if(from.length() == 0) {
			from = sender.getDisplayName();			
		}
		
		writeMessage(from, messageText);
		
		// Forwards the message
		if(isCreator) {
			sendMessage(from, messageText, sender);
		}
	}
	
	public void chatControlReceived(String control, F2FPeer src) {
		//Message structure: operationType;member;member;member...
		if (this.creator == null && isCreator == false) {
			this.creator = src;
			memberModel.add(creator);
		} 
		
		int separatorIndex = control.indexOf(";");
		if(separatorIndex != -1) {
			String operation = control.substring(0, separatorIndex);
			String[] members = control.substring(separatorIndex + 1).split(";");
			
			if (operation.equals(CHAT_OPTYPE_ADD)) {
				//Add people to chat
				for (String member : members) {
					memberModel.add(new F2FPeer(member));
				}
			}
			else if (operation.equals(CHAT_OPTYPE_REM)) {//Remove people from chat
				if(isCreator) {
					String notifyMessage = CHAT_TYPE_CTRL + ";" + chatId + ";" + CHAT_OPTYPE_REM + ";" + src.getDisplayName();
					F2FMessage notifyMsg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, notifyMessage);
					
					for (F2FPeer peer : memberModel.getPeers()) {
						try	{
							if(!peer.getID().equals(F2FComputing.getLocalPeer().getID()) &&
									!peer.getID().equals(src.getID())) { 
								peer.sendMessage(notifyMsg);
							}
						}
						catch (CommunicationFailedException cfe) {
							mainWindow.error("Sending message failed: "	+ cfe.getMessage());
						}
					}
					
					memberModel.remove(src);
				}
				else {
					for (String member : members) {
						for (F2FPeer peer : memberModel.getPeers()){
							if (peer.getDisplayName().equals(member)) {
								memberModel.remove(peer);
								break;
							}
						}				
					}
				}
			}
		}		
	}
	
	private void addButtonPressed() {
		Collection<F2FPeer> peers = mainWindow.getFriendModel().getPeers();
		Collection<F2FPeer> peopleList = new ArrayList<F2FPeer>();
		for (F2FPeer peer : peers) {
			boolean isInChat = false;
			for (F2FPeer member : memberModel.getPeers()) {
				if (peer.equals(member)) {
					isInChat = true;
					break;
				}
			}
			
			if (!isInChat) {
				peopleList.add(peer);
			}
		}
		
		if(peopleList.size() > 0) {	
			new PeopleChooser(peopleList, this);
		} 
		else {
			JOptionPane.showMessageDialog(this, "There's no people in friendlist to add", "Can't add people", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	private void removeButtonPressed() {
		//Message structure: end;chatId
		String kickMessage = CHAT_TYPE_END + ";" + chatId + ";" + CHAT_OPTYPE_REM;
		F2FMessage kickMsg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, kickMessage);
		String notifyMessage = CHAT_TYPE_CTRL + ";" + chatId + ";" + CHAT_OPTYPE_REM;
		
		// Send message to removed people
		for (F2FPeer selectedPeer : selectedMembers) {
			memberModel.remove(selectedPeer);
			notifyMessage = notifyMessage + ";" + selectedPeer.getDisplayName();			
			try	{
				selectedPeer.sendMessage(kickMsg);
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message failed: "	+ cfe.getMessage());
			}
		}		
		
		// Notify others
		F2FMessage notifyMsg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, notifyMessage);
		for (F2FPeer peer : memberModel.getPeers()) {
			try	{
				if(!peer.getID().equals(F2FComputing.getLocalPeer().getID())) { 
					peer.sendMessage(notifyMsg);
				}
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message failed: "	+ cfe.getMessage());
			}
		}
		
		selectedMembers.clear();	
		memberList.clearSelection();
		removeButton.setEnabled(false);		
	}	
	
	private void sendMessage(String from, String msg, F2FPeer sender) {
		String message =  CHAT_TYPE_MSG + ";" + chatId + ";" + from + ";" + msg;
		 
		F2FMessage mess = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, message);
		 
		if (isCreator) {
			//Send to everyone but self
			for (F2FPeer peer : ((FriendModel)memberList.getModel()).getPeers()) {
				if(!peer.getID().equals(F2FComputing.getLocalPeer().getID()) && 
						(sender == null || !peer.getID().equals(sender.getID()))) {
					try	{
						peer.sendMessage(mess);
					}
					catch (CommunicationFailedException cfe) {
						mainWindow.error("Sending message failed: "	+ cfe.getMessage());
					}
				}
			}
		}
		else {
			// Send to creator
			try	{
				creator.sendMessage(mess);
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message failed: "	+ cfe.getMessage());
			}
		}
	}
	
	private void sendButtonPressed() {
		sendMessage("", typeArea.getText().trim(), null);
		writeMessage(F2FComputing.getLocalPeer().getDisplayName(), typeArea.getText().trim());
		typeArea.setText("");
	}
	
	public String getChatId() {
		return chatId;
	}
	
	public void addMembers(Collection<F2FPeer> membersToAdd) {
		
		// Message structure: ctrl;chatId;+;member;member;...
		String messageStruct = CHAT_TYPE_CTRL + ";" + chatId + ";" + CHAT_OPTYPE_ADD;
		String message;
		
		// Send new members to old members
		for (F2FPeer member : memberModel.getPeers()) {
			if(!member.getID().equals(F2FComputing.getLocalPeer().getID())) {
				try	{
					message = messageStruct;
					for (F2FPeer memberToAdd : membersToAdd) {
						message = message + ";" + memberToAdd.getDisplayName();
					}	
					
					member.sendMessage(new F2FMessage(F2FMessage.Type.CHAT, null, null, null, message));
				}
				catch (CommunicationFailedException cfe) {
					mainWindow.error("Sending message failed: "	+ cfe.getMessage());
				}
			}
		}
		
		// Adds new members to my list
		for (F2FPeer memberToAdd : membersToAdd) {
			if(!memberToAdd.getID().equals(F2FComputing.getLocalPeer().getID())) {
				memberModel.add(memberToAdd);
			}
		}	
		
		// Send memberlist to new members
		for (F2FPeer memberToAdd : membersToAdd) {
			if(memberToAdd.getID().equals(F2FComputing.getLocalPeer().getID())) {
				continue;
			}
			
			try	{
				message = messageStruct;
				for (F2FPeer member : memberModel.getPeers()) {
					if(!member.getID().equals(F2FComputing.getLocalPeer().getID()) && 
							!member.getID().equals(memberToAdd.getID())) {
						message = message + ";" + member.getDisplayName();
					}
				}	
				
				memberToAdd.sendMessage(new F2FMessage(F2FMessage.Type.CHAT, null, null, null, message));
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message failed: "	+ cfe.getMessage());
			}
		}	
		
		
		if (memberList.getSelectedIndices().length > 0) {
			removeButton.setEnabled(true);
		}		
	}
	
	private class MembersListListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent listSelectionEvent) {
		
			// Return if this is one of multiple change events.
			if (listSelectionEvent.getValueIsAdjusting())
				return;
			
			// Process the event
			selectedMembers.clear();
			
			for (int i : memberList.getSelectedIndices()) {
				selectedMembers.add(memberModel.getElementAt(i));
			}
			
			// Can't remove yourself
			if(selectedMembers.size() == 0
					|| (selectedMembers.contains(F2FComputing.getLocalPeer()))) {
				removeButton.setEnabled(false);
			}
			else {
				removeButton.setEnabled(true);
			}
		}
	}
}
