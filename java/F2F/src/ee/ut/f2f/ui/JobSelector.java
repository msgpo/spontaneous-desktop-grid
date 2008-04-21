package ee.ut.f2f.ui;

import java.awt.Dimension;
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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.filechooser.FileFilter;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.Job;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.ui.model.FriendModel;
import ee.ut.f2f.util.logging.Logger;

public class JobSelector extends JFrame
{
	private static final Logger logger = Logger.getLogger(JobSelector.class);
	private static final long serialVersionUID = 1L;
	
	private JTextField tf1 = null;
	private JTextField tf2 = null;
	private JPanel mainPanel = null;
	private File[] selectedFiles = null;
	private SpringLayout layout = null;
	private JButton btnCompute = null;
	
	private FriendModel<F2FPeer> members = null;
	Collection<F2FPeer> friends = null;//NB! do not remove peers from this collection, because this means they are removed from the chat also

	private JCheckBox btnSlave = null;
	
	private Collection<F2FPeer> getF2FPeers()
	{
		Collection<F2FPeer> tmpPeers = null;
		if (friends != null)
		{
			tmpPeers = new ArrayList<F2FPeer>(friends);
		}
		else tmpPeers = members.getPeers();
		
		Collection<F2FPeer> peers = new ArrayList<F2FPeer>();
		for (F2FPeer peer: tmpPeers)
			if (peer != null && !peers.contains(peer)) peers.add(peer);
		
		if (btnSlave != null && btnSlave.isSelected() && !peers.contains(F2FComputing.getLocalPeer()))
			peers.add(F2FComputing.getLocalPeer());
		
		return peers;
	}

	public JobSelector(final Collection<F2FPeer> friends)
	{
		this.friends = friends;

		startInit();
		initFileChooser();
		initCompute();
		initSlave();
		endInit();
	}

	public JobSelector(FriendModel<F2FPeer> people)
	{
		members = people;
		
		startInit();
		initFileChooser();
		initCompute();
		initStats();
		endInit();
	}
	
	private void startInit()
	{
		this.setSize(new Dimension(560, 150));
		this.setLocationRelativeTo(null);
		this.setTitle("Start an F2F application");
		this.setResizable(false);
		
		layout = new SpringLayout();
		mainPanel = new JPanel(layout);
		mainPanel.setSize(new Dimension(560, 150));
	}
	
	private void initFileChooser()
	{
		JLabel label1 = new JLabel("Choose jar(s) and specify the Master class");
		layout.putConstraint(SpringLayout.NORTH, label1, 5, SpringLayout.NORTH, mainPanel);
		layout.putConstraint(SpringLayout.WEST, label1, 0, SpringLayout.WEST, mainPanel);
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
				int returnVal = fc.showOpenDialog(JobSelector.this);
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
	}
	
	private void initCompute()
	{
		btnCompute = new JButton("Compute");
		layout.putConstraint(SpringLayout.NORTH, btnCompute, 5, SpringLayout.SOUTH, tf2);
		layout.putConstraint(SpringLayout.WEST, btnCompute, 0, SpringLayout.WEST, tf2);
		mainPanel.add(btnCompute);
		
		btnCompute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (tf1.getText().length() == 0) {
					logger.error("no jar-files name was specified");
				}
				else if (tf2.getText().length() == 0) {
					logger.error("master task name was not specified");
				}
				else {
					Collection<String> jarFilesNames = new ArrayList<String>();
					StringTokenizer tokenizer = new StringTokenizer(tf1.getText(), ";", false);
					while (tokenizer.hasMoreTokens()) jarFilesNames.add(tokenizer.nextToken().trim());
					//for (File file: selectedFiles) jarFiles.add(new F2FJarFile(file.getAbsolutePath()));
					String jobID;
					try {
						jobID = F2FComputing.createJob(jarFilesNames, tf2.getText(), getF2FPeers()).getJobID();
						logger.info("Started job with ID: " + jobID);
					} catch (final Exception ex) {
						ex.printStackTrace();
						logger.error("Error with starting a job! " + ex);
						new Thread()
						{
							public void run()
							{
								JOptionPane.showMessageDialog(
					                null, "An exception was thrown during job creation. \n"+ ex,
					                "Exception", JOptionPane.OK_OPTION);
							}
						}.start();
					}
				}
			}
		});
		
		// bottomPanel constraints (constraint SOUTH of the bottom panel to the last button)
		layout.putConstraint(SpringLayout.SOUTH, mainPanel, 5, SpringLayout.SOUTH, btnCompute);
	}

	private void initSlave()
	{
	    btnSlave = new JCheckBox("Participate as a Slave");
	    layout.putConstraint(SpringLayout.NORTH, btnSlave, 0, SpringLayout.NORTH, btnCompute);
		layout.putConstraint(SpringLayout.WEST, btnSlave, 10, SpringLayout.EAST, btnCompute);
		btnSlave.setSelected(false);
	    mainPanel.add(btnSlave);
	}
	
	private void initStats()
	{
		
		JButton button3 = new JButton("Show stats");
		layout.putConstraint(SpringLayout.NORTH, button3, 0, SpringLayout.NORTH, btnCompute);
		layout.putConstraint(SpringLayout.WEST, button3, 10, SpringLayout.EAST, btnCompute);
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
					logger.info(job.getJobID());
					Collection<Task> tasks = job.getTasks();
					Iterator<Task> taskIterator = tasks.iterator();
					while (taskIterator.hasNext())
					{
						Task task = taskIterator.next();
						logger.info("\tTask " + task.getTaskID());
						logger.info("\t\tstate: java.lang.Thread.State." + task.getState());
						if (task.getException() != null)
							logger.info("\t\texception: " + task.getException() + task.getException().getMessage());
						Collection<TaskProxy> proxies = task.getTaskProxies();
						Iterator<TaskProxy> proxyIterator = proxies.iterator();
						while (proxyIterator.hasNext())
						{
							TaskProxy proxy = proxyIterator.next();
							logger.info("\t\tTask " + proxy.getRemoteTaskID() + " message queue size: " + proxy.getMessageCount());
						}
					}
				}
			}
		});
	}
	
	private void endInit()
	{
		this.setContentPane(mainPanel);
		this.setVisible(true);
	}
	
	/*private void error(String msg)
	{
		if (mainWindow != null)
			mainWindow.error(msg);
		else
		{
			F2FDebug.println(msg);
		}
	}
	
	private void info(String msg)
	{
		if (mainWindow != null)
			mainWindow.info(msg);
		else
		{
			F2FDebug.println(msg);
		}
	}*/
	
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
