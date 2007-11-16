package ee.ut.f2f.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractListModel;

import ee.ut.f2f.core.F2FPeer;

@SuppressWarnings("serial")
public class FriendModel extends AbstractListModel
{
	private List<F2FPeer> friends = new ArrayList<F2FPeer>();

	public void add(F2FPeer friend) {
		friends.add(friend);
		this.fireContentsChanged(this,0,friends.size());
	}

	public int getSize() {
		return friends.size();
	}

	public F2FPeer getElementAt(int index) {
		return friends.get(index);
	}
	
	public void remove(F2FPeer friend)
	{
		friends.remove(friend);
		this.fireContentsChanged(this,0,friends.size());
	}
	public Collection<F2FPeer> getPeers() { return new HashSet<F2FPeer>(friends); }
}
