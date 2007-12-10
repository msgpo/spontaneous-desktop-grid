package ee.ut.xpp2p.model;

import java.io.Serializable;

/**
 * Class that represents one rendering task
 * 
 * @author Jaan Neljandik, Vladimir Ðkarupelov
 * @created 20.10.2007
 */
public class RenderTask implements Serializable {

	private static final long serialVersionUID = 2366016649166254834L;
	
	private String fileName;
	private byte[] blenderFile;
	private String outputLocation;
	private String fileFormat;
	private long startFrame;
	private long endFrame;
	private String extension;

	public RenderTask() {
		super();
	}

	public byte[] getBlenderFile() {
		return blenderFile;
	}

	public void setBlenderFile(byte[] blenderFile) {
		this.blenderFile = blenderFile;
	}

	public String getOutputLocation() {
		return outputLocation;
	}

	public void setOutputLocation(String outputLocation) {
		this.outputLocation = outputLocation;
	}

	public String getFileFormat() {
		return fileFormat;
	}

	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getExtension() {
		return extension;
	}

	private void setExtension() {
		if (this.fileFormat.equals("AVIJPEG")) {
			this.extension = "avi";
		} else if (this.fileFormat.equals("TGA")) {
			this.extension = "tga";
		} else if (this.fileFormat.equals("IRIS")) {
			this.extension = "rgb";
		} else if (this.fileFormat.equals("HAMX")) {
			this.extension = "tga";
		} else if (this.fileFormat.equals("FTYPE")) {
			this.extension = "tga";
		} else if (this.fileFormat.equals("JPEG")) {
			this.extension = "jpg";
		} else if (this.fileFormat.equals("MOVIE")) {
			this.extension = "avi";
		} else if (this.fileFormat.equals("IRIZ")) {
			this.extension = "rgb";
		} else if (this.fileFormat.equals("RAWTGA")) {
			this.extension = "tga";
		} else if (this.fileFormat.equals("AVIRAW")) {
			this.extension = "avi";
		} else if (this.fileFormat.equals("PNG")) {
			this.extension = "png";
		} else if (this.fileFormat.equals("BMP")) {
			this.extension = "bmp";
		}
		// TODO: FRAMESERVER format seems to take forever to start rendering
	}
}
