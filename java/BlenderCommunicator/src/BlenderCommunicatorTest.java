import junit.framework.TestCase;

/**
 * Test case for BlenderCommunicator
 *
 * @author Jürmo
 *
 */
public class BlenderCommunicatorTest extends TestCase {

	/**
	 * Test method for {@link BlenderCommunicator#render(java.lang.String, int, int)}.
	 * Tests rendering frames 10 to 13 of a Blender file
	 * configured to render a QuickTime movie into C:\render
	 */
	public void testRender() {
		String inputFile = null, outputFile = null;

		try {
			inputFile = BlenderFileChooser.getBlendFile();
			
			if (inputFile == null) {
				System.out.println("No file chosen.");
				return;
			}
			
			outputFile = BlenderCommunicator.render(inputFile, 10, 13);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		assertEquals(outputFile, "C:\\render\\0010_0013.mov"); 
	}

}
