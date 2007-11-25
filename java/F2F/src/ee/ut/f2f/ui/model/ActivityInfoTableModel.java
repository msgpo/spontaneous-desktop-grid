/**
 * 
 */
package ee.ut.f2f.ui.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityListener;

/**
 * @author olegus
 *
 */
@SuppressWarnings("serial")
public class ActivityInfoTableModel extends AbstractTreeTableModel implements ActivityListener {
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/** Top level (root) activities */
	private List<Object> activities = new ArrayList<Object>();
	/** Children (activities and events) of all activities. */
	private Map<Activity, List<Object>> childrenMap = new HashMap<Activity, List<Object>>();
	
	/** Recently created objects in this table. */
	private Set<Object> newRows = new HashSet<Object>();
	
	private static final String[] columnNames = new String[]{
		"Activity name", "Event type", "Event time", "Event description"
	};
	
	public ActivityInfoTableModel() {
		super(new Object()); // dummy root
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return columnNames.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}	

	/**
	 * Called by activity manager when new activity event is trigerred. Adds
	 * event to local storage and requests table row updates. The rows that need
	 * to be updated include parent rows.
	 */
	public void activityEvent(ActivityEvent event) {
		Activity activity = event.getActivity();
		Activity parentActivity = activity.getParentActivity();
		TreePath parentPath = constructTreePath(parentActivity);
		List<Object> parentChildren;
		if(parentActivity == null) {
			parentChildren = this.activities;
		} else {
			parentChildren = childrenMap.get(parentActivity);
		}
		
		List<Object> children;
		// if it is new activity initialize its children map entry
		// and add it to parent children
		if(childrenMap.containsKey(activity)) {
			children = childrenMap.get(activity);			
		} else {
			children = new ArrayList<Object>();
			childrenMap.put(activity, children);
			parentChildren.add(activity);
			
			modelSupport.fireChildAdded(parentPath,
					parentChildren.size() - 1, event);			
		}
		
		// now add event
		children.add(event);
		newRows.add(event);
		modelSupport.fireChildAdded(constructTreePath(activity),
				children.size() - 1, event);
		// this is needed because activity shows its latest event info in the columns
		newRows.add(activity);
		modelSupport.fireChildChanged(parentPath, 
				parentChildren.indexOf(activity), activity);
	}

	private TreePath constructTreePath(Object row) {
		if(row == null)
			return new TreePath(root);
		
		List<Object> parents = new LinkedList<Object>();
		while(row!=null) {
			parents.add(0, row);
			if(row instanceof Activity)
				row = ((Activity)row).getParentActivity();
			else if (row instanceof ActivityEvent)
				row = ((ActivityEvent)row).getActivity();
		}
		parents.add(0, root);
		return new TreePath(parents.toArray());
	}

	/* (non-Javadoc)
	 * @see org.jdesktop.swingx.treetable.TreeTableModel#getValueAt(java.lang.Object, int)
	 */
	public Object getValueAt(Object obj, int column) {
		if(obj==root)
			return null;
		
		if(obj instanceof ActivityEvent) {
			return getValueAt((ActivityEvent)obj, column);
		}
		
		Activity activity = (Activity) obj;
		if(column == 0)
			return activity.getActivityName();
		
		List<Object> children = childrenMap.get(activity);
		ActivityEvent event = findLatestEvent(children);
		return getValueAt(event, column);
	}
		
	private ActivityEvent findLatestEvent(List<Object> children) {
		for(int i=children.size()-1; i>=0; i--) {
			if(children.get(i) instanceof ActivityEvent)
				return (ActivityEvent) children.get(i);
		}
		
		return null;
	}

	private Object getValueAt(ActivityEvent event, int column) {
		switch (column) {
		case 0:
			return "(event)";
		case 1:
			return event.getType().name();
		case 2:
			return ActivityInfoTableModel.dateFormat.format(new Date(event.getTime()));
		case 3:
			return event.getDescription();
		default:
			throw new RuntimeException("Invalid column index "+column);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	public Object getChild(Object parent, int index) {
		if(parent == root) {
			return activities.get(index);
		}
		
		Activity activity = (Activity) parent;
		return childrenMap.get(activity).get(index);
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	public int getChildCount(Object parent) {
		if(parent == root) {
			return activities.size();
		}
		
		if(!(parent instanceof Activity))
			return 0;
		
		Activity activity = (Activity) parent;
		return childrenMap.get(activity).size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
	 */
	public int getIndexOfChild(Object parent, Object child) {
		if(parent == root) {
			return activities.indexOf(child);
		}

		Activity activity = (Activity) parent;
		return childrenMap.get(activity).indexOf(child);	
	}

	public boolean isNewRow(Object value) {
		return newRows.contains(value);
	}

	public void removeNewRow(Object row) {
		boolean removed = newRows.remove(row);
		if(removed) {
			modelSupport.firePathChanged(constructTreePath(row));
		}
	}
}
