package ee.ut.f2f.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.core.Job;
import ee.ut.f2f.ui.model.FriendModel;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.F2FTests;

public class UIController{
	private JFrame frame = null;
	private JMenuBar generalMenuBar = null;
	private JPanel mainPanel = null;
	private JMenu fileMenu = null;
	private JMenu viewMenu = null;
	private JMenuItem exitMenuItem = null;
	private JCheckBoxMenuItem showDebugMenuItem = null;
	private JCheckBoxMenuItem showInfoMenuItem = null;
	private JCheckBoxMenuItem showErrorMenuItem = null;
	private JCheckBoxMenuItem autoScrollMenuItem = null;
	private JMenu helpMenu = null;
	private JPanel friendsPanel = null;
	private JList friendsList = null;
	private FriendModel friendModel = null;
	private JTextArea console = null;
	private JTextField tf1 = null;
	private JTextField tf2 = null;
	
	private JTextArea receievedMessagesTextArea = null;
	private JTextArea sendMessageTextArea = null;
	private JPanel messagingPanel = null;
	private JButton sendMessageButton = null;
	private File[] selectedFiles = null;
	
	private boolean showDebug = true;
	private boolean showInfo = true;
	private boolean showError = true;
	private boolean autoScroll = true;
	
	/**
	 *  The peers from whom the computation peers will be selected.
	 */
	private Collection<Peer> selectFromPeers = new ArrayList<Peer>();
	
