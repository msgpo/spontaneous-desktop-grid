/**
 * 
 */
package ee.ut.f2f.ui.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;

import javax.swing.table.AbstractTableModel;


/**
 * @author olegus
 *
 */
public class LogTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	
	private DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
	
	private static final String[] names = {
		"Time",
		"Thread",
		"Level",
		"Message"
	};

	private LogHandler logHandler;
	
	public void setLogHandler(LogHandler logHandler) {
		this.logHandler = logHandler;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return names.length;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		if(logHandler == null)
			return 0;
		return logHandler.getLogs().size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(logHandler == null)
			return null;
		LogRecord log = logHandler.getLogs().get(rowIndex);
		switch(columnIndex) {
		case 0: return dateFormat.format(new Date(log.getMillis()));
		case 1: return log.getThreadID();
		case 2: return log.getLevel();
		case 3: return log.getMessage();
		}
		return null;
	}

	@Override
	public String getColumnName(int column) {
		return names[column];
	}

	public LogRecord getRow(int row) {
		return logHandler.getLogs().get(row);
	}
}
