import junit.framework.TestCase;

/**
 * Test case for BlenderCommunicator
 *
 * @author Jürmo
 *
 */
public class BlenderCommuniocatorTest extends TestCase {

	/**
	 * Test method for {@link BlenderCommunicator#render(java.lang.String, int, int)}.
	 * Tests rendering frames 10 to 13 of file G:\\eclipse\\watertower.blend
	 */
	public void testRender() {
		String outputFile = null;
		try {
			outputFile = BlenderCommunicator.render("G:\\eclipse\\watertower.blend", 10, 13);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		assertEquals(outputFile, "C:\\render\\0010_0013.mov"); 
				
	}

}
