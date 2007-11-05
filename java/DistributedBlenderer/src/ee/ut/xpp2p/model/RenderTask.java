package ee.ut.xpp2p.model;

/**
 * Class that represents one rendering task
 * @author Jaan Neljandik
 * @created 20.10.2007
 */
public class RenderTask {
	
	private String inputFile;
	private String outputLocation;
	private String fileFormat;
	private long startFrame; 
	private long endFrame;
	
	public String getInputFile() {
		return inputFile;
	}
	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
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
}
