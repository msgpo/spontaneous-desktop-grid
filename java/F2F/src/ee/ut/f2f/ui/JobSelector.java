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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.filechooser.FileFilter;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.core.Job;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.ui.model.FriendModel;
import ee.ut.f2f.util.F2FDebug;

public class JobSelector extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	private JTextField tf1 = null;
	private JTextField tf2 = null;
	private JPanel mainPanel = null;
	private UIController mainWindow = null;
	private File[] selectedFiles = null;
	private SpringLayout bottomPanelLayout = null;
	private JButton btnCompute = null;
	
	private FriendModel members = null;
	Collection<F2FPeer> friends = null;
	
	private Collection<F2FPeer> getF2FPeers()
	{
		if (friends != null)
			return friends;
		return members.getPeers();
	}

	public JobSelector(Collection<F2FPeer> friends)
	{
		this.friends = friends;

		startInit();
		initFileChooser();
		initCompute();
		endInit();
	}

	public JobSelector(UIController wnd, FriendModel people) {
		mainWindow = wnd;
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
		this.setTitle("Pick a .JAR");
		this.setResizable(false);
		
		bottomPanelLayout = new SpringLayout();
		mainPanel = new JPanel(bottomPanelLayout);
		mainPanel.setSize(new Dimension(560, 150));
		
	}
	
	private void initFileChooser()
	{
		JLabel label1 = new JLabel("Choose file:");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, label1, 5, SpringLayout.NORTH, mainPanel);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, label1, 0, SpringLayout.WEST, mainPanel);
		mainPanel.add(label1);
		
		tf1 = new JTextField();
		tf1.setColumns(40);
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, tf1, 5, SpringLayout.SOUTH, label1);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, tf1, 0, SpringLayout.WEST, label1);
		mainPanel.add(tf1);

		JButton button1 = new JButton("Browse...");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, button1, 0, SpringLayout.NORTH, tf1);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, button1, 5, SpringLayout.EAST, tf1);
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
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, tf2, 5, SpringLayout.SOUTH, tf1);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, tf2, 0, SpringLayout.WEST, tf1);
		mainPanel.add(tf2);
	}
	
	private void initCompute()
	{
		btnCompute = new JButton("Compute");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, btnCompute, 5, SpringLayout.SOUTH, tf2);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, btnCompute, 0, SpringLayout.WEST, tf2);
		mainPanel.add(btnCompute);
		
		btnCompute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (tf1.getText().length() == 0) {
					error("no jar-files name was specified");
				}
				else if (tf2.getText().length() == 0) {
					error("master task name was not specified");
				}
				else {
					Collection<String> jarFilesNames = new ArrayList<String>();
					StringTokenizer tokenizer = new StringTokenizer(tf1.getText(), ";", false);
					while (tokenizer.hasMoreTokens()) jarFilesNames.add(tokenizer.nextToken().trim());
					//for (File file: selectedFiles) jarFiles.add(new F2FJarFile(file.getAbsolutePath()));
					String jobID;
					try {
						jobID = F2FComputing.createJob(jarFilesNames, tf2.getText(), getF2FPeers()).getJobID();
						info("Started job with ID: " + jobID);
					} catch (F2FComputingException ex) {
						error("Error with starting a job! " + ex);
					}
				}
			}
		});
		
		// bottomPanel constraints (constraint SOUTH of the bottom panel to the last button)
		bottomPanelLayout.putConstraint(SpringLayout.SOUTH, mainPanel, 5, SpringLayout.SOUTH, btnCompute);
	}
	
	private void initStats()
	{
		
		JButton button3 = new JButton("Show stats");
		bottomPanelLayout.putConstraint(SpringLayout.NORTH, button3, 0, SpringLayout.NORTH, btnCompute);
		bottomPanelLayout.putConstraint(SpringLayout.WEST, button3, 10, SpringLayout.EAST, btnCompute);
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
	}
	
	private void endInit()
	{
		this.setContentPane(mainPanel);
		this.setVisible(true);
	}
	
	private void error(String msg)
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
	}
	
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
