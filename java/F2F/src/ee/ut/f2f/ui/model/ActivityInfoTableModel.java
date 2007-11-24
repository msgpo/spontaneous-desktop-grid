/**
 * 
 */
package ee.ut.f2f.ui.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	private List<Activity> activities = new ArrayList<Activity>();
	private Map<Activity, ActivityEvent> activityLastEvents = new HashMap<Activity, ActivityEvent>(); 
	
	private static final String[] columnNames = new String[]{
		"Activity name", "Last event", "Time", "Event description"
	};
	
	public ActivityInfoTableModel() {
		super(new Object()); // dummy root
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return 4;
	}
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}	

	public void activityEvent(ActivityEvent event) {
		if(!activityLastEvents.containsKey(event.getActivity())) {
			activities.add(event.getActivity());
			activityLastEvents.put(event.getActivity(), event);
			modelSupport.fireChildAdded(new TreePath(root),
					activities.size() - 1, event.getActivity());
		} else {
			activityLastEvents.put(event.getActivity(), event);
			modelSupport.fireChildChanged(new TreePath(root), activities
					.indexOf(event.getActivity()), event.getActivity());			
		}
	}

	public Object getValueAt(Object obj, int column) {
		if(obj==root)
			return null;
		
		Activity activity = (Activity) obj;
		ActivityEvent event = activityLastEvents.get(activity);
		
		switch (column) {
		case 0:
			return activity.getActivityName();
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

	public Object getChild(Object parent, int index) {
		if(parent == root) {
			return activities.get(index);
		}
		
		return null;
	}

	public int getChildCount(Object parent) {
		if(parent == root) {
			return activities.size();
		}
		
		return 0;
	}

	public int getIndexOfChild(Object parent, Object child) {
		return activities.indexOf(child);
	}
}
