package ee.ut.f2f.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractListModel;

import ee.ut.f2f.comm.Peer;

public class FriendModel extends AbstractListModel
{
	private static final long serialVersionUID = -3371463492464758776L;
	private List<Peer> friends = new ArrayList<Peer>();

	public void add(Peer friend) {
		friends.add(friend);
		this.fireContentsChanged(this,0,friends.size());
	}

	public int getSize() {
		return friends.size();
	}

	public Peer getElementAt(int index) {
		return friends.get(index);
	}
	
	public void remove(Peer friend)
	{
		friends.remove(friend);
		this.fireContentsChanged(this,0,friends.size());
	}
	public Collection<Peer> getPeers() { return new HashSet<Peer>(friends); }
}
