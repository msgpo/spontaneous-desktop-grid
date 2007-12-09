package ee.ut.f2f.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.model.FriendModel;

/**
 * @author Andres A.
 * @author Jaan Neljandik
 */
public class PeopleChooser extends JDialog {
	private static final long serialVersionUID = 1L;
	private FriendModel friendModel;
	private JList friendList;
	private JButton okButton;
	private JButton cancelButton;
	private GroupChatWindow owner;

	public PeopleChooser(Collection<F2FPeer> friends, GroupChatWindow owner) {
		this.owner = owner;
		this.setSize(new Dimension(150, 210));
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setTitle("Add");
		this.setAlwaysOnTop(true);
		this.setModal(true);
		
		friendModel = new FriendModel();
		friendList = new JList(friendModel);
		for (F2FPeer friend : friends) {
			friendModel.add(friend);
		}
		JScrollPane listScroller = new JScrollPane(friendList);
		listScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		

		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOk();
			}
		});

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL; 
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2; 
		c.gridheight = 8;
		c.weightx = 1.0;
		c.weighty = 1.0;
		contentPanel.add(listScroller, c);

		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 8;

		contentPanel.add(okButton, c);

		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 8;

		contentPanel.add(cancelButton, c);
		
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
