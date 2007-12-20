package ee.ut.xpp2p.blenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.TestCase;
import ee.ut.xpp2p.exception.NothingRenderedException;
import ee.ut.xpp2p.model.RenderTask;

/**
 * Test case for MasterBlenderer
 * 
 * @author Jürmo, Andres, Jaan
 * 
 */
public class SlaveBlendererTest extends TestCase {

	/**
	 * Tests rendering frames 10 to 13 of a Blender file to the specified output
	 * location.
	 */
	public void testRenderTask() throws Exception {
		RenderTask task = new RenderTask();
		task.setFileName("etc\\VictorDancingTest.blend");
		task.setFileFormat("AVIJPEG");
		task.setStartFrame(10);
		
		InputStream is = new FileInputStream("etc\\VictorDancing.blend");
		byte[] fileBytes = new byte[is.available()];
		
		is.read(fileBytes);
		task.setBlenderFile(fileBytes);
		task.setEndFrame(13);

		SlaveBlenderer slave = new SlaveBlenderer();
		try {
			slave.renderTask(task);
		}
		catch(NothingRenderedException e) {
			System.out.println(e);
		}

		File output = new File("etc\\0010_0013.avi");
		assertTrue(output.exists());
		output.delete();
	}
}
