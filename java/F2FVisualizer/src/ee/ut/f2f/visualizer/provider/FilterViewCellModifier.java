package ee.ut.f2f.visualizer.provider;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Item;

import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.FilterTableRow;
import ee.ut.f2f.visualizer.model.PropertyFilter;
import ee.ut.f2f.visualizer.view.FilterView;

/**
 * Provides filters table cell modifying capabilities.
 * 
 * @author Indrek Priks
 */
public class FilterViewCellModifier implements ICellModifier {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(FilterViewCellModifier.class);
	private Viewer viewer;
	
	/**
	 * Default constructor.
	 * 
	 * @param viewer
	 *          the viewer of FilterView view
	 */
	public FilterViewCellModifier(Viewer viewer) {
		this.viewer = viewer;
	}
	
	public boolean canModify(Object element, String property) {
		return true;
	}
	
	public Object getValue(Object element, String property) {
		log.debug("getValue " + property + " " + element);
		
		FilterTableRow row = (FilterTableRow) element;
		PropertyFilter key = row.getPropertyFilter();
		
		if (FilterView.COLUMNS[FilterView.COLUMN_ACTIVE_IDX].equals(property)) {
			return row.isActive();
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_FILTER_MODE_IDX].equals(property)) {
			return row.getFilterMode();
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_FILTER_TYPE_IDX].equals(property)) {
			return row.getFilterType();
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_MATCH_MODE_IDX].equals(property)) {
			return key.getMatchMode();
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_NAME_IDX].equals(property)) {
			return asNotNull(key.getKey());
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_VALUE_IDX].equals(property)) {
			return asNotNull(key.getValuePattern());
		}
		return "";
	}
	
	public void modify(Object element, String property, Object value) {
		log.debug("modify:element=" + element + ", property=" + property + ", value=" + value);
		if (element instanceof Item)
			element = ((Item) element).getData();
		
		FilterTableRow row = (FilterTableRow) element;
		PropertyFilter key = row.getPropertyFilter();
		
		if (FilterView.COLUMNS[FilterView.COLUMN_ACTIVE_IDX].equals(property)) {
			row.setActive((Boolean) value);
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_FILTER_MODE_IDX].equals(property)) {
			row.setFilterMode((Boolean) value);
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_FILTER_TYPE_IDX].equals(property)) {
			row.setFilterType((Boolean) value);
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_MATCH_MODE_IDX].equals(property)) {
			key.setMatchMode((Integer) value);
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_NAME_IDX].equals(property)) {
			key.setKey((String) value);
		}
		else if (FilterView.COLUMNS[FilterView.COLUMN_VALUE_IDX].equals(property)) {
			key.setValuePattern((String) value);
		}
		
		viewer.refresh();
	}
	
	private String asNotNull(String s) {
		return s == null ? "" : s;
	}
	
}
