package ee.ut.xpp2p.communicator;
import java.io.File;

import junit.framework.TestCase;
import ee.ut.xpp2p.model.Job;
import ee.ut.xpp2p.model.Task;

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
	public void testRenderTask() {
		Task task = new Task();
		task.setInputFile("etc\\VictorDancing.blend");
		task.setOutputLocation("etc\\");
		task.setFileFormat("AVIJPEG");
		task.setStartFrame(10);
		task.setEndFrame(13);
		
		BlenderCommunicator.renderTask(task);
		
		File output = new File("etc\\0010_0013.avi");
		assertTrue(output.exists());
		output.delete();
	}
	
	public void testSplit(){
		// test uneven split
		long startFrame = 23L;
		long endFrame = 126L;
		int parts = 5;
		
		long[] partLengths = BlenderCommunicator.splitTask(startFrame, endFrame, parts);
		
		assertEquals(partLengths[0], 21L);
		assertEquals(partLengths[1], 21L);
		assertEquals(partLengths[2], 21L);
		assertEquals(partLengths[3], 21L);
		assertEquals(partLengths[4], 20L);
		
		// test even split
		startFrame = 23L;
		endFrame = 122L;
		parts = 5;
		
		partLengths = BlenderCommunicator.splitTask(startFrame, endFrame, parts);
		
		assertEquals(partLengths[0], 20L);
		assertEquals(partLengths[1], 20L);
		assertEquals(partLengths[2], 20L);
		assertEquals(partLengths[3], 20L);
		assertEquals(partLengths[4], 20L);
		
		// test splitting single frame
		startFrame = 23L;
		endFrame = 23L;
		parts = 5;
		
		partLengths = BlenderCommunicator.splitTask(startFrame, endFrame, parts);
		
		assertEquals(partLengths[0], 1L);
		assertEquals(partLengths[1], 0L);
		assertEquals(partLengths[2], 0L);
		assertEquals(partLengths[3], 0L);
		assertEquals(partLengths[4], 0L);
		
		// test splitting with more parts than frames
		startFrame = 23L;
		endFrame = 25L;
		parts = 5;
		
		partLengths = BlenderCommunicator.splitTask(startFrame, endFrame, parts);
		
		assertEquals(partLengths[0], 1L);
		assertEquals(partLengths[1], 1L);
		assertEquals(partLengths[2], 1L);
		assertEquals(partLengths[3], 0L);
		assertEquals(partLengths[4], 0L);
		
		// test splitting when number of parts equals number of frames
		startFrame = 23L;
		endFrame = 27L;
		parts = 5;
		
		partLengths = BlenderCommunicator.splitTask(startFrame, endFrame, parts);
		
		assertEquals(partLengths[0], 1L);
		assertEquals(partLengths[1], 1L);
		assertEquals(partLengths[2], 1L);
		assertEquals(partLengths[3], 1L);
		assertEquals(partLengths[4], 1L);
	}
	
	public void testRenderJob(){
		Job job = new Job();
		job.setInputFile("etc\\VictorDancing.blend");
		job.setOutputLocation("etc\\");
		job.setOutputFormat("AVIJPEG");
		job.setStartFrame(10);
		job.setEndFrame(14);
		job.setParticipants(2);
		
		BlenderCommunicator.renderJob(job);
		
		File output1 = new File(job.getOutputLocation() + "0010_0012.avi");
		File output2 = new File(job.getOutputLocation() + "0013_0014.avi");
		
		assertTrue(output1.exists());
		assertTrue(output2.exists());
		
		output1.delete();
		output2.delete();		
	}
	
	public void testCountFrames(){
		int frames = BlenderCommunicator.countFrames("etc\\VictorDancing.blend");
		
		assertEquals(frames, 24);
	}

}
