package ee.ut.xpp2p.blenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.xpp2p.exception.NothingRenderedException;
import ee.ut.xpp2p.model.RenderResult;
import ee.ut.xpp2p.model.RenderTask;
import ee.ut.xpp2p.util.FileUtil;

/**
 * @author Jaan Neljandik
 * @created 05.11.2007
 */
public class SlaveBlenderer extends Task {

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
		System.out.println("Line 31");
		if (masterProxy == null)
			throw new RuntimeException("Proxy of master task was not found!");

		boolean taskReceived = false;
		System.out.println("Line 36");
		while (!taskReceived) {
			System.out.println("Line 38");
			RenderTask receivedRenderTask = (RenderTask) masterProxy
					.receiveMessage();
			System.out.println("Line 40");
			if (receivedRenderTask != null) {
				taskReceived = true;
				try {
					System.out.println("Line 40");
					renderTask(receivedRenderTask);
					System.out.println("Line 47");
					RenderResult result = new RenderResult();
					result.setEndFrame(receivedRenderTask.getEndFrame());
					result.setStartFrame(receivedRenderTask.getStartFrame());
					// FIXME Find output file via user interface or some other way
					String outputFile = receivedRenderTask.getOutputLocation()
							+ "part" + receivedRenderTask.getStartFrame()
							+ "-" + receivedRenderTask.getEndFrame() + "."
							+ receivedRenderTask.getExtension();
					result.setRenderedPart(FileUtil.loadFile(outputFile));
					masterProxy.sendMessage(result);
					// FIXME Delete output file
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
			FileUtil.saveFile(task.getBlenderFile(), task.getFileName());

			String[] cmdarr = { "blender", "-b", task.getFileName(), "-o",
					task.getOutputLocation(), "-F", task.getFileFormat(), "-s",
					String.valueOf(task.getStartFrame()), "-e",
					String.valueOf(task.getEndFrame()), "-a", "-x", "1" };
			Process proc = Runtime.getRuntime().exec(cmdarr);
			BufferedReader br = new BufferedReader(new InputStreamReader(proc
					.getInputStream()));

			String line;
			boolean anythingRendered = false;

			while ((line = br.readLine()) != null) {
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
