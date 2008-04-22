package ee.ut.xpp2p.blenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;

import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.xpp2p.exception.NothingRenderedException;
import ee.ut.xpp2p.model.RenderJob;
import ee.ut.xpp2p.model.RenderResult;
import ee.ut.xpp2p.ui.MainWindow;
import ee.ut.xpp2p.util.FileUtil;

/**
 * Command line communicator between Java and Blender
 * 
 * @author Andres, Madis, Jaan Neljandik, Vladimir Ðkarupelov
 */
public class BlenderMasterTask extends Task {

	/**
	 * Executable
	 * @param args
	 */
	public static void main(String[] args) {
		new MainWindow(new BlenderMasterTask());
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
	 * of parts.
	 * 
	 * @param numberOfFrames
	 *            the frames to split
	 * @param parts
	 *            the number of parts the task must be split into
	 * @return an array with the lengths of the parts of the task. The length of
	 *         the array is the number of parts
	 * 
	 */
	public long[] splitTask(long numberOfFrames, int parts) {
		long remainder = numberOfFrames % parts;
		long exactPartLength = (numberOfFrames - remainder) / parts;
		long[] partLengths = new long[parts];

		for (int i = 0; i < parts; i++) {
			partLengths[i] = exactPartLength;
			if (i < remainder) {
				partLengths[i]++;
			}
		}

		return partLengths;
	}

	// here the master collects the results the slave tasks have render 
	private List<RenderResult> renderedResults = new ArrayList<RenderResult>();
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
			
			// prepare the slave tasks
			Collection<Task> tasks = new ArrayList<Task>();
			long[] partLengths = splitTask(job.getEndFrame() - job.getStartFrame() + 1, getJob().getPeers().size());
			long startFrame = job.getStartFrame();
			for (int i = 0; i < getJob().getPeers().size(); i++)
			{
				BlenderSlaveTask slaveTask = 
					new BlenderSlaveTask(
						job.getInputFileName(), 
						job.getInputFile(), 
						job.getOutputFormat(), 
						startFrame, 
						startFrame + partLengths[i] - 1);
				tasks.add(slaveTask);
				startFrame += partLengths[i];
			}

			// submit the slave tasks
			getJob().submitTasks(tasks, getJob().getPeers());
			
			// wait until the slaves have rendered and returned the movie clips
			// the messageReceivedEvent() method collects the results
			synchronized (renderedResults)
			{
				renderedResults.wait();
			}
						
			// compose the resulting movie
			FileUtil.composeResult(
					renderedResults,
					job.getOutputLocation(),
					job.getInputFileName(),
					job.getExtension());

			long end = System.currentTimeMillis();
			System.out.println("Took " + (end - start) / 1000 + "s");
			} catch (Exception e) {
				e.printStackTrace();
			}
			synchronized (mainWindow)
			{
				mainWindow.notifyAll();
			}
		}}.start();
	}
	
	// handle messages from the slave tasks
	// this means collecting the rendered parts 
	public void messageReceivedEvent(String remoteTaskID)
	{
		TaskProxy proxy = getTaskProxy(remoteTaskID);
		if (!proxy.hasMessage()) return;
		RenderResult renderResult = (RenderResult) proxy.receiveMessage();
		synchronized (renderedResults)
		{
			renderedResults.add(renderResult);
			if (renderedResults.size() == getJob().getPeers().size())
				renderedResults.notifyAll();
		}
	}
}
