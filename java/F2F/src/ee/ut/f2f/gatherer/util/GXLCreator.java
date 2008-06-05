package ee.ut.f2f.gatherer.util;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import ee.ut.f2f.gatherer.model.WindowsAttributes;
import ee.ut.f2f.gatherer.rmi.GXLConstants;

import net.sourceforge.gxl.GXLAttr;
import net.sourceforge.gxl.GXLBool;
import net.sourceforge.gxl.GXLCompositeValue;
import net.sourceforge.gxl.GXLDocument;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLElement;
import net.sourceforge.gxl.GXLGXL;
import net.sourceforge.gxl.GXLGraph;
import net.sourceforge.gxl.GXLGraphElement;
import net.sourceforge.gxl.GXLIDGenerator;
import net.sourceforge.gxl.GXLNode;
import net.sourceforge.gxl.GXLSet;
import net.sourceforge.gxl.GXLString;
import net.sourceforge.gxl.GXLValue;

/**
 * 
 * @author Raido Türk
 *
 */
public class GXLCreator {
	
	public static GXLDocument createGXLDocumentWithoutNodes() {
		GXLDocument doc = new GXLDocument();
		GXLIDGenerator idGenerator = new GXLIDGenerator(doc);
		GXLGraph graph = new GXLGraph(idGenerator.generateGraphID());
		doc.getDocumentElement().add(graph);
		return doc;
	}
	
