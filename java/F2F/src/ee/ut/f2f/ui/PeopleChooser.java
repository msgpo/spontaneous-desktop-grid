package ee.ut.f2f.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;

import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.model.FriendModel;

/**
 * @author Andres A.
 * @author Jaan Neljandik
 */
public class PeopleChooser extends JFrame {
	private static final long serialVersionUID = 1L;
	private FriendModel friendModel;
	private JList friendList;
	private JButton okButton;
	private JButton cancelButton;
	private GroupChatWindow owner;

	public PeopleChooser(Collection<F2FPeer> friends, GroupChatWindow owner) {
		this.owner = owner;
		System.out.println("Heihoo");
		friendModel = new FriendModel();
		friendList = new JList(friendModel);
		for (F2FPeer friend : friends) {
			friendModel.add(friend);
		}

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

		this.setSize(new Dimension(400, 100));
		JPanel panel = new JPanel();
		this.setContentPane(panel);
		panel.setLayout(new GridBagLayout());

		System.out.println("bakaa");
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 2;

		panel.add(friendList, c);

		c.gridheight = 1;
		c.weighty = 0.3;
		c.gridx = 1;
		c.gridy = 0;

		panel.add(okButton, c);

		c.gridheight = 1;
		c.weighty = 0.3;
		c.gridx = 1;
		c.gridy = 1;

		panel.add(cancelButton, c);
		this.setVisible(true);
		System.out.println("Jabba");
	}

	private void onOk() {
		int[] selectedIndices = friendList.getSelectedIndices();

		if (selectedIndices.length != 0) {
			Collection<F2FPeer> addedFriends = new ArrayList<F2FPeer>();
			for (int index : selectedIndices) {
				addedFriends.add(friendModel.getElementAt(index));
			}

			owner.addMembers(addedFriends, false);
		}
		this.dispose();
	}

	private void onCancel() {
		this.dispose();
	}
}
