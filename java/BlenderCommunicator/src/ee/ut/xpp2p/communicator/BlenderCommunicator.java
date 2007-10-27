package ee.ut.xpp2p.communicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import ee.ut.xpp2p.exception.NothingRenderedException;
import ee.ut.xpp2p.model.Job;
import ee.ut.xpp2p.model.Task;

/**
 * Command line communicator between Java and Blender
 * 
 * @author Andres, Madis, Jaan
 */
public class BlenderCommunicator {
	
	public static void main(String[] args) {
		String inputFile, outputLocation, fileFormat;
		
		// Getting input file
		inputFile = BlenderFileChooser.openBlendFile();
		if (inputFile == null) {
			System.out.println("Input file must be chosen");
			return;
		}
		
		
		// Getting output location
		outputLocation = BlenderFileChooser.saveBlendFile();
		if (outputLocation == null) {
			System.out.println("Output location must be chosen");
			return;
		}
		
		// Getting File format
		fileFormat = BlenderFileChooser.selectFileFormat();
		if (fileFormat == null) {
			System.out.println("Output file format must be chosen");
			return;
		}
		
		Job job = new Job();
		job.setInputFile(inputFile);
		job.setOutputLocation(outputLocation);
		job.setOutputFormat(fileFormat);
		//TODO: Hard-coded parameters
		job.setStartFrame(10);
		job.setEndFrame(14);
		job.setParticipants(2);
		
		int numberOfFrames = countFrames(inputFile);
		//renderJob(job);
	}
	
	/**
	 * Finds the number of frames in given .blend-file by starting rendering 
	 * and then reading the number of frames from command line.
	 * @param inputFile file for which framecount is found
	 * @return number of frames in given file
	 */
	public static int countFrames(String inputFile) {
		
		String[] cmdarr = {"blender", 
					"-b", inputFile, 
					"-o", "test", 
					"-a"}; 
		try {
			Process proc = Runtime.getRuntime().exec(cmdarr);
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		
			String line;
			
			while ((line = br.readLine()) != null) {
				if (
 					line.startsWith("'blender' is not recognized") ||
					line.indexOf("blender") != -1 && line.indexOf("not found") != -1
				)
					throw new NothingRenderedException("Couldn't read framecount in file");
				
				else if (line.startsWith("Created") && line.indexOf("test") > -1) {
					//Finds the framecount
					String filename = line.substring(line.lastIndexOf("test"));
					String frameCount = filename.substring(filename.lastIndexOf("_")+1, filename.lastIndexOf("."));
					
					//Cleans up
					proc.destroy();
					(new File(filename)).delete();
					
					return Integer.parseInt(frameCount);
				}
			}
				
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}
		catch(NothingRenderedException e){
			System.out.println(e.getMessage());
		}
		return 0;
	}
	
	/**
	 * Calls the Blender command line tool to render an
	 * animation in AVIJPEG format from a .blend file.
	 * Writes info about the process to standard output.
	 * @param task task to render
	 * @throws Exception
	 */
	public static void renderTask(Task task) {
		
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
		
			if (!anythingRendered)
				throw new NothingRenderedException("Nothing rendered!");
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}
		catch(NothingRenderedException e){
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Splits the number of frames (from startFrame to endFrame) by the number
	 * of parts. The parameter startFrame has to be a larger number than endFrame 
	 * @param startFrame	the frame to start from when splitting
	 * @param endFrame		the frame to finish with when splitting
	 * @param parts			the number of parts the task must be split into
	 * @return				an array with the lengths of the parts of the task. The
	 * length of the array is the number of parts
	 * 
	 */
	public static long[] splitTask(long startFrame, long endFrame, int parts) {
		long numberOfFrames = endFrame - startFrame + 1;
		long remainder = numberOfFrames % parts;
		long exact = numberOfFrames - remainder;
		long exactPartLength = exact / parts;
		long[] partLengths = new long[parts];
		
		for (int i = 0; i < parts; i++){
			partLengths[i] = exactPartLength;
			if (i < remainder){
				partLengths[i]++;
			}
		}
		
		System.out.println(System.getProperty("user.dir"));
		
		return partLengths;
	}

	/**
	 * Splits given job into tasks and renders it
	 * @param job job to render
	 */
	public static void renderJob(Job job) {
		long[] partLengths = splitTask(job.getStartFrame(), job.getEndFrame(), job.getParticipants());
		
		long currentFrame = job.getStartFrame();
		
		for(int i = 0; i <partLengths.length; i++){
			Task task = new Task();
			task.setInputFile(job.getInputFile());
			task.setOutputLocation(job.getOutputLocation());
			task.setFileFormat(job.getOutputFormat());
			task.setStartFrame(currentFrame);
			task.setEndFrame(currentFrame + partLengths[i] - 1);
			
			renderTask(task);
						
			currentFrame += partLengths[i];
		}
	}
}
