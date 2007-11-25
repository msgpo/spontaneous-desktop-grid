/**
 * 
 */
package ee.ut.f2f.ui.model;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * @author olegus
 *
 */
public class ActivityInfoSelectionListener implements TreeSelectionListener {

	private ActivityInfoTableModel tableModel;
	
	public ActivityInfoSelectionListener(ActivityInfoTableModel tableModel) {
		this.tableModel = tableModel;
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent e) {
		TreePath paths[] = e.getPaths();
		for(TreePath path: paths) {
			Object row = path.getLastPathComponent();
			tableModel.removeNewRow(row);
		}
	}

}
