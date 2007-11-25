package ee.ut.f2f.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.jdesktop.swingx.JXTreeTable;

import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.Job;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.ui.model.ActivityInfoTableModel;
import ee.ut.f2f.ui.model.FriendModel;
import ee.ut.f2f.ui.model.StunInfoTableModel;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.F2FMessage;
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
	
	
	//NAT/Traversal panel
	private JPanel traversalPanel = null;
	private JTable stunInfoTable = null;
	private StunInfoTableModel stunInfoTableModel = null;
	private JTextArea natLogArea = null;
	
	
	private boolean showDebug = true;
	private boolean showInfo = true;
	private boolean showError = true;
	private boolean autoScroll = true;
	
	/**
	 *  The peers from whom the computation peers will be selected.
	 */
	private Collection<F2FPeer> selectFromPeers = new ArrayList<F2FPeer>();
	
	public UIController(String title)
	{		
		frame = new JFrame("F2FComputing - " + title);
		//SpringLayout layout = new SpringLayout();
		mainPanel = new JPanel(new BorderLayout());
		frame.setContentPane(mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 700);
		
		generalMenuBar = new JMenuBar();
		frame.setJMenuBar(generalMenuBar);
		mainPanel.setPreferredSize(frame.getSize());

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
		//layout.putConstraint(SpringLayout.WEST, friendsPanel, 5, SpringLayout.WEST, mainPanel);
		//layout.putConstraint(SpringLayout.NORTH, friendsPanel, 5, SpringLayout.NORTH, mainPanel);
		mainPanel.add(friendsPanel, BorderLayout.WEST);

		friendModel = new FriendModel();
		friendsList = new JList(friendModel);
		friendsList.addListSelectionListener(new FriendsListListener());

		friendsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		friendsList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(friendsList);
		friendsPanel.add(listScroller);

			
		JPanel consolePanel = new JPanel();
		consolePanel.setLayout(new GridLayout(1,1));
		//consolePanel.setBorder(BorderFactory.createEmptyBorder());
		consolePanel.setPreferredSize(new Dimension(570, 300+200));
		//layout.putConstraint(SpringLayout.WEST, consolePanel, 5, SpringLayout.EAST, friendsPanel);
		//layout.putConstraint(SpringLayout.NORTH, consolePanel, 0, SpringLayout.NORTH, friendsPanel);
		mainPanel.add(consolePanel);
		
		JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		console = new JTextArea();
		JScrollPane consoleScroller = new JScrollPane(console);

		tabs.addTab("Information", consoleScroller);
		consolePanel.add(tabs);
		
		// Messaging panel elements.
		messagingPanel = new JPanel();
		messagingPanel.setLayout(new BorderLayout());
		//messagingPanel.setBorder(BorderFactory.createTitledBorder("Messaging"));
		// Currently, will not add the messaging panel, as its usage has become obselete.
		//messagingPanel.setPreferredSize(new Dimension(770, 200));
		messagingPanel.setPreferredSize(new Dimension(770, 0));
		// ===
		//layout.putConstraint(SpringLayout.NORTH, messagingPanel, 5, SpringLayout.SOUTH, friendsPanel);
		//layout.putConstraint(SpringLayout.WEST, messagingPanel, 5, SpringLayout.WEST, mainPanel);
		
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
				F2FMessage msg = new F2FMessage(F2FMessage.Type.CHAT, null, null, null, sendMessageTextArea.getText());
				// get selected peers and send the message to them
				for (F2FPeer peer : getSelectedFriends())
				{
					try
					{
						peer.sendMessage(msg);
					}
					catch (CommunicationFailedException cfe)
					{
						error("Sending message '"
								+ sendMessageTextArea.getText()
								+ "' to the peer '" + peer.getDisplayName()
								+ "' failed with '" + cfe.getMessage() + "'");
					}					
				}
				writeMessage("me", sendMessageTextArea.getText());
			}
		});
		
		messagingPanel.add(receievedMessagesTextAreaScrollPane, BorderLayout.CENTER);		
		JPanel southPanel = new JPanel(new GridBagLayout());
		messagingPanel.add(southPanel, BorderLayout.SOUTH);
		
		GridBagConstraints c = new GridBagConstraints();
		
		//messagingPanelScrollPanel.setMinimumSize(new Dimension(300, 25));
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.8;
		c.weighty = 1.0;
		southPanel.add(messagingPanelScrollPanel, c);
		
		c.weightx = 0.2;
		southPanel.add(sendMessageButton, c);
		
		messagingPanelScrollPanel.setPreferredSize(new Dimension(300, 20));
		sendMessageButton.setPreferredSize(new Dimension(80, 20));
		
		// Currently, will not add the messaging panel, as its usage has become obselete.
		tabs.add("Chat", messagingPanel);

		//NAT/Traversal Panel
		
		traversalPanel = new JPanel();
		traversalPanel.setLayout(new GridLayout(3,1));
		traversalPanel.setPreferredSize(new Dimension(770, 0));
		
		stunInfoTableModel = new StunInfoTableModel();
			
		stunInfoTable = new JTable(stunInfoTableModel);
		stunInfoTable.setAutoscrolls(true);
		stunInfoTable.setEnabled(false);
		stunInfoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		for (int i = 0; i < stunInfoTableModel.getColumnCount(); i++)
			stunInfoTable.getColumnModel().getColumn(i).setPreferredWidth(StunInfoTableModel.widths[i]);
		
		JScrollPane stunInfoTableScrollPane = new JScrollPane(stunInfoTable);
		stunInfoTableScrollPane.setAutoscrolls(true);
		stunInfoTableScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "STUN Info"));
		
		
		//NAT Log
		natLogArea = new JTextArea();
		natLogArea.setEditable(false);
		
		JScrollPane natLogAreaScrollPane = new JScrollPane(natLogArea);
		natLogAreaScrollPane.setAutoscrolls(true);
		natLogAreaScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Traversal Log"));
		
		
		//Control Buttons
		
		/*
		JButton initButton = new JButton("TEST");
		initButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						
						F2FPeer to = (F2FPeer) friendsList.getSelectedValue();
						if (to != null) {
							String localId = F2FComputing.getLocalPeer().getID().toString();
							try {
								StunInfo sinf = ConnectionManager.getLocalStunInfo();
							} catch (ConnectionManagerException e1) {
								// TODO Auto-generated catch block
								F2FDebug.println(e1.getStackTrace().toString());
								e1.printStackTrace();
							} catch (NetworkDiscoveryException e1) {
								// TODO Auto-generated catch block
								F2FDebug.println(e1.getStackTrace().toString());
								e1.printStackTrace();
							}
							//NatMessage nmsg = new NatMessage(localId, to.getID().toString(),NatMessage.COMMAND_GET_STUN_INFO,null);
							//NatMessageProcessor.sendNatMessage(nmsg);
						}
					}
				}
		);
		*/
		
		JPanel natButtonPanel = new JPanel(new FlowLayout());
		//natButtonPanel.add(initButton);
	
		traversalPanel.add(stunInfoTableScrollPane);
		traversalPanel.add(natButtonPanel);
		traversalPanel.add(natLogAreaScrollPane);
		tabs.add("NAT Traversal", traversalPanel);
		
		//End of traversal panel
		
		// Activity model
		ActivityInfoTableModel activityInfoTableModel = new ActivityInfoTableModel();
		ActivityManager.getDefault().addListener(
				ActivityEvent.Type.values(), activityInfoTableModel);
		JTable activityInfoTable = new JXTreeTable(activityInfoTableModel);
		activityInfoTable.setAutoscrolls(true);
				
		tabs.add("F2F activities", new JScrollPane(activityInfoTable));
		
		// other
		SpringLayout bottomPanelLayout = new SpringLayout();
		JPanel bottomPanel = new JPanel(bottomPanelLayout);
		bottomPanel.setSize(new Dimension(300,100));
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
		
		JLabel label1 = new JLabel("Choose file:");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, label1, 5, SpringLayout.NORTH, bottomPanel);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, label1, 0, SpringLayout.WEST, bottomPanel);
		bottomPanel.add(label1);

		tf1 = new JTextField();
		tf1.setColumns(40);
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, tf1, 5, SpringLayout.SOUTH, label1);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, tf1, 0, SpringLayout.WEST, label1);
		bottomPanel.add(tf1);

		JButton button1 = new JButton("Browse...");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, button1, 0, SpringLayout.NORTH, tf1);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, button1, 5, SpringLayout.EAST, tf1);
		bottomPanel.add(button1);
		
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
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, tf2, 5, SpringLayout.SOUTH, tf1);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, tf2, 0, SpringLayout.WEST, tf1);
		bottomPanel.add(tf2);
		

		JButton button2 = new JButton("Compute");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, button2, 5, SpringLayout.SOUTH, tf2);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, button2, 0, SpringLayout.WEST, tf2);
		bottomPanel.add(button2);
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
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, button3, 0, SpringLayout.NORTH, button2);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, button3, 10, SpringLayout.EAST, button2);
		bottomPanel.add(button3);
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
						if (task.getException() != null)
							info("\t\texception: " + task.getException() + task.getException().getMessage());
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
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, buttonDebug, 0, SpringLayout.NORTH, button3);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, buttonDebug, 10, SpringLayout.EAST, button3);
		bottomPanel.add(buttonDebug);
		buttonDebug.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				F2FDebug.show(true);
			}
		});

		// Some room for playing and testing
		JButton buttonTest = new JButton("Trigger tests");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, buttonTest, 0, SpringLayout.NORTH, buttonDebug);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, buttonTest, 10, SpringLayout.EAST, buttonDebug);
		bottomPanel.add(buttonTest);
		
		// bottomPanel constraints (constraint SOUTH of the bottom panel to the last button)
		bottomPanelLayout.putConstraint(SpringLayout.SOUTH, bottomPanel, 5, SpringLayout.SOUTH, button2);		
		
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
	Collection<F2FPeer> getSelectedFriends()
	{
		Collection<F2FPeer> selectedPeers = new ArrayList<F2FPeer>();
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
	
	public void writeMessage(String from, String msg) {
		receievedMessagesTextArea.setText(receievedMessagesTextArea.getText()+"\n"+from+": "+msg);
	}
	
	public void writeNatLog(String msg){
		natLogArea.setText(natLogArea.getText() + "\n" + msg); 
	}

	public StunInfoTableModel getStunInfoTableModel() {
		return stunInfoTableModel;
	}

	public void setStunInfoTableModel(StunInfoTableModel stunInfoTableModel) {
		this.stunInfoTableModel = stunInfoTableModel;
	}
}
