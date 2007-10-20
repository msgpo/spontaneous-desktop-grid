package ee.ut.xpp2p.communicator;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Command line communicator between Java and Blender
 * 
 * @author Andres, Madis, Jaan
 */
public class BlenderCommunicator {
	
	public static void main(String[] args) {
		String inputFile, outputLocation, fileFormat;
		
		try {
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
			
			render(inputFile, outputLocation, fileFormat, 10, 13);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Calls the Blender command line tool to render an
	 * animation in AVIJPEG format from a .blend file.
	 * Writes info about the process to standard output.
	 * 
	 * @param inputFile		  File to render
	 * @param outputLocation  Directory to save output
	 * @param fileFormat	  File format of the output
	 * @param startFrame	  First frame to render
	 * @param endFrame		  Last frame to render
	 */
	public static void render(String inputFile, String outputLocation, String fileFormat, int startFrame, int endFrame) throws Exception {
		
		String[] cmdarr = {"blender", 
					"-b", inputFile, 
					"-o", outputLocation, 
					"-F", fileFormat, 
					"-s", String.valueOf(startFrame),
					"-e", String.valueOf(endFrame), 
					"-a", "-x", "1"}; 
		Process proc = Runtime.getRuntime().exec(cmdarr);
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		
		String line;
		boolean anythingRendered = false;
		
		while ((line = br.readLine()) != null) {
			if (
				line.startsWith("'blender' is not recognized") ||
				line.indexOf("blender") != -1 && line.indexOf("not found") != -1
			)
				throw new Exception("Blender not found!");
			
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
			throw new Exception("Nothing rendered!");
	}
}
