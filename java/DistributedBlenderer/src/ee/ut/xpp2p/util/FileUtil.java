package ee.ut.xpp2p.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import ee.ut.xpp2p.model.RenderResult;

/**
 * @author Jaan Neljandik
 * @created 09.11.2007
 */
public class FileUtil {

	public static byte[] loadFile(String fileName) {
		try {
			File file = new File(fileName);
			FileInputStream stream = new FileInputStream(file);

			// Create the byte array to hold the data
			byte[] fileBytes = new byte[(int) file.length()];
			int offset = 0;
			int numRead = 0;

			while (offset < fileBytes.length) {
				numRead = stream.read(fileBytes, offset, fileBytes.length - offset);
				if(numRead >= 0) 
					offset += numRead;
			}

			return fileBytes;
		} catch (IOException e) {
			// TODO: Handle Exception
			return null;
		}
	}
	
	public static void saveFile(byte[] fileContent, String fileName) {
		try {
			File file = new File(fileName);
			FileOutputStream stream = new FileOutputStream(file);

			stream.write(fileContent);
			stream.flush();
			
		} catch (IOException e) {
			// TODO: Handle Exception
		}
	}
	
	public static void composeFile(List<RenderResult> partContents, String fileName) {
		try {
			File file = new File(fileName);
			FileOutputStream stream = new FileOutputStream(file);

			for(RenderResult partContent : partContents) {
				stream.write(partContent.getRenderedPart());
			}
			
			stream.flush();
			
		} catch (IOException e) {
			// TODO: Handle Exception
		}
	}
}
