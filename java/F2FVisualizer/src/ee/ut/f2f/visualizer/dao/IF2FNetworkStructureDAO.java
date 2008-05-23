package ee.ut.f2f.visualizer.dao;

import net.sourceforge.gxl.GXLDocument;

/**
 * Provides access to F2F network data.
 * 
 * @author Indrek Priks
 */
public interface IF2FNetworkStructureDAO {
	
	/**
	 * Collects data about F2F network topology.
	 * 
	 * This data is well formed GXL document, that contains at most one graph (<code>GXLGraph</code>).
	 * This graph contains unlimited number of nodes (<code>GXLNode</code>)
	 * and unlimited number of edges (<code>GXLEdge</code>) between the
	 * defined nodes. Nodes that are not mentioned in any of the edge definitions
	 * are ignored, as they are not related to the graph.
	 * 
	 * Each node and edge can have unlimited number of attributes (<code>GXLAttr</code>).
	 * The value in the attribute is presented as string (<code>GXLString</code>).
	 * Or if attribute contains collection of values, then value strings are
	 * inside a <code>GXLSet</code> element.
	 * 
	 * Only required attribute (for both - node and edge) is the attribute with
	 * name "connections". This attribute contains a set (<code>GXLSet</code>)
	 * of connection types represented as strings (<code>GXLString</code>).
	 * 
	 * Example structure:
	 * 
	 * <pre>
	 * 		&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
	 * 		&lt;!DOCTYPE gxl SYSTEM &quot;http://www.gupro.de/GXL/gxl-1.0.dtd&quot;&gt;
	 * 		&lt;gxl&gt;
	 * 			&lt;graph id=&quot;graph1&quot;&gt;
	 * 				&lt;node id=&quot;node1&quot;&gt;
	 * 					&lt;attr name=&quot;connections&quot;&gt;
	 * 						&lt;set&gt;
	 * 							&lt;string&gt;MSN&lt;/string&gt;
	 * 							&lt;string&gt;Skype&lt;/string&gt;
	 * 						&lt;/set&gt;
	 * 					&lt;/attr&gt;
	 * 				&lt;/node&gt;
	 * 				&lt;node id=&quot;node2&quot;&gt;
	 * 					&lt;attr name=&quot;connections&quot;&gt;
	 * 						&lt;set&gt;
	 * 							&lt;string&gt;MSN&lt;/string&gt;
	 * 						&lt;/set&gt;
	 * 					&lt;/attr&gt;
	 * 				&lt;/node&gt;
	 * 				&lt;node id=&quot;node3&quot;&gt;
	 * 					&lt;attr name=&quot;connections&quot;&gt;
	 * 						&lt;set&gt;
	 * 							&lt;string&gt;Skype&lt;/string&gt;
	 * 						&lt;/set&gt;
	 * 					&lt;/attr&gt;
	 * 				&lt;/node&gt;
	 * 				&lt;edge from=&quot;node1&quot; to=&quot;node2&quot;&gt;
	 * 					&lt;attr name=&quot;connections&quot;&gt;
	 * 						&lt;set&gt;
	 * 							&lt;string&gt;MSN&lt;/string&gt;
	 * 						&lt;/set&gt;
	 * 					&lt;/attr&gt;
	 * 				&lt;/edge&gt;
	 * 				&lt;edge from=&quot;node1&quot; to=&quot;node3&quot;&gt;
	 * 					&lt;attr name=&quot;connections&quot;&gt;
	 * 						&lt;set&gt;
	 * 							&lt;string&gt;Skype&lt;/string&gt;
	 * 						&lt;/set&gt;
	 * 					&lt;/attr&gt;
	 * 				&lt;/edge&gt;
	 * 			&lt;/graph&gt;
	 * 		&lt;/gxl&gt;
	 * </pre>
	 * 
	 * @return F2F network topology structure in GXL format.
	 * @see net.sourceforge.gxl.GXLGraph
	 * @see net.sourceforge.gxl.GXLNode
	 * @see net.sourceforge.gxl.GXLEdge
	 * @see net.sourceforge.gxl.GXLAttr
	 * @see net.sourceforge.gxl.GXLSet
	 * @see net.sourceforge.gxl.GXLString
	 */
	GXLDocument getGXLDocument();
	
}
