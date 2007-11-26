/**
 * 
 */
package ee.ut.f2f.ui.log;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JTable;

import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

/**
 * @author olegus
 *
 */
public class LogHighlighter extends AbstractHighlighter {

	/* (non-Javadoc)
	 * @see org.jdesktop.swingx.decorator.AbstractHighlighter#doHighlight(java.awt.Component, org.jdesktop.swingx.decorator.ComponentAdapter)
	 */
	@Override
	protected Component doHighlight(Component component,
			ComponentAdapter adapter) {
		LogTableModel tableModel = (LogTableModel) ((JTable)adapter.getComponent()).getModel();
		LogRecord log = tableModel.getRow(adapter.row);
		
		int logLevel = log.getLevel().intValue();
		if(logLevel >= Level.SEVERE.intValue())
			component.setForeground(Color.RED);
		else if(logLevel >= Level.WARNING.intValue())
			component.setForeground(Color.ORANGE);
		else if(logLevel >= Level.INFO.intValue())
			component.setForeground(Color.BLACK);
		else
			component.setForeground(Color.GRAY);
			
		return component;
	}

}
