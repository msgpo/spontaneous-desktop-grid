package ee.ut.f2f.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;

import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.model.FriendModel;

/**
 * @author Andres A.
 * @author Jaan Neljandik
 */
@SuppressWarnings("serial")
public class PeopleChooser extends JDialog
{
	private FriendModel<F2FPeer> friendModel;
	private JList friendList;
	private JButton okButton;
	private JButton cancelButton;
	private GroupChatWindow owner;
	private SpringLayout layout;
	private JPanel contentPanel;
	private JScrollPane listScroller;
	private JPanel buttonPanel;

	public PeopleChooser(Collection<F2FPeer> friends, GroupChatWindow owner)
	{
		this.owner = owner;
		this.setSize(150, 210);
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setTitle("Add");
		this.setAlwaysOnTop(true);
		this.setModal(true);

		contentPanel = new JPanel();
		layout = new SpringLayout();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
		
		friendModel = new FriendModel<F2FPeer>();
		friendList = new JList(friendModel);
		friendList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		friendList.setLayoutOrientation(JList.VERTICAL);
		for (F2FPeer friend : friends) {
			friendModel.add(friend);
		}
		listScroller = new JScrollPane(friendList);
		listScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		listScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		contentPanel.add(listScroller);

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		
		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOk();
			}
		});
		layout.putConstraint(SpringLayout.NORTH, okButton, 0, SpringLayout.SOUTH, listScroller);
		buttonPanel.add(okButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});
		layout.putConstraint(SpringLayout.NORTH, cancelButton, 0, SpringLayout.SOUTH, listScroller);
		layout.putConstraint(SpringLayout.WEST, cancelButton, 0, SpringLayout.EAST, okButton);
		buttonPanel.add(cancelButton);
		contentPanel.add(buttonPanel);
		this.setContentPane(contentPanel);
		this.setVisible(true);
		
	}

	private void onOk() {
		int[] selectedIndices = friendList.getSelectedIndices();
		//TODO: Handle adding missing users
		
		if (selectedIndices.length != 0) {
			Collection<F2FPeer> addedFriends = new ArrayList<F2FPeer>();
			for (int index : selectedIndices) {
				addedFriends.add(friendModel.getElementAt(index));
			}

			owner.addMembers(addedFriends);
		}
		this.dispose();
	}

	private void onCancel() {
		this.dispose();
	}
}
