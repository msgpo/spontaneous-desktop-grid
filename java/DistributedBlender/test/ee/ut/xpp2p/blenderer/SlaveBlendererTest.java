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
		task.setFileName("VictorDancingTest.blend");
		task.setFileFormat("AVIJPEG");
		task.setStartFrame(10);
		
		InputStream is = new FileInputStream("etc"+ File.separator +"VictorDancing.blend");
		byte[] fileBytes = new byte[is.available()];
		
		is.read(fileBytes);
		task.setBlenderFile(fileBytes);
		task.setEndFrame(13);

		BlenderSlaveTask slave = new BlenderSlaveTask(task);
		try {
			slave.renderTask(task);
		}
		catch(NothingRenderedException e) {
			e.printStackTrace();
		}

		String tempDir = System.getProperty("java.io.tmpdir");
		if (!tempDir.endsWith(File.separator))
		{
			tempDir += File.separator;
		}
		File output = new File(tempDir +"0010_0013.avi");
		assertTrue(output.exists());
		output.delete();
	}
}
