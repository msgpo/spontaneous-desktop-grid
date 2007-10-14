import java.io.File;

import junit.framework.TestCase;

/**
 * Test case for BlenderCommunicator
 *
 * @author Jürmo, Andres, Jaan
 *
 */
public class BlenderCommunicatorTest extends TestCase {

	/**
	 * Test method for {@link BlenderCommunicator#render(java.lang.String, int, int)}.
	 * Tests rendering frames 10 to 13 of a Blender file to the specified
	 * output location.
	 */
	public void testRender() {
		String inputFile = null, outputFile = null;
		
		try {
			inputFile = BlenderFileChooser.openBlendFile();
			
			String outputLocation = BlenderFileChooser.saveBlendFile();
			outputFile = outputLocation + "0010_0013.avi";
			
			if (inputFile == null || outputLocation == null) {
				System.out.println("Both input file and output location must be chosen");
				return;
			}			
			
			BlenderCommunicator.render(inputFile, outputLocation, "AVIJPEG", 10, 13);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		File output = new File(outputFile);
		
		System.out.println(outputFile);
		
		assertTrue(output.exists());
	}

}
