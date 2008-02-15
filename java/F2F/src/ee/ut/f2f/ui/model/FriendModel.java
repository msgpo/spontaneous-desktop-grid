package ee.ut.f2f.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractListModel;

@SuppressWarnings("serial")
public class FriendModel<ElemType> extends AbstractListModel
{
	private List<ElemType> friends = new ArrayList<ElemType>();

	public void add(ElemType friend) {
		if(!friends.contains(friend)){
			//log.debug("Added new peer to F2F friend list [" + friend.getDisplayName() + "]");
			friends.add(friend);
			this.fireContentsChanged(this,0,friends.size());
		} else {
			//log.debug("Friend [" + friend.getDisplayName() + "] allready exist in firends list, nothing added");
		}
	}

	public int getSize() {
		return friends.size();
	}

	public ElemType getElementAt(int index) {
		return friends.get(index);
	}
	
	public void remove(ElemType friend)
	{
		if(friends.remove(friend)){
		//	log.debug("Removed friend [" + friend.getDisplayName() + "] from F2F frieds list");
		} else {
		//	log.debug("Friend [" + friend.getDisplayName() + "] does not exist in friends list, nothing removed");
		}
		this.fireContentsChanged(this,0,friends.size());
	}
	public Collection<ElemType> getPeers() { return new ArrayList<ElemType>(friends); }
}
