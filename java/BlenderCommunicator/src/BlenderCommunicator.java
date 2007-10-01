import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Command line communicator between Java and Blender
 * 
 * @author Andres, Madis
 */

public class BlenderCommunicator {

	/**
	 * Calls the Blender command line tool to render an animation from a .blend file.
	 * Writes info about the process to standard output.
	 * 
	 * @param filename 		File to render (should export AVI or Quicktime)
	 * @param startFrame	First frame to render
	 * @param endFrame		Last frame to render
	 * @return 				Output filename
	 */
	public static String render(String filename, int startFrame, int endFrame) throws Exception {
		String command = "blender -b " + filename + " -s " + startFrame + " -e " + endFrame + " -a";
		
		Process proc = Runtime.getRuntime().exec(command);
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		
		String line, outputFile = null;
		
		while ((line = br.readLine()) != null) {
			
			if (line.startsWith("'blender' is not recognized"))
				throw new Exception("Blender not found!");
			
			if (line.startsWith("Starting")) {
				outputFile = line.substring(19);
				System.out.println("Starting to render into file: " + outputFile);
			}
			
			else if (line.startsWith("Created")) {
				String[] words = line.split(" ");
				outputFile = words[words.length - 1];
				System.out.println("Starting to render into file: " + outputFile);
			}	
			
			else if (
				line.startsWith("Append frame") ||
				line.startsWith("added frame") ||
				line.startsWith("Writing frame")
			) {
				System.out.println("Rendered frame " + line.split(" +")[2]);
			}
			
			else if (line.startsWith("Blender quit") && outputFile != null) {
				System.out.println("Done!");
			}
		}
		
		if (outputFile == null)
			throw new Exception("AVI not created!");

		return outputFile;
	}

}
