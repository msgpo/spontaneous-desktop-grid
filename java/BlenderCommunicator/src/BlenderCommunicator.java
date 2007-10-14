import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Command line communicator between Java and Blender
 * 
 * @author Andres, Madis, Jaan
 */
public class BlenderCommunicator {

	/**
	 * Calls the Blender command line tool to render an
	 * animation in AVIJPEG format from a .blend file.
	 * Writes info about the process to standard output.
	 * 
	 * @param inputFile		  File to render
	 * @param outputLocation Directory to save output
	 * @param startFrame	  First frame to render
	 * @param endFrame		  Last frame to render
	 */
	public static void render(String inputFile, String outputLocation, int startFrame, int endFrame) throws Exception {
		String command =
			"blender -b \"" + inputFile +
			"\" -o \"" + outputLocation +
			"\" -F AVIJPEG -s " +startFrame +
			" -e " + endFrame + " -a -x 1"
		;
		
		System.out.println(command);
		
		Process proc = Runtime.getRuntime().exec(command);
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
