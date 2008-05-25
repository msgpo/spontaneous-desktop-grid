package ee.ut.f2f.visualizer.gxl;

import java.io.File;
import java.io.IOException;

import net.sourceforge.gxl.GXLAttr;
import net.sourceforge.gxl.GXLCompositeValue;
import net.sourceforge.gxl.GXLDocument;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLGXL;
import net.sourceforge.gxl.GXLGraph;
import net.sourceforge.gxl.GXLGraphElement;
import net.sourceforge.gxl.GXLNode;
import net.sourceforge.gxl.GXLString;
import net.sourceforge.gxl.GXLValue;
import ee.ut.f2f.gatherer.rmi.GXLConstants;
import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Contains most of the GXL specific operations that have to be done on the GXL
 * model.
 * 
 * @author Indrek Priks
 */
public class GXLCodec {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(GXLCodec.class);
	/**
	 * Name of the <code>GXLAttr</code> attribute that represents the
	 * connections on <code>GXLNode</code> and <code>GXLEdge</code>.
	 */
	public static final String ATTR_NAME_CONNECTION = GXLConstants.ATTR_NAME_CONNECTION.getName();
	
	/**
	 * Name of the <code>GXLAttr</code> attribute that represents the connection
	 * band-with on <code>GXLEdge</code>.
	 */
	public static final String ATTR_NAME_BANDWITH = GXLConstants.ATTR_NAME_BANDWIDTH.getName();
	
	/**
	 * Reads the GXL document from file.
	 * 
	 * @param file
	 *          File to be read from
	 * @return GXL document
	 */
	public static GXLDocument read(File file) {
		GXLDocument doc = null;
		try {
			doc = new GXLDocument(file);
		}
		catch (Exception e) {
			log.debug("Error while parsing file: " + e);
		}
		debug(doc);
		return doc;
	}
	
	private static void debug(GXLDocument doc) {
		if (!true) {
			if (doc == null) {
				log.debug("GXL document null!");
			}
			else {
				log.debug("GXL document:");
				try {
					doc.write(System.out);
				}
				catch (IOException ioe) {
					log.debug("Error while printing the file to standard output: " + ioe);
				}
			}
		}
	}
	
	/**
	 * Extracts the first GXLGraph element from the GXLDocument.
	 * 
	 * @param doc
	 *          GXLDocument to be extracted from
	 * @return GXLGraph object if GXLDocment contains graphs, null otherwise
	 */
	public static GXLGraph extractGXLGraph(GXLDocument doc) {
		log.debug("extractGXLGraph");
		GXLGraph graph = null;
		if (doc != null) {
			GXLGXL gxl = doc.getDocumentElement();
			int count = gxl.getGraphCount();
			if (count > 0) {
				graph = gxl.getGraphAt(0);
			}
		}
		return graph;
	}
	
	/**
	 * Finds the count of nodes in the GXLDocument.
	 * 
	 * @param doc
	 *          GXLDocument to count nodes in
	 * @return number of nodes in the GXL document
	 */
	public static int getNodesCount(GXLDocument doc) {
		log.debug("getNodesCount");
		int result = 0;
		GXLGraph graph = extractGXLGraph(doc);
		if (graph != null) {
			for (int i = 0; i < graph.getGraphElementCount(); i++) {
				GXLGraphElement el = (GXLGraphElement) graph.getGraphElementAt(i);
				if (el instanceof GXLNode) {
					// log.debug("el:" + el.getID());
					result++;
				}
			}
		}
		return result;
	}
	
	public static int getConnectionBandwith(GXLEdge edge) {
		int b = 0;
		if (edge != null) {
			GXLAttr attr = edge.getAttr(ATTR_NAME_BANDWITH);
			if (attr != null) {
				String val = toString(attr.getValue());
				if (val != null && val.trim().length() > 0) {
					try {
						b = Integer.valueOf(val);
					}
					catch (NumberFormatException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return b;
	}
	
	/**
	 * Returns the GXLValue as a String.
	 * 
	 * If the GXLValue is a GXLCompositeValue then empty String is returned.
	 * 
	 * @param val
	 *          GXLValue to be decoded into a String
	 * @return String representation of the GXLValue. null if can't decode.
	 */
	public static String toString(GXLValue val) {
		if (val != null) {
			if (val instanceof GXLString) {
				return ((GXLString) val).getValue();
			}
			else if (val instanceof GXLCompositeValue) {
				return "";
			}
			else {
				log.debug("Warning! Unmapped type:" + val.getClass().getName());
			}
		}
		return null;
	}
}
