package ee.ut.xpp2p.blenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ee.ut.f2f.core.Task;
import ee.ut.f2f.core.TaskProxy;
import ee.ut.xpp2p.exception.NothingRenderedException;
import ee.ut.xpp2p.model.RenderTask;

/**
 * @author Jaan Neljandik
 * @created 05.11.2007
 */
public class SlaveBlenderer extends Task{

	/* (non-Javadoc)
	 * @see ee.ut.f2f.core.Task#runTask()
	 */
	public void runTask() { 
		// Gets proxy of MasterRenderer
		TaskProxy masterProxy = this.getTaskProxy(this.getJob().getMasterTaskID());
		if (masterProxy == null) throw new RuntimeException("Proxy of master task was not found!");
		
		boolean taskReceived = false;
		while (!taskReceived) {
			RenderTask receivedRenderTask = (RenderTask)masterProxy.receiveMessage();
			if (receivedRenderTask != null) {
				taskReceived = true;
				renderTask(receivedRenderTask);
			}
		}
	}
	
	
	/**
	 * Calls the Blender command line tool to render an
	 * animation in AVIJPEG format from a .blend file.
	 * Writes info about the process to standard output.
	 * @param task task to render
	 * @throws Exception
	 */
	public void renderTask(RenderTask task) {
		
		String[] cmdarr = {"blender", 
					"-b", task.getInputFile(), 
					"-o", task.getOutputLocation(), 
					"-F", task.getFileFormat(), 
					"-s", String.valueOf(task.getStartFrame()),
					"-e", String.valueOf(task.getEndFrame()), 
					"-a", "-x", "1"}; 
		try {
			Process proc = Runtime.getRuntime().exec(cmdarr);
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		
			String line;
			boolean anythingRendered = false;
			
			while ((line = br.readLine()) != null) {
				if (
					line.startsWith("'blender' is not recognized") ||
					line.indexOf("blender") != -1 && line.indexOf("not found") != -1
				)
					throw new NothingRenderedException("Blender not found!");
				
				else if (
					line.startsWith("Append frame") ||
					line.startsWith("added frame") ||
					line.startsWith("Writing frame")
				) {
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
			else {
				//TODO: Send resulting file
			}
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}
		catch(NothingRenderedException e){
			System.out.println(e.getMessage());
		}
	}
	
}
