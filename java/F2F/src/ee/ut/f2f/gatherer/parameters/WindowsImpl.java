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
		/**Properties sysProps = System.getProperties();
        Enumeration enProps = sysProps.propertyNames();
        String key = "";
        while ( enProps.hasMoreElements() ) {
            key = (String) enProps.nextElement();
            System.out.println("  " + key + "  ->  " + sysProps.getProperty(key));
        }*/
		
		WinDllInformation information = new WinDllInformation();
		WindowsAttributes atr = information.gatherInformationFromDll();
		
		GXLCreator.addWindowsAPIAttributes(doc, atr);
		
		return doc;
	}

}
