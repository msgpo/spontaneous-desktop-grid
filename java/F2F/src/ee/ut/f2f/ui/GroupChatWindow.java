package ee.ut.f2f.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
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
	
	private JTextArea typeArea = null;
	private JTextArea receievedMessagesTextArea = null;
	
	private JPanel messagingPanel = null;
	private JList memberList = null;

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
		
		// This id should never contain strings or delimiters used in message structure. 
		// Refer to MESSAGE_STRUCTURE
		this.chatId = chatId != null ? chatId : String.valueOf(System.currentTimeMillis());
		
		// Messaging panel elements.
		messagingPanel = new JPanel();
		messagingPanel.setLayout(new BorderLayout());
		messagingPanel.setPreferredSize(new Dimension(400, 300));
		
		receievedMessagesTextArea = new JTextArea();
		receievedMessagesTextArea.setEditable(false);
		
		JScrollPane receievedMessagesTextAreaScrollPane = new JScrollPane(receievedMessagesTextArea); 
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
			addMembers(members, true);		
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
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 4;
		
		southPanel.add(typeAreaScrollPane, c);
		
		c.gridheight = 1;
		c.weighty = 0.3;
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
	}
	
	public void writeMessage(String from, String msg) {
		if(msg.trim().length() > 0) {
			receievedMessagesTextArea.append("\n");
			receievedMessagesTextArea.append(from + ": " + msg);
			typeArea.setText("");
		}
	} 
	
	public static String findMsgType(String message) {
		return message.split(";", 2)[0];
	}
	
	public static String findChatId(String message) {
		return message.split(";", 3)[1];
	}
	
	public static String findMsg(String message) {
		return message.split(";", 3)[2];
	}
	
	public void chatMessageReceived(String message) {
		//Message structure: sender;text
		String[] msgParts = message.split(";", 2); 
		writeMessage(msgParts[0], msgParts[1]);
		if(isCreator) {
			//TODO: Send everyone but self
		}  
	}
	
	public void chatControlReceived(String control, F2FPeer creator) {
		//Message structure: operationType;member;member;member...
		//FIXME: Fix case when member has ; in his/her name (Also applies to chat message)
		if (this.creator == null) {
			this.creator = creator;
		} 
		
		int separatorIndex = control.indexOf(";");
		String operation = control.substring(0, separatorIndex);
		String[] members = control.substring(separatorIndex + 1).split(";");
		if (operation.equals(CHAT_OPTYPE_ADD)) {
			//Add people to chat
			for (String member : members) {
				memberModel.add(new F2FPeer(member));
			}
		}
		else if (operation.equals(CHAT_OPTYPE_REM)) {
			//Remove people from chat
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
		
		new PeopleChooser(peopleList, this);
	}
	
	private void removeButtonPressed() {
		//Message structure: end;chatId
		String message = CHAT_TYPE_END + ";" + chatId + ";" + CHAT_OPTYPE_REM;
		F2FMessage msg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, message);
		
		for (F2FPeer selectedPeer : selectedMembers) {
			memberModel.remove(selectedPeer);
			try	{
				selectedPeer.sendMessage(msg);
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message '"
						+ typeArea.getText() + "' to the peer '"
						+ selectedPeer.getDisplayName() + "' failed with '"
						+ cfe.getMessage() + "'");
			}
		}
		
		selectedMembers.clear();	
		removeButton.setEnabled(false);
	}	
	
	private void sendButtonPressed() {
		//Message structure: msg;chatId;sender;text
		String sender = F2FComputing.getLocalPeer().getDisplayName();
		String text = typeArea.getText().trim();		
		
		String message =  CHAT_TYPE_MSG + ";" + chatId + ";" + sender + ";" + text;
		
		F2FMessage msg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, message);
		
		if (isCreator) {
			for (F2FPeer peer : ((FriendModel)memberList.getModel()).getPeers()) {
				try	{
					peer.sendMessage(msg);
				}
				catch (CommunicationFailedException cfe) {
					mainWindow.error("Sending message '"
							+ typeArea.getText() + "' to the peer '"
							+ peer.getDisplayName() + "' failed with '"
							+ cfe.getMessage() + "'");
				}					
			}
		}
		else {
			try	{
				creator.sendMessage(msg);
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message '"
						+ typeArea.getText() + "' to the peer '"
						+ creator.getDisplayName() + "' failed with '"
						+ cfe.getMessage() + "'");
			}
		}
		writeMessage(F2FComputing.getLocalPeer().getDisplayName(), typeArea.getText().trim());
	}
	
	public String getChatId() {
		return chatId;
	}
	
	public void addMembers(Collection<F2FPeer> members, boolean addSelf) {
		//Message structure: ctrl;chatId;member;member;...
		String message = CHAT_TYPE_CTRL + ";" + chatId + ";" + CHAT_OPTYPE_ADD;
		
		if (addSelf) {
			memberModel.add(F2FComputing.getLocalPeer());
			message = message + ";" + F2FComputing.getLocalPeer().getDisplayName();
		}
		
		for (F2FPeer member : members) {
			memberModel.add(member);
			message = message + ";" + member.getDisplayName();
		}
		
		F2FMessage msg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, message);
		for (F2FPeer member : members) {
			try	{
				member.sendMessage(msg);
			}
			catch (CommunicationFailedException cfe) {
				mainWindow.error("Sending message '"
						+ typeArea.getText() + "' to the peer '"
						+ creator.getDisplayName() + "' failed with '"
						+ cfe.getMessage() + "'");
			}
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
