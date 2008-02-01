package ee.ut.f2f.util;

import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ee.ut.f2f.util.logging.Logger;

public class F2FDebug
{
	private static final Logger logger = Logger.getLogger(F2FDebug.class);
	
	private static F2FDebug debug;
	
	private JFrame frame;
	private JPanel panel;
	private JTextArea console;

	private F2FDebug()
	{
		frame = new JFrame("F2F DEBUG WINDOW");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setSize(1000, 1000);

		panel = new JPanel();
		frame.setContentPane(panel);
		panel.setLayout(new GridLayout(1,1));
		panel.setSize(frame.getSize());

		console = new JTextArea();
		console.setEditable(false);
		JScrollPane consoleScroller = new JScrollPane(console);
		panel.add(consoleScroller);
		
		frame.setVisible(false);
	}
	
	private static F2FDebug getInstance()
	{
		if (debug == null)
		{
			synchronized (F2FDebug.class)
			{
				if (debug == null)
					debug = new F2FDebug();
			}
		}
		return debug;
	}

	public static synchronized void println(String msg)
	{
		logger.debug(msg);
		
		getInstance().console.append(msg);
		if (!msg.endsWith(String.valueOf('\n')))
		{
			getInstance().console.append(String.valueOf('\n'));
		}
		getInstance().console.setCaretPosition(getInstance().console.getText().length());
	}
	
	public static void show(boolean bShow)
	{
		getInstance().frame.setVisible(bShow);
		getInstance().frame.requestFocus();
	}
}
