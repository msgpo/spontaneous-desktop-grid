/**
 * 
 */
package ee.ut.f2f.ui;

import javax.swing.table.AbstractTableModel;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.Job;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskListener;

@SuppressWarnings("serial")
public class TasksTableModel extends AbstractTableModel implements TaskListener
{
	private static final String[] names = {
		"Job ID",
		"Taks ID",
		"Status",
		""
	};
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return names.length;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount()
	{
		if (F2FComputing.getJobs() == null) return 0;
		int res = 0;
		for (Job job: F2FComputing.getJobs())
			res += job.getTasks().size();
		return res;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Task task = null;
		int c = 0;
		findTask: for (Job job: F2FComputing.getJobs())
			for (Task t: job.getTasks())
			{
				if (c == rowIndex)
				{
					task = t;
					break findTask;
				}
				c++;
			}
		if (task == null) return null;
		switch(columnIndex) {
		case 0: return task.getJob().getJobID();
		case 1: return task.getTaskID();
		case 2: return task.getState() == Thread.State.TERMINATED ? "stopped" : "running";
		case 3: return task.bStopFlag ? null : new StopTaskButton(task, this);
		}
		return null;
	}

	@Override
	public String getColumnName(int column) {
		return names[column];
	}
	
	public boolean isCellEditable(int row, int col)
	{
        if (col < 3) return false;
        else return true;
    }
	
	@SuppressWarnings("unchecked")
	public Class getColumnClass(int c)
	{
		if (c < 3) return String.class;
		return StopTaskButton.class;
    }

	public void taskStarted(Task task)
	{
		fireTableDataChanged();
	}

	public void taskStopped(Task task)
	{
		fireTableDataChanged();
	}
}
