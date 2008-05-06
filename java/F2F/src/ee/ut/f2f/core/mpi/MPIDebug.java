package ee.ut.f2f.core.mpi;

import java.awt.GridLayout;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A debugging util for MPI. If used then each process will have on window.
 */
public class MPIDebug {
	public static int START_UP = 30, SYSTEM = 40, DEFAULT = 50;
	private int debugLevel = DEFAULT;

	private JFrame frame;
	private JPanel panel;
	private JTextArea console;
	private StringBuffer line = new StringBuffer();
	boolean setVisible = true;

	MPIDebug(MPITask task) {
		frame = new JFrame("F2F DEBUG WINDOW: Task - " + task.getTaskID());
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setSize(1000, 1000);

		panel = new JPanel();
		frame.setContentPane(panel);
		panel.setLayout(new GridLayout(1, 1));
		panel.setSize(frame.getSize());

		console = new JTextArea();
		console.setEditable(false);
		JScrollPane consoleScroller = new JScrollPane(console);
		panel.add(consoleScroller);

		frame.setVisible(false);
	}

	public synchronized void println(String msg) {
		println(DEFAULT, msg);
	}

	public synchronized void print(String msg) {
		line.append(msg);
	}

	public synchronized void println(int level, String msg) {
		if (level >= getDebugLevel()) {
			if (setVisible) {
				setVisible = false;
				frame.setVisible(true);
			}
			try {
				console.append(level + " " + (new Date()) + " " + line.toString() + msg);
				line = new StringBuffer();
				if (!msg.endsWith(String.valueOf('\n'))) {
					console.append(String.valueOf('\n'));
				}
				console.setCaretPosition(console.getText().length());
			} catch (Exception e) {
			}
		}
	}

	public void show(boolean bShow) {
		setVisible = false;
		frame.setVisible(bShow);
		frame.requestFocus();
	}

	public int getDebugLevel() {
		return debugLevel;
	}

	public void setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
	}

	public void setName(String name) {
		frame.setTitle(name);
	}
}
