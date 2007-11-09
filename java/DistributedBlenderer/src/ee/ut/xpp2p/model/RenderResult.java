package ee.ut.xpp2p.model;

/**
 * @author Jaan Neljandik
 * @created 09.11.2007
 */
public class RenderResult implements Comparable {

	private byte[] renderedPart;
	private long startFrame;
	private long endFrame;

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

	@Override
	public int compareTo(Object obj) {
		RenderResult resultToCompare = (RenderResult) obj;
		if (this.getStartFrame() < resultToCompare.getStartFrame()) {
			return -1;
		} else if (this.getStartFrame() > resultToCompare.getStartFrame()) {
			return 1;
		} else {
			return 0;
		}
	}
}
