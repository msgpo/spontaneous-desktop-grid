/**
 * 
 */
package ee.ut.f2f.ui.log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * @author olegus
 *
 */
public class LogHandler extends Handler {
	
	private static LogHandler instance;
	
	public static LogHandler getInstance()
	{
		if (instance == null)
		{
			synchronized(LogHandler.class)
			{
				if (instance == null)
				{
					Logger rootLogger = (Logger.global).getParent();
					Handler handlers[] = rootLogger.getHandlers();
					for(Handler handler: handlers)
					{
						if(handler instanceof LogHandler)
							return (LogHandler) handler;
					}
					
					instance = new LogHandler();
				}
			}
		}
		return instance;
	}
	
	LogHandler() {
	}
	
	private LogTableModel tableModel;
	private List<LogRecord> logs = new ArrayList<LogRecord>();

	/* (non-Javadoc)
	 * @see java.util.logging.Handler#close()
	 */
	@Override
	public void close() throws SecurityException {
		logs = null;
	}

	/* (non-Javadoc)
	 * @see java.util.logging.Handler#flush()
	 */
	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
	 */
	@Override
	public void publish(LogRecord record) {
		if(record.getLevel().intValue() < getLevel().intValue())
			return;
		synchronized (logs) {
			logs.add(record);			
			if(tableModel!=null)
				tableModel.fireTableRowsInserted(logs.size()-1, logs.size()-1);
		}
	}

	public void setTableModel(LogTableModel tableModel) {
		this.tableModel = tableModel;
		tableModel.setLogHandler(this);
		tableModel.fireTableDataChanged();
	}

	public List<LogRecord> getLogs() {
		return logs;
	}
}
