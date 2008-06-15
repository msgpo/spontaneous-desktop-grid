package ee.ut.f2f.gatherer.parameters;

import net.sourceforge.gxl.GXLDocument;
import ee.ut.f2f.gatherer.model.LinuxAttributes;
import ee.ut.f2f.gatherer.model.WindowsAttributes;
import ee.ut.f2f.gatherer.util.GXLCreator;

/**
 * Linux implementation
 * @author Raido Türk
 *
 */
public class LinuxImpl extends SystemInformation{

	public GXLDocument gatherInformation(String nodeId) {
		GXLDocument doc = GXLCreator.createGXLDocument(nodeId);
		GXLCreator.addAttributes(doc, System.getProperties());
		
		LinuxSoInformation information = new LinuxSoInformation();
		LinuxAttributes atr = information.gatherInformationFromSo();
		
		GXLCreator.addLinuxAttributes(doc, atr);
		
		return doc;
	}
}
