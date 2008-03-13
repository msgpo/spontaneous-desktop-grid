package ee.ut.xpp2p.blenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;

import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.xpp2p.exception.NothingRenderedException;
import ee.ut.xpp2p.model.RenderJob;
import ee.ut.xpp2p.model.RenderResult;
import ee.ut.xpp2p.model.RenderTask;
import ee.ut.xpp2p.ui.MainWindow;
import ee.ut.xpp2p.util.FileUtil;

/**
 * Command line communicator between Java and Blender
 * 
 * @author Andres, Madis, Jaan Neljandik, Vladimir Ðkarupelov
 */
public class MasterBlenderer extends Task {

	private String inputFile;
	/**
	 * Executable
	 * @param args
	 */
	public static void main(String[] args) {
		new MainWindow(new MasterBlenderer());
	}

	private JFrame mainWindow;
	/*
	 * (non-Javadoc)
	 * 
	 * @see ee.ut.f2f.core.Task#runTask()
	 */
	public void runTask()
	{
		mainWindow = new MainWindow(this);
		synchronized (mainWindow)
		{
			try
			{
				mainWindow.wait();
			}
			catch (InterruptedException e){}
		}
	}

	/**
	 * Finds the number of frames in given .blend-file by starting rendering and
	 * then reading the number of frames from command line.
	 * 
	 * @param inputFile
	 *            file for which framecount is found
	 * @return number of frames in given file
	 */
	public long countFrames(String inputFile) {
		setInputFile(inputFile);
		String[] cmdarr = { "blender", "-b", inputFile, "-o", "frameCount",
				"-F", "AVIJPEG", "-a", "-x", "1" };
		try {
			Process proc = Runtime.getRuntime().exec(cmdarr);
			BufferedReader br = new BufferedReader(new InputStreamReader(proc
					.getInputStream()));
			String line;

			while ((line = br.readLine()) != null) {
				if (line.startsWith("'blender' is not recognized")
						|| line.indexOf("blender") != -1
						&& line.indexOf("not found") != -1)
					throw new NothingRenderedException(
							"Couldn't read framecount in file");

				else if (line.startsWith("Created")
						&& line.indexOf("frameCount") > -1) {
					// Finds the framecount
					String filename = line.substring(line
							.lastIndexOf("frameCount"));
					String frameCount = filename.substring(filename
							.lastIndexOf("_") + 1, filename.lastIndexOf("."));

					// Cleans up
					proc.destroy();
					boolean frameFileDeleted = false;

					// Creating this file takes some time... so we wait a little
					while (!frameFileDeleted) {
						frameFileDeleted = (new File(filename)).delete();
					}
					return Long.parseLong(frameCount);
				}
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (NothingRenderedException e) {
			System.out.println(e.getMessage());
		}
		return 0;
	}

	/**
	 * Splits the number of frames (from startFrame to endFrame) by the number
	 * of parts. The parameter startFrame has to be a larger number than
	 * endFrame
	 * 
	 * @param startFrame
	 *            the frame to start from when splitting
	 * @param endFrame
	 *            the frame to finish with when splitting
	 * @param parts
	 *            the number of parts the task must be split into
	 * @return an array with the lengths of the parts of the task. The length of
	 *         the array is the number of parts
	 * 
	 */
	public long[] splitTask(long startFrame, long endFrame, int parts) {
		long numberOfFrames = endFrame - startFrame + 1;
		long remainder = numberOfFrames % parts;
		long exact = numberOfFrames - remainder;
		long exactPartLength = exact / parts;
		long[] partLengths = new long[parts];

		for (int i = 0; i < parts; i++) {
			partLengths[i] = exactPartLength;
			if (i < remainder) {
				partLengths[i]++;
			}
		}

		return partLengths;
	}

	/**
	 * Splits given job into tasks and renders it
	 * 
	 * @param job
	 *            job to render
	 */
	public void renderJob(final RenderJob job) {
		new Thread() { public void run() {
			try {
			long start = System.currentTimeMillis();

			// submit slave tasks
			getJob().submitTasks(SlaveBlenderer.class.getName(),
					getJob().getPeers().size(), getJob().getPeers());

			// get IDs of all the tasks that have been created
			Collection<String> taskIDs = getJob().getTaskIDs();

			// get proxies of slave tasks
			Collection<TaskProxy> slaveProxies = new ArrayList<TaskProxy>();
			for (String taskID : taskIDs) {
				// do not get proxy of master task
				if (taskID == getTaskID())
					continue;
				TaskProxy proxy = getTaskProxy(taskID);
				if (proxy != null)
					slaveProxies.add(proxy);
			}

			long[] partLengths = splitTask(job.getStartFrame(), job
					.getEndFrame(), getJob().getPeers().size());
			long currentFrame = job.getStartFrame();
			int i = 0;
			byte[] input = FileUtil.loadFile(job.getInputFileName());
			String inputFileName = new File(job.getInputFileName()).getName();
			RenderTask task = new RenderTask();
			task.setFileName(inputFileName);
			task.setBlenderFile(input);
			task.setFileFormat(job.getOutputFormat());
			for (TaskProxy proxy : slaveProxies)
			{
				F2FDebug.println("task "+proxy.getRemoteTaskID()+" renders frames "+currentFrame+".."+(currentFrame + partLengths[i] - 1));
				task.setStartFrame(currentFrame);
				task.setEndFrame(currentFrame + partLengths[i] - 1);
				try {
					proxy.sendMessage(task);
				} catch (CommunicationFailedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				currentFrame += partLengths[i];
				i++;
			}

			long replies = 0;
			List<RenderResult> results = new ArrayList<RenderResult>();

			while (replies < partLengths.length) {
				for (TaskProxy proxy : slaveProxies) {
					if (proxy.hasMessage()) {
						replies++;
						results.add((RenderResult) proxy.receiveMessage());
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// FIXME: Adapt to lost friends
			}
			String fileName = FileUtil.generateOutputFileName(getInputFile(), job.getExtension());
			FileUtil.composeFile(results, job.getOutputLocation(), fileName);

			long end = System.currentTimeMillis();
			System.out.println("Took " + (end - start) / 1000 + "s");
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: Handle exception
			}
			synchronized (mainWindow)
			{
				mainWindow.notifyAll();
			}
		}}.start();
	}


	public String getInputFile() {
		return inputFile;
	}


	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}
}