	public UIController(String title)
	{
		frame = new JFrame("F2FComputing - " + title);
		SpringLayout layout = new SpringLayout();
		mainPanel = new JPanel(layout);
		frame.setContentPane(mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 700);

		generalMenuBar = new JMenuBar();
		frame.setJMenuBar(generalMenuBar);
		mainPanel.setSize(frame.getSize());

		fileMenu = new JMenu("File");
		exitMenuItem = new JMenuItem("Exit");
		viewMenu = new JMenu("View");
		showDebugMenuItem = new JCheckBoxMenuItem("Show debug messages", true);
		showInfoMenuItem = new JCheckBoxMenuItem("Show info messages", true);
		showErrorMenuItem = new JCheckBoxMenuItem("Show error messages", true);
		autoScrollMenuItem = new JCheckBoxMenuItem("AutoScroll console", true);
		viewMenu.add(showDebugMenuItem);
		viewMenu.add(showInfoMenuItem);
		viewMenu.add(showErrorMenuItem);
		viewMenu.addSeparator();
		viewMenu.add(autoScrollMenuItem);
		fileMenu.add(exitMenuItem);
		
		helpMenu = new JMenu("Help");

		generalMenuBar.add(fileMenu);
		generalMenuBar.add(viewMenu);
		generalMenuBar.add(helpMenu);

		friendsPanel = new JPanel();
		friendsPanel.setLayout(new GridLayout(1,1));
		friendsPanel.setBorder(BorderFactory.createTitledBorder("Friends"));
		friendsPanel.setPreferredSize(new Dimension(200, 300+200));
		layout.putConstraint(SpringLayout.WEST, friendsPanel, 5, SpringLayout.WEST, mainPanel);
		layout.putConstraint(SpringLayout.NORTH, friendsPanel, 5, SpringLayout.NORTH, mainPanel);
		mainPanel.add(friendsPanel);

		friendModel = new FriendModel();
		friendsList = new JList(friendModel);
		friendsList.addListSelectionListener(new FriendsListListener());

		friendsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		friendsList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(friendsList);
		friendsPanel.add(listScroller);

		JPanel consolePanel = new JPanel();
		consolePanel.setLayout(new GridLayout(1,1));
		consolePanel.setBorder(BorderFactory.createTitledBorder("Information"));
		consolePanel.setPreferredSize(new Dimension(570, 300+200));
		layout.putConstraint(SpringLayout.WEST, consolePanel, 5, SpringLayout.EAST, friendsPanel);
		layout.putConstraint(SpringLayout.NORTH, consolePanel, 0, SpringLayout.NORTH, friendsPanel);
		mainPanel.add(consolePanel);

		console = new JTextArea();
		JScrollPane consoleScroller = new JScrollPane(console);
		consolePanel.add(consoleScroller);
		
		// Messaging panel elements.
		messagingPanel = new JPanel();
		messagingPanel.setLayout(new GridLayout(3,1));
		messagingPanel.setBorder(BorderFactory.createTitledBorder("Messaging"));
		// Currently, will not add the messaging panel, as its usage has become obselete.
		//messagingPanel.setPreferredSize(new Dimension(770, 200));
		messagingPanel.setPreferredSize(new Dimension(770, 0));
		// ===
		layout.putConstraint(SpringLayout.NORTH, messagingPanel, 5, SpringLayout.SOUTH, friendsPanel);
		layout.putConstraint(SpringLayout.WEST, messagingPanel, 5, SpringLayout.WEST, mainPanel);
		
		receievedMessagesTextArea = new JTextArea();
		receievedMessagesTextArea.setEditable(false);
		
		JScrollPane receievedMessagesTextAreaScrollPane = new JScrollPane(receievedMessagesTextArea); 
		sendMessageTextArea = new JTextArea();
		JScrollPane messagingPanelScrollPanel = new JScrollPane(sendMessageTextArea);
		
		sendMessageButton = new JButton("Send");
		sendMessageButton.setPreferredSize(new Dimension(sendMessageButton.getPreferredSize().width, 10));
		sendMessageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				// Get selected peers and send the message to them.
				for (Peer peer : getSelectedFriends())
				{
					try
					{
						peer.sendMessage(sendMessageTextArea.getText());
					} catch (CommunicationFailedException cfe)
					{
						error("Sending message '"
								+ sendMessageTextArea.getText()
								+ "' to the peer '" + peer.getDisplayName()
								+ "' failed with '" + cfe.getMessage() + "'");
					}					
				}
			}
		});
		
		messagingPanel.add(receievedMessagesTextAreaScrollPane);		
		messagingPanel.add(messagingPanelScrollPanel);
		messagingPanel.add(sendMessageButton);
		
		// Currently, will not add the messaging panel, as its usage has become obselete.
		//mainPanel.add(messagingPanel);

		JLabel label1 = new JLabel("Choose file:");
		layout.putConstraint(SpringLayout.NORTH, label1, 5, SpringLayout.SOUTH, messagingPanel);
		layout.putConstraint(SpringLayout.WEST, label1, 0, SpringLayout.WEST, messagingPanel);
		mainPanel.add(label1);

		tf1 = new JTextField();
		tf1.setColumns(40);
		layout.putConstraint(SpringLayout.NORTH, tf1, 5, SpringLayout.SOUTH, label1);
		layout.putConstraint(SpringLayout.WEST, tf1, 0, SpringLayout.WEST, label1);
		mainPanel.add(tf1);

		JButton button1 = new JButton("Browse...");
		layout.putConstraint(SpringLayout.NORTH, button1, 0, SpringLayout.NORTH, tf1);
		layout.putConstraint(SpringLayout.WEST, button1, 5, SpringLayout.EAST, tf1);
		mainPanel.add(button1);
		
		// Main task will be filled from the manifest file's entry.
		tf2 = new JTextField("");
		button1.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final JFileChooser fc = new JFileChooser(selectedFiles == null || selectedFiles.length == 0 ? null : selectedFiles[0]);
				fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
				FileFilter filter = new JarFilter();
				fc.setFileFilter(filter);
				fc.setMultiSelectionEnabled(true);
				int returnVal = fc.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					File[] files = fc.getSelectedFiles();
					if (files.length == 0) return;
           			selectedFiles = files;
					
           			tf2.setText("");
					// this string will show names of seleced files as semicolon-separated list
					String sFiles = "";
           			// try to find master class
					boolean foundMasterClass = false;
           			for (File file: selectedFiles)
           			{
           				// add the name of the file to sFiles
	           			if (sFiles.length() > 0) sFiles += ";";
           				sFiles += file.getAbsolutePath();
           				
           				// If it is a .jar file, scan it for main class manifest file entry.
	           			if (!foundMasterClass && file != null && file.isFile() && file.canRead() && file.getName().indexOf(".jar") != -1)
	           			{
	           				try
	           				{
	           					JarFile jarFile = new JarFile(file);
	           					Manifest man = jarFile.getManifest();
	           					Attributes mainAttributes = man.getMainAttributes();
	           					String masterClass = mainAttributes.getValue("F2F-MasterTask");
	           					// Close the file.
	           					jarFile.close();
	        					if (masterClass != null && masterClass.length() > 0)
	        					{
	        						tf2.setText(masterClass);
	        						foundMasterClass = true;
	        					}
	           				}
	           				catch (IOException ioe)
	           				{
	           					// Not a .jar file - just continue.
	           				}
	           			}
           			}
					tf1.setText(sFiles);
        		}
			}
		});
		
		tf2.setColumns(40);
		layout.putConstraint(SpringLayout.NORTH, tf2, 5, SpringLayout.SOUTH, tf1);
		layout.putConstraint(SpringLayout.WEST, tf2, 0, SpringLayout.WEST, tf1);
		mainPanel.add(tf2);
		

		JButton button2 = new JButton("Compute");
		layout.putConstraint(SpringLayout.NORTH, button2, 5, SpringLayout.SOUTH, tf2);
		layout.putConstraint(SpringLayout.WEST, button2, 0, SpringLayout.WEST, tf2);
		mainPanel.add(button2);
		button2.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (tf1.getText().length() == 0)
				{
					error("no jar-files name was specified");
				}
				else if (tf2.getText().length() == 0)
				{
					error("master task name was not specified");
				}
				else
				{
					Collection<String> jarFilesNames = new ArrayList<String>();
					StringTokenizer tokenizer = new StringTokenizer(tf1.getText(), ";", false);
					while (tokenizer.hasMoreTokens()) jarFilesNames.add(tokenizer.nextToken().trim());
					//for (File file: selectedFiles) jarFiles.add(new F2FJarFile(file.getAbsolutePath()));
					String jobID;
					try {
						jobID = F2FComputing.createJob(jarFilesNames, tf2.getText(), getSelectedFriends()).getJobID();
						info("Started job with ID: " + jobID);
					} catch (F2FComputingException ex) {
						error("Error with starting a job! " + ex);
					}
				}
			}
		});
		
		JButton button3 = new JButton("Show stats");
		layout.putConstraint(SpringLayout.NORTH, button3, 0, SpringLayout.NORTH, button2);
		layout.putConstraint(SpringLayout.WEST, button3, 10, SpringLayout.EAST, button2);
		mainPanel.add(button3);
		button3.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Collection<Job> jobs = F2FComputing.getJobs();
				Iterator<Job> jobIterator = jobs.iterator();
				while (jobIterator.hasNext())
				{
					Job job = jobIterator.next();
					info(job.getJobID());
					Collection<Task> tasks = job.getTasks();
					Iterator<Task> taskIterator = tasks.iterator();
					while (taskIterator.hasNext())
					{
						Task task = taskIterator.next();
						info("\tTask " + task.getTaskID());
						info("\t\tstate: java.lang.Thread.State." + task.getState());
						Collection<TaskProxy> proxies = task.getTaskProxies();
						Iterator<TaskProxy> proxyIterator = proxies.iterator();
						while (proxyIterator.hasNext())
						{
							TaskProxy proxy = proxyIterator.next();
							info("\t\tTask " + proxy.getRemoteTaskID() + " message queue size: " + proxy.getMessageCount());
						}
					}
				}
			}
		});
		
		JButton buttonDebug = new JButton("Open Debug window");
		layout.putConstraint(SpringLayout.NORTH, buttonDebug, 0, SpringLayout.NORTH, button3);
		layout.putConstraint(SpringLayout.WEST, buttonDebug, 10, SpringLayout.EAST, button3);
		mainPanel.add(buttonDebug);
		buttonDebug.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				F2FDebug.show(true);
			}
		});

		// Some room for playing and testing
		JButton buttonTest = new JButton("Trigger tests");
		layout.putConstraint(SpringLayout.NORTH, buttonTest, 0, SpringLayout.NORTH, buttonDebug);
		layout.putConstraint(SpringLayout.WEST, buttonTest, 10, SpringLayout.EAST, buttonDebug);
		mainPanel.add(buttonTest);
		
		buttonTest.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				F2FTests.doTests();
			}
		});

		//frame.pack();
		frame.setVisible(true);
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				System.exit(0);
			}
		});
		showDebugMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showDebug = showDebugMenuItem.isSelected();
			}
		});
		showInfoMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showInfo = showInfoMenuItem.isSelected();
			}
		});
		showErrorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showError = showErrorMenuItem.isSelected();
			}
		});
		autoScrollMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoScroll = autoScrollMenuItem.isSelected();
				if (!autoScroll) {
					console.setCaretPosition(console.getText().length()-1);
				}
			}
		});
	}

	public FriendModel getFriendModel() {
		return friendModel;
	}

	/**
	 * Prints info message to the console window
	 * @param msg
	 */
	public void info(String msg) {
		if (showInfo) {
			console("INFO", msg);
		}
	}

	/**
	 * Prints error message to the console window
	 * @param msg
	 */
	public void error(String msg) {
		if (showError) {
			console("ERROR", msg);
		}
	}
	
	/**
	 * Prints debug message to the console window
	 * @param msg
	 */
	public void debug(String msg) {
		if (showDebug) {
			console("DEBUG", msg);
		}
	}
	
	private void console(String prefix, String msg) {
		console.append(prefix+": "+msg);
		if (!msg.endsWith(String.valueOf('\n'))) {
			console.append(String.valueOf('\n'));
		}
		if (autoScroll) {
			console.setCaretPosition(console.getText().length());
		}
	}
	
	/**
	 * @return the collection of peers selected from the friends list.
	 */
	Collection<Peer> getSelectedFriends()
	{
		Collection<Peer> selectedPeers = new ArrayList<Peer>();
		for (int i : friendsList.getSelectedIndices())
		{
			selectedPeers.add(friendModel.getElementAt(i));
		}
		return selectedPeers;
	}

	/**
	 * Listener for {@link #friendsPanel}.
	 */
	private class FriendsListListener implements ListSelectionListener
	{
		/**
		 * Update the {@link #selectFromPeers}.
		 */
		public void valueChanged(ListSelectionEvent listSelectionEvent)
		{
		
			// Return if this is one of multiple change events.
			if (listSelectionEvent.getValueIsAdjusting())
				return;
			
			// Process the event
			selectFromPeers.clear();
			selectFromPeers.addAll(getSelectedFriends());
		}
	} // private class FriendsListListener
	
	private class JarFilter extends FileFilter {

		public boolean accept(File f) {
			if (f.isDirectory()) {
	            return true;
	        }
			int dotPos = f.getName().lastIndexOf(".");
			if (dotPos > 0) {
				String ext = f.getName().substring(dotPos);
		        if (ext.equals(".jar")) {
		        	return true;
		        } else {
		        	return false;
		        }
	        } else {
	        	return false;
	        }
		}

		public String getDescription() {
			return "Jar file (.jar)";
		}

	}
}