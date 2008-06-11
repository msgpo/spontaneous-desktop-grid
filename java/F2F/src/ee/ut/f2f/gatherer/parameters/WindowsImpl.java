package ee.ut.f2f.gatherer.parameters;

import ee.ut.f2f.gatherer.util.GXLCreator;
import ee.ut.f2f.gatherer.model.WindowsAttributes;
import net.sourceforge.gxl.GXLDocument;

/**
 * Microsoft Windows Implementation
 * @author Raido Türk
 *
 */
public class WindowsImpl extends SystemInformation{
	
	public GXLDocument gatherInformation(String nodeId) {		
		GXLDocument doc = GXLCreator.createGXLDocument(nodeId);
		GXLCreator.addAttributes(doc, System.getProperties());
		
		WinDllInformation information = new WinDllInformation();
		WindowsAttributes atr = information.gatherInformationFromDll();
		
		GXLCreator.addWindowsAPIAttributes(doc, atr);
		
		return doc;
	}

}
