package ee.ut.xpp2p.blenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.xpp2p.exception.NothingRenderedException;
import ee.ut.xpp2p.model.RenderResult;
import ee.ut.xpp2p.model.RenderTask;
import ee.ut.xpp2p.util.FileUtil;

/**
 * @author Jaan Neljandik, Vladimir Ðkarupelov
 * @created 05.11.2007
 */
public class SlaveBlenderer extends Task {

	String tempDir = null;
	/*
	 * (non-Javadoc)
	 * 
	 * @see ee.ut.f2f.core.Task#runTask()
	 */
	public void runTask() {
		System.out.println("runTask started!");
		// Gets proxy of MasterRenderer
		TaskProxy masterProxy = this.getTaskProxy(this.getJob()
				.getMasterTaskID());
		Properties props = System.getProperties();
		tempDir = props.getProperty("java.io.tmpdir");
		if (!tempDir.endsWith(File.separator))
		{
			tempDir += File.separator;
		}
		if (masterProxy == null)
			throw new RuntimeException("Proxy of master task was not found!");

		RenderTask receivedRenderTask = (RenderTask) masterProxy
				.receiveMessage();
		try {
			renderTask(receivedRenderTask);
			RenderResult result = new RenderResult();
			result.setEndFrame(receivedRenderTask.getEndFrame());
			result.setStartFrame(receivedRenderTask.getStartFrame());
			String fileName = FileUtil.generateOutputFileName(receivedRenderTask.getStartFrame(), receivedRenderTask.getEndFrame(), receivedRenderTask.getExtension());
			result.setFileName(fileName);			
			String outputFile = tempDir + fileName;
			result.setRenderedPart(FileUtil.loadFile(outputFile));
			masterProxy.sendMessage(result);
			//deletes rendered and sended part
			FileUtil.deleteFiles(outputFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calls the Blender command line tool to render an animation in AVIJPEG
	 * format from a .blend file. Writes info about the process to standard
	 * output.
	 * 
	 * @param task
	 *            task to render
	 * @throws NothingRenderedException
	 * @throws IOException 
	 */
	public void renderTask(RenderTask task) throws NothingRenderedException, IOException
	{
		File blenderFile = new File(task.getFileName());
		String blenderFileName = blenderFile.getName();
		if (tempDir == null)
		{
			Properties props = System.getProperties();
			tempDir = props.getProperty("java.io.tmpdir");
			if (!tempDir.endsWith(File.separator))
			{
				tempDir += File.separator;
			}
		}
		String fullBlenderFileName = tempDir + blenderFileName;
		System.out.println("Filename = " + fullBlenderFileName);
		File file = FileUtil.saveFile(task.getBlenderFile(), fullBlenderFileName);
		System.out.println("Saved file: " + file);
		String[] cmdarr = { "blender", "-b", fullBlenderFileName, "-o",
				tempDir, "-F", task.getFileFormat(), "-s",
				String.valueOf(task.getStartFrame()), "-e",
				String.valueOf(task.getEndFrame()), "-a", "-x", "1" };
		System.out.println("Arguments: ");
		for(int i = 0; i < cmdarr.length; i++ ) System.out.println(cmdarr[i]);
		Process proc = Runtime.getRuntime().exec(cmdarr);
		BufferedReader br = new BufferedReader(new InputStreamReader(proc
				.getInputStream()));

		String line;
		System.out.println("Buffered reader: ");
		while ((line = br.readLine()) != null && !isStopped()) {
			System.out.println(line);
			if (line.startsWith("'blender' is not recognized")
					|| line.indexOf("blender") != -1
					&& line.indexOf("not found") != -1)
			{
				System.out.println("Blender not found!");
				break;
			}
			else if (line.startsWith("Append frame")
					|| line.startsWith("added frame")
					|| line.startsWith("Writing frame"))
			{
				System.out.println("Rendered frame " + line.split(" +")[2]);
			}
			else if (line.startsWith("Blender quit"))
			{
				System.out.println("Rendering finished");
			}
		}
		proc.destroy();
	}

}
