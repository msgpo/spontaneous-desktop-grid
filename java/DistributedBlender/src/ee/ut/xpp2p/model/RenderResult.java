package ee.ut.xpp2p.model;

import java.io.Serializable;

/**
 * @author Jaan Neljandik, Vladimir Ðkarupelov
 * @created 09.11.2007
 */
public class RenderResult implements Comparable<RenderResult>, Serializable {

	private static final long serialVersionUID = -3646642611510621567L;
	
	private byte[] renderedPart;
	private long startFrame;
	private long endFrame;
	private String fileName;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getRenderedPart() {
		return renderedPart;
	}

	public void setRenderedPart(byte[] renderedPart) {
		this.renderedPart = renderedPart;
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

	public int compareTo(RenderResult obj) {
		if (this.getStartFrame() < obj.getStartFrame()) {
			return -1;
		} else if (this.getStartFrame() > obj.getStartFrame()) {
			return 1;
		} else {
			return 0;
		}
	}
}
