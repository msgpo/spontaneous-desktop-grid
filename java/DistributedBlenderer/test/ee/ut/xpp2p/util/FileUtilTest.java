package ee.ut.xpp2p.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import ee.ut.xpp2p.model.RenderResult;

/**
 * @author Jaan Neljandik
 * @created 12.11.2007
 */
public class FileUtilTest extends TestCase {

	private String part1;
	private String part2;
	private String part3;
	private String fileName = "etc\\saveFile.txt";

	@Override
	protected void setUp() throws Exception { 
		part1 = "Some arts students or a small movie studio want to render a part of an animation \r\n"
				+ "movie (compare for example Elephant's dream http://www.elephantsdream.org/). \r\n";
		part2 = "To speed up this process they create in their instant messenger a chat group and \r\n"
				+ "start the dynamic GRID to carry out the distributed rendering task. Extensions \r\n";
		part3 = "to this scenarios will make use of rendering static scenes with povray \r\n"
				+ "http://www.povray.org/ or support video or sound encoding.";
	}

	@Override
	protected void tearDown() throws Exception {
		boolean fileDeleted = false;
		if ((new File(fileName)).exists())
			while (!fileDeleted) {
				fileDeleted = (new File(fileName)).delete();
			}
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
			FileUtil.saveFile(textBytes, fileName);

			File file = new File(fileName);
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

			assertEquals(part1, loadedText);
		} catch (IOException e) {
			fail();
		}
	}

	public void testComposeFile() {
		try {
			//Prepare data
			RenderResult result1 = new RenderResult();
			RenderResult result2 = new RenderResult();
			RenderResult result3 = new RenderResult();

			result1.setRenderedPart(part1.getBytes());
			result2.setRenderedPart(part2.getBytes());
			result3.setRenderedPart(part3.getBytes());

			result1.setStartFrame(0);
			result1.setEndFrame(6);
			result2.setStartFrame(7);
			result2.setEndFrame(9);
			result3.setStartFrame(10);
			result3.setEndFrame(13);

			List<RenderResult> list = new ArrayList<RenderResult>();
			list.add(result2);
			list.add(result3);
			list.add(result1);

			//Execute
			FileUtil.composeFile(list, fileName);
			
			//Assert
			File file = new File(fileName);
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

			assertEquals(part1+part2+part3, loadedText);
		} catch (IOException e) {
			fail();
		}
	}
}
