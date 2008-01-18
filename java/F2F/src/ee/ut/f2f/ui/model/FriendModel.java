package ee.ut.f2f.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.swing.AbstractListModel;

import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.util.logging.Logger;

@SuppressWarnings("serial")
public class FriendModel extends AbstractListModel
{
	final private static Logger log = Logger.getLogger(FriendModel.class);
	
	private List<F2FPeer> friends = new ArrayList<F2FPeer>();

	public void add(F2FPeer friend) {
		if(!friends.contains(friend)){
			log.debug("Added new peer to F2F friend list [" + friend.getDisplayName() + "]");
			friends.add(friend);
			this.fireContentsChanged(this,0,friends.size());
		} else {
			log.debug("Friend [" + friend.getDisplayName() + "] allready exist in firends list, nothing added");
		}
	}

	public int getSize() {
		return friends.size();
	}

	public F2FPeer getElementAt(int index) {
		return friends.get(index);
	}
	
	public void remove(F2FPeer friend)
	{
		if(friends.remove(friend)){
			log.debug("Removed friend [" + friend.getDisplayName() + "] from F2F frieds list");
		} else {
			log.debug("Friend [" + friend.getDisplayName() + "] does not exist in friends list, nothing removed");
		}
		this.fireContentsChanged(this,0,friends.size());
	}
	public Collection<F2FPeer> getPeers() { return new HashSet<F2FPeer>(friends); }
	
	public F2FPeer getF2FPeerById(UUID id){
		for(F2FPeer f2fpeer : friends){
			if(f2fpeer.getID().equals(id)){
				return f2fpeer;
			}
		}
		return null;
	}
}
