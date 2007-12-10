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
 * @author Jaan Neljandik, Vladimir Ðkarupelov
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
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			// Create the byte array to hold the data
			byte[] fileBytes = new byte[(int) file.length()];
			int offset = 0;
			int numRead = 0;

			while (offset < fileBytes.length) {
				numRead = stream.read(fileBytes, offset, fileBytes.length
						- offset);
				if (numRead >= 0)
					offset += numRead;
			}

			return fileBytes;
		} finally {
			if (stream != null)
				stream.close();
		}
	}

	/**
	 * Saves byte array into file
	 * 
	 * @param fileContent
	 *            byte array to save
	 * @param fileName
	 *            name of the file to save content to
	 * @return created file
	 */
	public static File saveFile(byte[] fileContent, String fileName)
			throws IOException {
		File file = new File(fileName);
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			stream.write(fileContent);
			stream.flush();
			return file;
		} finally {
			if (stream != null)
				stream.close();
		}
	}

	/**
	 * Composes full file from rendered parts
	 * 
	 * @param partContents
	 *            list of rendered parts
	 * @param fileName
	 *            name of the file to compose
	 * @return true if file is created, false otherwise
	 */
	public static boolean composeFile(List<RenderResult> partContents,
			String outputLocation, String fileName) throws IOException {
		Collections.sort(partContents);
		String[] inputFileNames = new String[partContents.size()];
		for (int i = 0; i < partContents.size(); i++) {
			byte[] partBytes = partContents.get(i).getRenderedPart();
			String partName = outputLocation + partContents.get(i).getFileName();
			File part = saveFile(partBytes, partName);
			inputFileNames[i] = "file:/" + part.getAbsolutePath();
		}
		return Concat.concatinateVideoFiles(outputLocation + fileName, inputFileNames);
	}

	// FIXME: Need to be controlelled for frames > 9999 in blender
	public static String generateOutputFileName(long startFrame, long endFrame,
			String extension) {
		String start = numberEnlarger(String.valueOf(startFrame));
		String end = numberEnlarger(String.valueOf(endFrame));
		return start + "_" + end + "." + extension;
	}

	private static String numberEnlarger(String num) {
		while (num.length() < 4) {
			num = "0" + num;
		}
		return num;
	}

}
