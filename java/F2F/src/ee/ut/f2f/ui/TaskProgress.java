package ee.ut.f2f.ui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import ee.ut.f2f.core.Task;

@SuppressWarnings("serial")
class TaskProgress extends JProgressBar implements TableCellRenderer
{

	private Task task;
	private TasksTableModel tableModel;
	
	TaskProgress(TasksTableModel tasksTableModel)
	{
		super(0, 100);
		setValue(0);
		setStringPainted(true);
		this.tableModel = tasksTableModel;
	}
	
	TaskProgress(Task task, TasksTableModel tasksTableModel)
	{
		this(tasksTableModel);
		this.task = task;
	}

	public Component getTableCellRendererComponent(
			JTable table, Object color,
            boolean isSelected, boolean hasFocus,
            int row, int column)
	{
		TaskProgress ret = (TaskProgress)tableModel.getValueAt(row, column);
		if (ret.task.getProgress() < 0)
		{
			return new JLabel(ret.task.getState() == Thread.State.TERMINATED? "stopped":"running"); 
		}
		int value = ret.task.getProgress();
		if (value > 100) value = 100;
		ret.setValue(ret.task.getProgress());
		if (ret.task.getState() == Thread.State.TERMINATED)
			ret.setString(value+"% (stopped)");
		return ret;
	}
}
