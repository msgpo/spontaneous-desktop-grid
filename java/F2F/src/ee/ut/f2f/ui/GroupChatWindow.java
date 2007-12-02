package ee.ut.f2f.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.model.FriendModel;
import ee.ut.f2f.util.F2FMessage;

/**
 * @author Jaan Neljandik
 * @created 19.11.2007
 */
public class GroupChatWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public static final String MESSAGE_STRUCTURE = "F2F;key;msg"; 
	
	private JTextArea typeArea = null;
	private JPanel messagingPanel = null;
	private JButton sendMessageButton = null;
	private JTextArea receievedMessagesTextArea = null;	
	private JList memberList = null;
	private FriendModel memberModel;
	private UIController mainWindow;
	private JButton addButton;
	private JButton removeButton;
	private String chatId;
	
	public GroupChatWindow(Collection<F2FPeer> members, UIController mainWindow, String chatId){
		
		this.mainWindow = mainWindow;
		this.setSize(new Dimension(400, 300));
		this.setLocationRelativeTo(null);
		
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
						onSendMessage();
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
		for (F2FPeer peer : members) {
			memberModel.add(peer);
		}
		
		memberList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		memberList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(memberList);
		listScroller.setPreferredSize(new Dimension(100, 500));
		listScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		messagingPanel.add(listScroller, BorderLayout.EAST);
		
		
		addButton = new JButton("Add...");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO: Add people
			}
		});		
		
		sendMessageButton = new JButton("Send");
		sendMessageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onSendMessage(); 
			}
		});
		
		removeButton = new JButton("Kick...");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO: Remove button
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
		c.gridheight = 3;
		
		southPanel.add(typeAreaScrollPane, c);
		
		c.gridheight = 1;
		c.weighty = 0.3;
		c.gridx = 1;
		c.gridy = 0;
		southPanel.add(sendMessageButton, c);
		
		c.gridx = 1;
		c.gridy = 1;
		southPanel.add(addButton, c);
		
		c.gridx = 1;
		c.gridy = 2;
		southPanel.add(removeButton, c);
		
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
	
	public void onPeopleAdd(Object[] selectedPeople) {
		
	}
	
	private void onSendMessage() {
		String messageText = MESSAGE_STRUCTURE;
		messageText = messageText.replaceAll("key", chatId);	
		messageText = messageText.replaceAll("msg", typeArea.getText());		
		
		F2FMessage msg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, messageText.trim());
		// get selected peers and send the message to them
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
		writeMessage("me", typeArea.getText().trim());
	}

	public String getChatId() {
		return chatId;
	}
	
	public static String findChatId(String message) {
		return message.split(";", 3)[1];
	}	
}
