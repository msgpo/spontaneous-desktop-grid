/**
 * 
 */
package ee.ut.f2f.ui.model;

import java.awt.Component;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

/**
 * @author olegus
 *
 */
public class ActivityInfoNewRowsPredicate implements HighlightPredicate {

	private ActivityInfoTableModel tableModel;
	
	public ActivityInfoNewRowsPredicate(ActivityInfoTableModel tableModel) {
		this.tableModel = tableModel;
	}

	/* (non-Javadoc)
	 * @see org.jdesktop.swingx.decorator.HighlightPredicate#isHighlighted(java.awt.Component, org.jdesktop.swingx.decorator.ComponentAdapter)
	 */
	public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
		JXTreeTable table = (JXTreeTable) adapter.getComponent();
		Object obj = table.getPathForRow(adapter.row).getLastPathComponent();
		return tableModel.isNewRow(obj);
	}
}
