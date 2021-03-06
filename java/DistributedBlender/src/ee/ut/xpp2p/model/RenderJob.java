package ee.ut.xpp2p.model;

import java.io.File;
import java.io.IOException;

import ee.ut.xpp2p.util.FileUtil;

/**
 * Class that represents one rendering job
 * 
 * @author Jaan Neljandik
 * @created 20.10.2007
 */
public class RenderJob {

	private String inputFileName = null;
	private byte[] inputFile = null;
	private String outputLocation = null;
	private String outputFormat = null;
	private long startFrame;
	private long endFrame;
	private int participants;
	private String extension;

	/**
	 * Returns the absolute pathname.
	 */
	public String getInputFileName() {
		return inputFileName;
	}

	public void setInputFileName(String fileName) throws IOException
	{
		inputFileName = new File(fileName).getName();
		inputFile = FileUtil.loadFile(fileName);
	}

	public byte[] getInputFile()
	{
		return inputFile;
	}
	
	public String getOutputLocation() {
		return outputLocation;
	}

	public void setOutputLocation(String outputLocation) {
		this.outputLocation = outputLocation;
		if (!this.outputLocation.endsWith(File.separator)) this.outputLocation += File.separator;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
		this.setExtension();
	}

	public long getStartFrame() {
		return startFrame;
	}

	public void setStartFrame(long startFrame) {
		this.startFrame = startFrame;
	}

	public long getEndFrame() {
		return endFrame;
	}
	
	public void setEndFrame(long endFrame) {
		this.endFrame = endFrame;
	}

	public int getParticipants() {
		return participants;
	}

	public void setParticipants(int participants) {
		this.participants = participants;
	}

	public String getExtension() {
		return extension;
	}
	
	private void setExtension() {
		if (this.outputFormat.equals("AVIJPEG")){
			this.extension = "avi";
		}
		else if (this.outputFormat.equals("TGA")){
			this.extension = "tga";
		}
		else if (this.outputFormat.equals("IRIS")){
			this.extension = "rgb";
		}
		else if (this.outputFormat.equals("HAMX")){
			this.extension = "tga";
		}
		else if (this.outputFormat.equals("FTYPE")){
			this.extension = "tga";
		}
		else if (this.outputFormat.equals("JPEG")){
			this.extension = "jpg";
		}
		else if (this.outputFormat.equals("MOVIE")){
			this.extension = "avi";
		}
		else if (this.outputFormat.equals("IRIZ")){
			this.extension = "rgb";
		}
		else if (this.outputFormat.equals("RAWTGA")){
			this.extension = "tga";
		}
		else if (this.outputFormat.equals("AVIRAW")){
			this.extension = "avi";
		}
		else if (this.outputFormat.equals("PNG")){
			this.extension = "png";
		}
		else if (this.outputFormat.equals("BMP")){
			this.extension = "bmp";
		}
		//TODO: FRAMESERVER format seems to take forever to start rendering
	}
}

