package ee.ut.f2f.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;

import ee.ut.f2f.core.F2FPeer;

public class PeopleChooser extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JList people;
	private JButton okButton;
	private JButton cancelButton;
	private GroupChatWindow owner;
	
	public PeopleChooser(Vector<F2FPeer> list, GroupChatWindow owner) {
		this.owner = owner;
		people = new JList(list);
		
		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOk();
			}
		});
		
		cancelButton = new JButton("Bug off");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});
		
		this.add(people);
		this.add(okButton);
		this.add(cancelButton);
	}
	
	private void onOk() {
		int[] selected = people.getSelectedIndices();
		
		if(selected.length == 0) {
			return;
		}
		
		owner.onPeopleAdd(people.getSelectedValues());
		this.dispose();
	}
	
	private void onCancel() {
		this.dispose();
	}
}
