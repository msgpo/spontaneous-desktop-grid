package ee.ut.xpp2p.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import ee.ut.xpp2p.model.RenderResult;
import ee.ut.xpp2p.ui.BlenderFileFilter;

/**
 * Class that handles file operations
 * 
 * @author Jaan Neljandik, Vladimir �karupelov
 * @created 09.11.2007
 */
public class FileUtil {

	private final static String URL_FILE_PREFIX = "file:/";

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
	 * Composes full file from rendered parts, deletes rendered parts if
	 * concatenation was successful
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
		String[] inputFileURLs = new String[partContents.size()];
		for (int i = 0; i < partContents.size(); i++) {
			byte[] partBytes = partContents.get(i).getRenderedPart();
			Properties props = System.getProperties();
			String tempDir = props.getProperty("java.io.tmpdir");
			if (!tempDir.endsWith(File.separator))
			{
				tempDir += File.separator;
			}
			String partName = tempDir + partContents.get(i).getFileName();
			File part = saveFile(partBytes, partName);
			inputFileURLs[i] = URL_FILE_PREFIX + part.getAbsolutePath();
		}
		boolean filesConcatinated = Concat.concatinateVideoFiles(
				URL_FILE_PREFIX + outputLocation + fileName, inputFileURLs);
		if (filesConcatinated)
			deleteFiles(inputFileURLs);
		return filesConcatinated;
	}

	/**
	 * Deletes given files or fileURLs
	 * 
	 * @param partNames
	 *            file names
	 */
	public static void deleteFiles(String... partNames) {
		for (String partName : partNames) {
			if(partName.startsWith(URL_FILE_PREFIX)){
				partName = partName.substring(URL_FILE_PREFIX.length(), partName.length()); //excluses prefix from name
			}
			File file = new File(partName);
			System.out.println("Preparing to delete file " + file);
			boolean fileDeleted = false;
			if (file.exists())
				fileDeleted = file.delete();
			if (fileDeleted) {
				System.out.println("file " + file + " deleted");
			} else {
				System.err.println("Cannot delete file " + file);
			}
		}
	}

	/**
	 * Method returns the same name as blender generates
	 * 
	 * @param startFrame
	 * @param endFrame
	 * @param extension
	 *            file extension
	 * @return file name
	 */
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

	/**
	 * Returns filename by blender file name + extension
	 * 
	 * @param inputBlenderFile
	 *            Blender file
	 * @param extension
	 *            file Extension
	 * @return filename by input blender file name + extension
	 */
	public static String generateOutputFileName(String inputBlenderFile,
			String extension) {
		inputBlenderFile = new File(inputBlenderFile).getName();
		if (inputBlenderFile.toLowerCase().endsWith(
				BlenderFileFilter.BLENDER_EXTENSION))
			inputBlenderFile = inputBlenderFile.substring(0, inputBlenderFile
					.length()
					- BlenderFileFilter.BLENDER_EXTENSION.length() - 1);
		return inputBlenderFile + "." + extension;
	}
}
