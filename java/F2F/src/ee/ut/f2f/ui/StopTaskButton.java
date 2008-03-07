package ee.ut.f2f.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import ee.ut.f2f.core.Task;
	
@SuppressWarnings("serial")
public class StopTaskButton extends JButton implements ActionListener, TableCellRenderer
{
	private Task task;
	private TasksTableModel tableModel;
	StopTaskButton(Task task, TasksTableModel model)
	{
		super("Stop");
		this.task = task;
		this.tableModel = model;
		addActionListener(this);
	}
	
	StopTaskButton(TasksTableModel model)
	{
		super("Stop");
		this.tableModel = model;
		addActionListener(this);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		System.out.println("event at "+(task == null? "NULL": task.getTaskID()));
		if (task != null)
		{
			task.stopTask();
		}
		setVisible(false);
	}

	public Component getTableCellRendererComponent(
			JTable table, Object color,
            boolean isSelected, boolean hasFocus,
            int row, int column)
	{
		Component ret = (StopTaskButton)tableModel.getValueAt(row, column);
		if (ret == null) ret = new JPanel();
		return ret;
	}
	
	TableCellEditor editor = new StopCellEditor();
	
	private class StopCellEditor extends AbstractCellEditor implements TableCellEditor
	{
		public Component getTableCellEditorComponent(
				JTable table, Object value, boolean isSelected,
				int row, int column)
		{
			Component ret = (StopTaskButton)tableModel.getValueAt(row, column);
			if (ret == null) ret = new JPanel();
			return ret;
		}
		
		public Object getCellEditorValue()
		{
			return null;
		}
	}
}