	public static GXLDocument addAttributes(GXLDocument doc,Properties attributes) {
		GXLGraph graph = extractGXLGraph(doc);
		if(graph != null) {
			GXLNode node = (GXLNode)graph.getChildAt(0);
			for (Enumeration<Object> en = attributes.keys(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				String val = attributes.getProperty(key);
				node.setAttr(key, new GXLString(val));
			}
		}
		return doc;
	}
		
	public static GXLDocument createGXLDocument(String nodeName) {
		GXLDocument doc = new GXLDocument();
		GXLIDGenerator idGenerator = new GXLIDGenerator(doc);
		GXLGraph graph = new GXLGraph(idGenerator.generateGraphID());
		GXLNode node = new GXLNode(nodeName);
		graph.add(node);
		doc.getDocumentElement().add(graph);
		return doc;
	}
	
	/**
	 * 
	 * @param doc
	 * @param atr
	 * @return
	 */
	public static GXLDocument addWindowsAPIAttributes(GXLDocument doc, WindowsAttributes atr) {
		GXLGraph graph = extractGXLGraph(doc);
		GXLNode node = null;
		for(int i = 0; i < graph.getGraphElementCount(); i++) {
			GXLGraphElement el = (GXLGraphElement) graph.getGraphElementAt(i);
			if (el instanceof GXLNode) {
				node = (GXLNode)el;
			}
		}
		try {
			//iterate over all Windows API declared methods
			for(int i = 0;i < atr.getClass().getDeclaredMethods().length; i++) {
				Method method = atr.getClass().getDeclaredMethods()[i];
				Object val = method.invoke(atr, (Object[])null); 
				node.setAttr(method.getName().replaceFirst("get", "").toLowerCase(), new GXLString(String.valueOf(val)));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return doc;
	}
	
    /**
     * Extracts the first GXLGraph element from the GXLDocument.
     * 
     * @param doc GXLDocument to be extracted from
     * @return GXLGraph object if GXLDocment contains graphs, null otherwise
     */
	public static GXLGraph extractGXLGraph(GXLDocument doc) {
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
	 * Searches for a node from GXLDocument
	 * @param doc
	 * @param nodeName node to be searched
	 * @return GXLNode object if GXLDocument contains node, null otherwise
	 */
	public static GXLNode findNodeFromDoc(GXLDocument doc, String nodeName) {
		GXLGraph graph = extractGXLGraph(doc);
		if(graph != null) {
			for (int i = 0; i < graph.getGraphElementCount(); i++) {
				GXLGraphElement el = (GXLGraphElement) graph.getGraphElementAt(i);
				if (el instanceof GXLNode) {
					GXLNode node = (GXLNode) el;
					if(nodeName != null && nodeName.equals(node.getID()))
							return node;
				}
			}
		}
		return null;
	}
	
	/**
	 * Adds connections with ancestor and bandwidth data to corresponding nodes.
	 * @param doc
	 * @param bwInfo
	 * @param ancestor
	 * @param connectionsWithAncestor
	 * @return
	 */
	public static GXLDocument addConnectionsAndBandwidthsToGraph(UUID localPeer, GXLDocument doc, Map<UUID,String> bwInfo, UUID ancestor, List<String> connectionsWithAncestor ){
		if(connectionsWithAncestor == null || connectionsWithAncestor.size() == 0)
			return doc;
		GXLGraph graph = extractGXLGraph(doc);
		if (graph != null) {
			for(Map.Entry<UUID, String> entry : bwInfo.entrySet()) {
				UUID id = entry.getKey();
				String bwValue = entry.getValue();
				GXLNode node = addFriendConnectionWBandwidthGXLNode(localPeer.toString(), id.toString(), ancestor != null && id.equals(ancestor) ? connectionsWithAncestor : null, bwValue);
				graph.add(node);
			}
		}
		return doc;
	}
	
	/**
	 * Creates a node with bandwidth and connection information
	 * @param nodeName
	 * @param connections
	 * @param bandwidth
	 * @return
	 */
	private static GXLNode addFriendConnectionWBandwidthGXLNode(String localPeer, String nodeName, List<String> connections, String bandwidth) {
		GXLNode node = new GXLNode(nodeName);
		if(connections != null) {
			GXLCompositeValue cons = new GXLSet();
			for(String con : connections) {
				cons.add(new GXLString(con));
			}
			GXLAttr attr = new GXLAttr(GXLConstants.ATTR_NAME_CONNECTION.getName(), cons);
			node.add(attr);
		}
		GXLAttr bw = new GXLAttr(GXLConstants.ATTR_NAME_BANDWIDTH.getName(), new GXLString(bandwidth));
		node.add(bw);
		GXLAttr isNotRealFriendAttr = new GXLAttr(Constants.IS_DUMMY_NODEINFO_ATTR.getName(), new GXLBool(true));
		node.add(isNotRealFriendAttr);
		GXLAttr edgeWithWhom = new GXLAttr(Constants.EDGE_WITH_WHOM_ATTR.getName(), new GXLString(localPeer));
		node.add(edgeWithWhom);
		return node;
	}
	
	/**
	 * Creates a new node from existing node.
	 * @param oldNode
	 * @return
	 */
	public static GXLNode createNewNodeFromExisting(GXLNode oldNode) {
		GXLNode node = new GXLNode(oldNode.getID());
		int attrCount = oldNode.getAttrCount();
		for(int i = 0; i < attrCount; i++) {
			GXLAttr attribute = oldNode.getAttrAt(i);
			if (!Constants.IS_DUMMY_NODEINFO_ATTR.equals(attribute.getName())
					&& !Constants.EDGE_WITH_WHOM_ATTR.equals(attribute.getName())) {
				GXLValue newValue = (GXLValue) clone(attribute.getValue());
				node.setAttr(attribute.getName(), newValue);
			}
		}
		return node;
	}
	
	/**
	 * Creates clone from existing GXLElement
	 * @param value - GXLElement to be cloned
	 * @return new GXLElement with same parameter values as method parameter GXLElement
	 */
	private static GXLElement clone(GXLElement value) {
		if (value == null)  {
			return null;
		}
		if(value instanceof GXLString) {
			return new GXLString(((GXLString)value).getValue());
		} else if(value instanceof GXLSet) {
			GXLSet set = new GXLSet();
			GXLSet oldSet = (GXLSet) value;
			for(int i = 0; i < oldSet.getChildCount(); i++) {
				GXLElement el = oldSet.getChildAt(i);
				set.add(clone(el));
			}
			return set;
		} else if(value instanceof GXLAttr) {
			GXLAttr oldAttr = (GXLAttr) value;
			return  new GXLAttr(oldAttr.getName(), (GXLValue)clone(oldAttr.getValue()));
		} else if(value instanceof GXLBool) {
			GXLBool oldBool = (GXLBool) value;
			return new GXLBool(oldBool.getBooleanValue());
		}
		
		throw new RuntimeException("wrong type of element: "+value.getClass().getName());
	}
	
	
	
	public static GXLDocument assembleNodeToRootDocument(GXLDocument rootDoc, Map<UUID, GXLDocument> data) {
		GXLGraph rootGraph = extractGXLGraph(rootDoc);
		Map<String, GXLNode> realNodes = new HashMap<String, GXLNode>();
		for(Map.Entry<UUID, GXLDocument> doc : data.entrySet()) {
			GXLGraph graph = extractGXLGraph(doc.getValue());
			if(graph != null) {
				int count = graph.getGraphElementCount();
				for(int i = 0; i < count; i++) {
					GXLGraphElement el = (GXLGraphElement) graph.getGraphElementAt(i);
					if(el instanceof GXLNode) {
						GXLNode node = (GXLNode) el;
						GXLAttr isDummyNode = node.getAttr(Constants.IS_DUMMY_NODEINFO_ATTR.getName());
						//add data from dummy node information to edge
						GXLAttr edgeWithWhom = node.getAttr(Constants.EDGE_WITH_WHOM_ATTR.getName());
						GXLNode newNode = createNewNodeFromExisting(node);
						
						GXLNode realNode = null;
						if(edgeWithWhom != null) {
							realNode = realNodes.get(((GXLString)edgeWithWhom.getValue()).getValue());
							System.out.println("Trying to build up edge from: "+node.getID()+" to "+((GXLString)edgeWithWhom.getValue()).getValue());
						}
						if(isDummyNode != null && realNode != null) {//dummy node, carrying connection information
							if(findEdgeBetweenCurrentNodes(rootGraph, realNode.getID(), newNode.getID())) 
								continue;
							if(!currentNamedNodeExists(newNode.getID(),rootGraph)) {
								rootGraph.add(newNode);
							}
							GXLEdge edge = new GXLEdge(realNode, newNode);
							GXLAttr conn = (GXLAttr)clone(newNode.getAttr(GXLConstants.ATTR_NAME_CONNECTION.getName()));
							if(conn != null) {
								edge.add(conn);
								newNode.remove(conn);
							}
							GXLAttr bw = (GXLAttr)clone(newNode.getAttr(GXLConstants.ATTR_NAME_BANDWIDTH.getName()));
							if(bw != null) {
								edge.add(bw);
								newNode.remove(bw);
							}
							rootGraph.add(edge);
						} else { //real node information
							realNodes.put(newNode.getID(), newNode);
							GXLNode existingNode = findDummyNode(newNode.getID(), rootGraph);
							if(existingNode != null)
								rootGraph.remove(existingNode);
							rootGraph.add(newNode);
						}
					}
				}
			}
		}
		removeUnNecessaryEdges(rootGraph);
		return rootDoc;
	}
	
	private static boolean findEdgeBetweenCurrentNodes(GXLGraph currentDoc, String from, String to) {
		int count = currentDoc.getChildCount();
		for(int i = 0; i < count; i++) {
			GXLGraphElement el = (GXLGraphElement) currentDoc.getGraphElementAt(i);
			if(el instanceof GXLEdge) {
				GXLEdge edge = (GXLEdge) el;
				GXLAttr cons = edge.getAttr(GXLConstants.ATTR_NAME_CONNECTION.getName());
				if (from.equals(edge.getSourceID())
						&& to.equals(edge.getTargetID())
						|| from.equals(edge.getTargetID())
						&& to.equals(edge.getSourceID()) && cons != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * If we are trying to insert real node into document, dummy node has to be found.
	 * @param id
	 * @param rootGraph
	 * @return
	 */
	private static GXLNode findDummyNode(String id, GXLGraph rootGraph) {
		int count = rootGraph.getChildCount();
		for(int i = 0; i < count; i++) {
			GXLElement el = rootGraph.getChildAt(i);
			if(el instanceof GXLNode) {
				GXLNode node = (GXLNode) el;
				if(id.equals(node.getID()))
					return node;
			}
		}
		return null;
	}
	
	/**
	 * Remove those edges, that don't have connection information, they are unnecessary
	 * @param rootGraph
	 * @return
	 */
	private static GXLGraph removeUnNecessaryEdges(GXLGraph rootGraph) {
		if(rootGraph != null) {
			int count = rootGraph.getChildCount();
			System.out.println("final doc child count: "+count);
			for(int i = 0; i < count; i++) {
				GXLElement el = rootGraph.getChildAt(i);
				if(el instanceof GXLEdge) {
					GXLEdge edge = (GXLEdge) el;
					GXLAttr conn = edge.getAttr(GXLConstants.ATTR_NAME_CONNECTION.getName());
					if(conn == null) {
						rootGraph.remove(edge);
						count--;
						i--;
					}
				}
			}
		}
		return rootGraph;
	}
	
	/**
	 * Checks if current named node already exists in GXLGraph
	 * @param id
	 * @param rootGraph
	 * @return
	 */
	private static boolean currentNamedNodeExists(String id, GXLGraph rootGraph) {
		int count = rootGraph.getGraphElementCount();
		for(int i = 0; i < count; i++) {
			GXLGraphElement el = (GXLGraphElement) rootGraph.getGraphElementAt(i);
			if(el instanceof GXLNode) {
				GXLNode node = (GXLNode) el;
				if(id.equals(node.getID()))
					return true;		
			}
		}
		return false;
	}
	
	public static GXLDocument extractConnectionsIntoExistingDoc(GXLDocument currentDoc, GXLDocument receivedDoc) throws Exception{
		GXLGraph receivedGraph = extractGXLGraph(receivedDoc);
		GXLGraph currentDocGraph = extractGXLGraph(currentDoc);
		int count = receivedGraph.getGraphElementCount();
		for(int i = 0; i < count; i++) {
			GXLGraphElement el = (GXLGraphElement) receivedGraph.getGraphElementAt(i);
			if(el instanceof GXLNode) {
				GXLNode node = (GXLNode) el;
				GXLAttr cons = node.getAttr(GXLConstants.ATTR_NAME_CONNECTION.getName());
				if(cons != null) {
					try {
					GXLAttr toWhom = node.getAttr(Constants.EDGE_WITH_WHOM_ATTR.getName());
					System.out.println("edge between "+node.getID()+" and "+((GXLString)toWhom.getValue()).getValue());
					GXLNode oldNode = findNodeFromDoc(currentDoc,node.getID());
					GXLAttr oldCons = oldNode.getAttr(GXLConstants.ATTR_NAME_CONNECTION.getName());
					GXLNode clonedNode = null;
					if(oldCons != null) {
						System.out.println("leidsin vanad connectionid ka");
						clonedNode = createNewNodeFromNewAndExistingConnections(oldNode,node);
					} else {
						clonedNode = cloneConnectionsNode(node);
					}
					currentDocGraph.remove(oldNode);
					currentDocGraph.add(clonedNode);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return currentDoc;
	}
	
	private static GXLNode cloneConnectionsNode(GXLNode oldNode) {
		GXLNode node = new GXLNode(oldNode.getID());
		int attrCount = oldNode.getAttrCount();
		for(int i = 0; i < attrCount; i++) {
			GXLAttr attribute = oldNode.getAttrAt(i);
			GXLValue newValue = (GXLValue) clone(attribute.getValue());
			node.setAttr(attribute.getName(), newValue);
		}
		return node;
	}
	
	/**
	 * If there already exists a connection between these current nodes, then merge their connections and find average bandwidth to new node
	 * @param existingNode
	 * @param newNode
	 * @return
	 */
	private static GXLNode createNewNodeFromNewAndExistingConnections(GXLNode existingNode, GXLNode newNode) {
		Map<String,String> connections = new HashMap<String,String>();
		GXLAttr existingConnections = existingNode.getAttr(GXLConstants.ATTR_NAME_CONNECTION.getName());
		GXLAttr existingBandwidth = existingNode.getAttr(GXLConstants.ATTR_NAME_BANDWIDTH.getName());
		GXLAttr newConnections = newNode.getAttr(GXLConstants.ATTR_NAME_CONNECTION.getName());
		GXLAttr newBandwidth = newNode.getAttr(GXLConstants.ATTR_NAME_BANDWIDTH.getName());
		extractConnectionsFromNode(connections, existingConnections);
		extractConnectionsFromNode(connections, newConnections);
		GXLNode createdNode = new GXLNode(newNode.getID());
		GXLCompositeValue cons = new GXLSet();
		for(String con : connections.values()) {
			cons.add(new GXLString(con));
		}
		GXLAttr attr = new GXLAttr(GXLConstants.ATTR_NAME_CONNECTION.getName(), cons);
		createdNode.add(attr);
		long oldValue = Long.valueOf(((GXLString)existingBandwidth.getValue()).getValue()).longValue();
		long newValue = Long.valueOf(((GXLString)newBandwidth.getValue()).getValue()).longValue();
		GXLAttr bw = new GXLAttr(GXLConstants.ATTR_NAME_BANDWIDTH.getName(), new GXLString(String.valueOf((oldValue+newValue)/2)));
		createdNode.add(bw);
		
		return createdNode;
	}
	
	private static void extractConnectionsFromNode(Map<String,String> map, GXLAttr connections) {
		GXLSet set = (GXLSet)connections.getValue();
		for(int i = 0; i < set.getChildCount(); i++) {
			GXLString el = (GXLString)set.getChildAt(i);
			map.put(el.getValue(), el.getValue());
		}
	}
	
}
