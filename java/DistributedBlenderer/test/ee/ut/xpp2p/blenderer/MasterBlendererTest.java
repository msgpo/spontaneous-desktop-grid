package ee.ut.xpp2p.blenderer;

import java.io.File;

import junit.framework.TestCase;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.xpp2p.blenderer.MasterBlenderer;
import ee.ut.xpp2p.model.RenderJob;

/**
 * Test case for MasterBlenderer
 * 
 * @author Jürmo, Andres, Jaan
 * 
 */
public class MasterBlendererTest extends TestCase {

	/**
	 * Tests splitting job into tasks
	 */
	public void testSplit() {
		MasterBlenderer master = new MasterBlenderer();
		
		// test uneven split
		long startFrame = 23L;
		long endFrame = 126L;
		int parts = 5;

		long[] partLengths = master.splitTask(startFrame,
				endFrame, parts);

		assertEquals(partLengths[0], 21L);
		assertEquals(partLengths[1], 21L);
		assertEquals(partLengths[2], 21L);
		assertEquals(partLengths[3], 21L);
		assertEquals(partLengths[4], 20L);

		// test even split
		startFrame = 23L;
		endFrame = 122L;
		parts = 5;

		partLengths = master.splitTask(startFrame, endFrame, parts);

		assertEquals(partLengths[0], 20L);
		assertEquals(partLengths[1], 20L);
		assertEquals(partLengths[2], 20L);
		assertEquals(partLengths[3], 20L);
		assertEquals(partLengths[4], 20L);

		// test splitting single frame
		startFrame = 23L;
		endFrame = 23L;
		parts = 5;

		partLengths = master.splitTask(startFrame, endFrame, parts);

		assertEquals(partLengths[0], 1L);
		assertEquals(partLengths[1], 0L);
		assertEquals(partLengths[2], 0L);
		assertEquals(partLengths[3], 0L);
		assertEquals(partLengths[4], 0L);

		// test splitting with more parts than frames
		startFrame = 23L;
		endFrame = 25L;
		parts = 5;

		partLengths = master.splitTask(startFrame, endFrame, parts);

		assertEquals(partLengths[0], 1L);
		assertEquals(partLengths[1], 1L);
		assertEquals(partLengths[2], 1L);
		assertEquals(partLengths[3], 0L);
		assertEquals(partLengths[4], 0L);

		// test splitting when number of parts equals number of frames
		startFrame = 23L;
		endFrame = 27L;
		parts = 5;

		partLengths = master.splitTask(startFrame, endFrame, parts);

		assertEquals(partLengths[0], 1L);
		assertEquals(partLengths[1], 1L);
		assertEquals(partLengths[2], 1L);
		assertEquals(partLengths[3], 1L);
		assertEquals(partLengths[4], 1L);
	}

	/**
	 * Tests job rendering
	 * @throws F2FComputingException 
	 */
	public void testRenderJob() throws F2FComputingException {
		//FIXME: Adapt to new filename in renderJob
		RenderJob job = new RenderJob();
		job.setInputFileName("etc"+File.separator+"VictorDancing.blend");
		job.setOutputLocation("etc"+File.separator);
		job.setOutputFormat("AVIJPEG");
		job.setStartFrame(10);
		job.setEndFrame(14);
		job.setParticipants(2);

		new MasterBlenderer().renderJob(job);

		File output1 = new File(job.getOutputLocation() + "0010_0012.avi");
		File output2 = new File(job.getOutputLocation() + "0013_0014.avi");

		assertTrue(output1.exists());
		assertTrue(output2.exists());

		output1.delete();
		output2.delete();
	}

	/**
	 * Tests Counting the frames of given .blend file
	 */
	public void testCountFrames() {
		long frames = new MasterBlenderer()
				.countFrames("etc"+File.separator+"VictorDancing.blend");

		assertEquals(frames, 24L);
	}

}
