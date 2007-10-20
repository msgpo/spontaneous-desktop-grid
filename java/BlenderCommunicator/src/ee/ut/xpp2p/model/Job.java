package ee.ut.xpp2p.model;

/**
 * Class that represents one rendering job
 * @author Jaan Neljandik
 * @created 20.10.2007
 */
public class Job {
	
	private String inputFile;
	private String outputLocation;
	private String outputFormat; 
	private long startFrame;
	private long endFrame;
	private int participants;
	
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
	public String getOutputFormat() {
		return outputFormat;
	}
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
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
}
