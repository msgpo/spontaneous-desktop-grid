package ee.ut.xpp2p.blenderer;

import java.io.File;

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
	public void testRenderTask() {
		//FIXME: Adapt to new filename and behavior in renderTask
		RenderTask task = new RenderTask();
		task.setFileName("etc\\VictorDancing.blend");
		task.setOutputLocation("etc\\");
		task.setFileFormat("AVIJPEG");
		task.setStartFrame(10);
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
