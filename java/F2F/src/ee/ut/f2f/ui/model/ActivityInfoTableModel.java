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

import javax.swing.table.AbstractTableModel;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityListener;

/**
 * @author olegus
 *
 */
@SuppressWarnings("serial")
public class ActivityInfoTableModel extends AbstractTableModel implements ActivityListener {
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private List<Activity> activities = new ArrayList<Activity>();
	private Map<Activity, ActivityEvent> activityLastEvents = new HashMap<Activity, ActivityEvent>(); 
	
	private static final String[] columnNames = new String[]{
		"Activity name", "Last event", "Time", "Event description"
	};
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return 4;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		return activities.size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int row, int column) {
		Activity activity = activities.get(row);
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
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}	

	public void activityEvent(ActivityEvent event) {
		if(!activityLastEvents.containsKey(event.getActivity())) {
			activities.add(event.getActivity());
		}
		activityLastEvents.put(event.getActivity(), event);
		fireTableDataChanged();		
	}
}
