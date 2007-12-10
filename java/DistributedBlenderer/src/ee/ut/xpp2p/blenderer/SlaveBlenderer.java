package ee.ut.xpp2p.blenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import ee.ut.f2f.comm.CommunicationFailedException;
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

	String tempDir;
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
		if (masterProxy == null)
			throw new RuntimeException("Proxy of master task was not found!");

		boolean taskReceived = false;
		while (!taskReceived) {
			RenderTask receivedRenderTask = (RenderTask) masterProxy
					.receiveMessage();
			if (receivedRenderTask != null) {
				taskReceived = true;
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
					break;
				} catch (NothingRenderedException e) {
					System.out.println(e.getMessage());
					// TODO: Handle exception
				} catch (IOException e) {
					System.out.println(e.getMessage());
					// TODO: Handle exception
				}
				catch (CommunicationFailedException cfe) {
					System.out.println(cfe.getMessage());
					// TODO: Handle exception
				}
			}
			System.out.println("Line 72");
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
	 */
	public void renderTask(RenderTask task) throws NothingRenderedException {
		try {
			System.out.println("Filename = " + task.getFileName());
			File file = FileUtil.saveFile(task.getBlenderFile(), task.getFileName());
			System.out.println("Saved file: " + file);
			String[] cmdarr = { "blender", "-b", task.getFileName(), "-o",
					tempDir, "-F", task.getFileFormat(), "-s",
					String.valueOf(task.getStartFrame()), "-e",
					String.valueOf(task.getEndFrame()), "-a", "-x", "1" };
			System.out.println("Arguments: ");
			for(int i = 0; i < cmdarr.length; i++ ) System.out.println(cmdarr[i]);
			Process proc = Runtime.getRuntime().exec(cmdarr);
			BufferedReader br = new BufferedReader(new InputStreamReader(proc
					.getInputStream()));

			String line;
			boolean anythingRendered = false;
			System.out.println("Buffered reader: ");
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				if (line.startsWith("'blender' is not recognized")
						|| line.indexOf("blender") != -1
						&& line.indexOf("not found") != -1)
					throw new NothingRenderedException("Blender not found!");

				else if (line.startsWith("Append frame")
						|| line.startsWith("added frame")
						|| line.startsWith("Writing frame")) {
					System.out.println("Rendered frame " + line.split(" +")[2]);
					anythingRendered = true;
				}

				else if (line.startsWith("Blender quit") && anythingRendered) {
					System.out.println("Rendering finished");
				}
			}
			proc.destroy();

			if (!anythingRendered)
				throw new NothingRenderedException("Nothing rendered!");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			//TODO: Handle Exception
		}
	}

}
