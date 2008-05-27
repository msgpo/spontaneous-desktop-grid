package ee.ut.f2f.gatherer.parameters;

import net.sourceforge.gxl.GXLDocument;
import ee.ut.f2f.gatherer.util.GXLCreator;

public class LinuxImpl extends SystemInformation{

	public GXLDocument gatherInformation(String nodeId) {
		GXLDocument doc = GXLCreator.createGXLDocument(nodeId);
		doc = GXLCreator.addAttributes(doc, System.getProperties());
		
		return doc;
	}
}
