package ee.ut.xpp2p.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import ee.ut.xpp2p.model.RenderResult;

/**
 * Class that handles file operations
 * 
 * @author Jaan Neljandik
 * @created 09.11.2007
 */
public class FileUtil {

	/**
	 * Loads file contents into byte array
	 * 
	 * @param fileName
	 *            name of the file to load bytes from
	 * @return contents of given file as byte array
	 */
	public static byte[] loadFile(String fileName) throws IOException {
		File file = new File(fileName);
		FileInputStream stream = new FileInputStream(file);

		// Create the byte array to hold the data
		byte[] fileBytes = new byte[(int) file.length()];
		int offset = 0;
		int numRead = 0;

		while (offset < fileBytes.length) {
			numRead = stream.read(fileBytes, offset, fileBytes.length - offset);
			if (numRead >= 0)
				offset += numRead;
		}

		return fileBytes;
	}

	/**
	 * Saves byte array into file
	 * 
	 * @param fileContent
	 *            byte array to save
	 * @param fileName
	 *            name of the file to save content to
	 */
	public static void saveFile(byte[] fileContent, String fileName)
			throws IOException {
		File file = new File(fileName);
		FileOutputStream stream = new FileOutputStream(file);

		stream.write(fileContent);
		stream.flush();
	}

	/**
	 * Composes full file from rendered parts
	 * 
	 * @param partContents
	 *            list of rendered parts
	 * @param fileName
	 *            name of the file to compose
	 */
	public static void composeFile(List<RenderResult> partContents,
			String fileName) throws IOException {
		Collections.sort(partContents);
		
		File file = new File(fileName);
		FileOutputStream stream = new FileOutputStream(file);

		for (RenderResult partContent : partContents) {
			stream.write(partContent.getRenderedPart());
		}

		stream.flush();
	}
}
