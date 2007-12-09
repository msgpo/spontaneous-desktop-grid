package ee.ut.xpp2p.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import ee.ut.xpp2p.model.RenderResult;

/**
 * @author Jaan Neljandik, Vladimir Ðkarupelov
 * @created 12.11.2007
 */
public class FileUtilTest extends TestCase {

	private String part1;
	private String part2;
	private String part3;
	private String file1;
	private String file2;
	private String file3;
	private String output1;
	private String output2;
	private String output3;

	private String fileName;
	private String testFileName;

	@Override
	protected void setUp() throws Exception {
		part1 = "Some arts students or a small movie studio want to render a part of an animation \r\n"
				+ "movie (compare for example Elephant's dream http://www.elephantsdream.org/). \r\n";
		part2 = "To speed up this process they create in their instant messenger a chat group and \r\n"
				+ "start the dynamic GRID to carry out the distributed rendering task. Extensions \r\n";
		part3 = "to this scenarios will make use of rendering static scenes with povray \r\n"
				+ "http://www.povray.org/ or support video or sound encoding.";
		file1 = "etc/0001_0012.avi";
		file2 = "etc/0013_0019.avi";
		file3 = "etc/0020_0024.avi";
		output1 = "etc/test_0001_0012.avi";
		output2 = "etc/test_0013_0019.avi";
		output3 = "etc/test_0020_0024.avi";
		fileName = "etc\\victorDancing.avi";
		testFileName = "etc\\0001_0024.avi";
	}

	@Override
	protected void tearDown() throws Exception {
		// TODO: uncomment when the file can be correctly played
		/**
		 * boolean fileDeleted = false; if ((new File(fileName)).exists()) while
		 * (!fileDeleted) { fileDeleted = (new File(fileName)).delete(); }
		 */
		File test_output1 = new File(output1);
		File test_output2 = new File(output1);
		File test_output3 = new File(output1);
		if(test_output1.exists()) test_output1.delete();
		if(test_output2.exists()) test_output2.delete();
		if(test_output3.exists()) test_output3.delete();
	}

	public void testLoadFile() {
		try {
			byte[] loadedBytes = FileUtil.loadFile("etc\\scenario part 1.txt");
			String loadedString = new String(loadedBytes, "utf-8");
			assertEquals(part1, loadedString);
		} catch (IOException e) {
			fail();
		}
	}

	public void testSaveFile() {
		try {
			byte[] textBytes = part1.getBytes();
			File file = FileUtil.saveFile(textBytes, fileName);
			FileInputStream stream = new FileInputStream(file);
			assertTrue(file.exists());

			byte[] fileBytes = new byte[(int) file.length()];
			int offset = 0;
			int numRead = 0;

			while (offset < fileBytes.length) {
				numRead = stream.read(fileBytes, offset, fileBytes.length
						- offset);
				if (numRead >= 0)
					offset += numRead;
			}
			String loadedText = new String(fileBytes, "UTF-8");
			stream.close();
			assertEquals(part1, loadedText);
		} catch (IOException e) {
			fail();
		}
	}

	public void testComposeFile() {
		try {
			// Prepare data
			RenderResult result1 = new RenderResult();
			RenderResult result2 = new RenderResult();
			RenderResult result3 = new RenderResult();

			result1.setRenderedPart(FileUtil.loadFile(file1));
			result2.setRenderedPart(FileUtil.loadFile(file2));
			result3.setRenderedPart(FileUtil.loadFile(file3));

			result1.setStartFrame(0);
			result1.setEndFrame(12);
			result1.setFileName(output1);
			result2.setStartFrame(13);
			result2.setEndFrame(19);
			result2.setFileName(output2);
			result3.setStartFrame(20);
			result3.setEndFrame(24);
			result3.setFileName(output3);

			List<RenderResult> list = new ArrayList<RenderResult>();
			list.add(result2);
			list.add(result3);
			list.add(result1);

			// Execute
			boolean fileCreated = FileUtil.composeFile(list, fileName);
			assertTrue(fileCreated);

		} catch (IOException e) {
			fail();
		}
	}
}
