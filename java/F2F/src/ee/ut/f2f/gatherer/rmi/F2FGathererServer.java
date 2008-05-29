package ee.ut.f2f.gatherer.rmi;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Properties;

import ee.ut.f2f.gatherer.DataGathering;
import ee.ut.f2f.util.logging.Logger;

import net.sourceforge.gxl.GXLCompositeValue;
import net.sourceforge.gxl.GXLDocument;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLGraph;
import net.sourceforge.gxl.GXLIDGenerator;
import net.sourceforge.gxl.GXLNode;
import net.sourceforge.gxl.GXLSet;
import net.sourceforge.gxl.GXLString;

public class F2FGathererServer implements IF2FGatherer {

	private static final Logger logger = Logger.getLogger(F2FGathererServer.class);

	public F2FGathererServer() throws RemoteException{
		super();
	}

	public byte[] getWholeF2FTopologyGXL() throws Exception{
		GXLDocument doc = DataGathering.getInstance().gatherAllData();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		doc.write(oos);
		return out.toByteArray();
	}

	public static void main(String[] args) throws Exception{
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
		try {
			IF2FGatherer server = new F2FGathererServer();
			IF2FGatherer stub =
                (IF2FGatherer) UnicastRemoteObject.exportObject(server, 0);
			LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind(IF2FGatherer.RMI_BIND_NAME, stub);
			System.out.println("Server started...");
		}catch (ExportException e){
			logger.error("Binding object to registry failed, probably another process is using the same port: ",e);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	
	private static final String ATTR_NAME_CONNECTION = "connections";
	private static final int NODE_COUNT = 510;
	private static final int MAX_EDGES_PER_NODE = 2;
	
	
	/**
	 * Dummy implementation for getting the F2F network topology data.
	 * 
	 * Only useful for testing purposes.
	 * 
	 * @return Generated GXLDocument.
	 */
	public GXLDocument getGXLDocument() {
		debug("Generating dummy F2F network topology data ....");
		GXLDocument doc = null;
		if (true) {
			doc = generateGXLDocumentDynamically(NODE_COUNT, MAX_EDGES_PER_NODE);
		}
		else {
			doc = getFixedStructureGXLDocument();
		}
		debug("Dummy F2F network topology data generated.");
		return doc;
	}
	
	private void debug(String s) {
		System.out.println(s);
	}
	
	/**
	 * Generates a programmatically fixed GXLDocument.
	 * 
	 * Useful for testing purposes.
	 * 
	 * @return Generated GXLDocument
	 */
	private GXLDocument getFixedStructureGXLDocument() {
		debug("getFixedStructureGXLDocument");
		// Create document and elements
		GXLDocument gxlDocument = new GXLDocument();
		GXLIDGenerator idGenerator = new GXLIDGenerator(gxlDocument);
		GXLGraph graph = new GXLGraph(idGenerator.generateGraphID());
		// Create nodes
		GXLNode node1 = new GXLNode(idGenerator.generateNodeID());
		GXLNode node2 = new GXLNode(idGenerator.generateNodeID());
		GXLNode node3 = new GXLNode(idGenerator.generateNodeID());
		GXLNode node4 = new GXLNode(idGenerator.generateNodeID());
		GXLNode node5 = new GXLNode(idGenerator.generateNodeID());
		// Add extra information to nodes
		Properties props = System.getProperties();
		for (Enumeration<Object> en = props.keys(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			String val = System.getProperty(key);
			// debug(key + "->" + val);
			node1.setAttr(key, new GXLString(val));
			node2.setAttr(key, new GXLString(val));
			node3.setAttr(key, new GXLString(val));
			node4.setAttr(key, new GXLString(val));
			node5.setAttr(key, new GXLString(val));
		}
		// Build the tree structure
		graph.add(node1);
		graph.add(node2);
		graph.add(node3);
		graph.add(node4);
		graph.add(node5);
		// Bind nodes
		GXLEdge edge1 = new GXLEdge(node1, node2);
		GXLEdge edge2 = new GXLEdge(node1, node3);
		GXLEdge edge3 = new GXLEdge(node2, node3);
		GXLEdge edge4 = new GXLEdge(node1, node4);
		graph.add(edge1);
		graph.add(edge2);
		graph.add(edge3);
		graph.add(edge4);
		//
		gxlDocument.getDocumentElement().add(graph);
		return gxlDocument;
	}
	
	/**
	 * Generates dynamically a <code>GXLDocument<code>.
	 * 
	 * Useful for testing purposes.
	 * 
	 * There will be <code>node_count</code> of nodes. 
	 * Each node randomly selects <code>max_edges_per_node</code> of new edges for itself.
	 * Thus, total edges per node can be more than max_edges_per_node defines!
	 * Each edge randomly selects connection types for itself.
	 * 
	 * @param NODE_COUNT number of nodes
	 * @param MAX_EDGES_PER_NODE number of edges each node selects for itself
	 * @return Generated GXLDocument.
	 */
	private GXLDocument generateGXLDocumentDynamically(final int NODE_COUNT, final int MAX_EDGES_PER_NODE) {
		debug("generateGXLDocumentDynamically");
		// Create document and elements
		GXLDocument gxlDocument = new GXLDocument();
		GXLIDGenerator idGenerator = new GXLIDGenerator(gxlDocument);
		GXLGraph graph = new GXLGraph(idGenerator.generateGraphID());
		gxlDocument.getDocumentElement().add(graph);
		
		// Create nodes
		for (int i = 0; i < NODE_COUNT; i++) {
			GXLNode node = new GXLNode(idGenerator.generateNodeID());
			graph.add(node);
			
			// Add extra information to nodes
			Properties props = System.getProperties();
			for (Enumeration<Object> en = props.keys(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				String val = System.getProperty(key);
				// debug(key + "->" + val);
				node.setAttr(key, new GXLString(val));
				
				GXLCompositeValue composite = new GXLSet();
				node.setAttr(ATTR_NAME_CONNECTION, composite);
			}
			
		}
		
		// Bind nodes
		for (int i = 0; i < NODE_COUNT; i++) {
			int connectionCount = getRandom(MAX_EDGES_PER_NODE, 0);
			int[] connections = new int[connectionCount + 1];
			connections[connectionCount] = i;
			GXLNode node1 = (GXLNode) graph.getGraphElementAt(i);
			
			for (int j = 0; j < connections.length; j++) {
				GXLNode node2 = (GXLNode) graph.getGraphElementAt(getRandom(NODE_COUNT, connections));
				
				GXLEdge edge = new GXLEdge(node1, node2);
				graph.add(edge);
				
				GXLCompositeValue cons = generateConnections();
				edge.setAttr(ATTR_NAME_CONNECTION, cons);
				
				addMissingConnectionTypes(cons, (GXLCompositeValue) node1.getAttr(ATTR_NAME_CONNECTION).getValue());
				addMissingConnectionTypes(cons, (GXLCompositeValue) node2.getAttr(ATTR_NAME_CONNECTION).getValue());
			}
		}
		
		return gxlDocument;
	}
	
	private void addMissingConnectionTypes(GXLCompositeValue from, GXLCompositeValue to) {
		for (int k = 0; k < from.getValueCount(); k++) {
			GXLString val = (GXLString) from.getValueAt(k);
			if (to.indexOf(val) < 0) {
				GXLString newVal = new GXLString(val.getValue());
				to.add(newVal);
			}
		}
	}
	
	/**
	 * Generates random connection type sets
	 */
	private GXLCompositeValue generateConnections() {
		GXLCompositeValue composite = new GXLSet();
		int types = getRandom(11, 0);// number of cases, and not 0.
		switch (types) {
		// Single connection types
		case 1:
			composite.add(new GXLString("MSN"));
			break;
		case 2:
			composite.add(new GXLString("Yahoo"));
			break;
		case 3:
			composite.add(new GXLString("Skype"));
			break;
		case 4:
			composite.add(new GXLString("AOL"));
			break;
		// Two connection types
		case 5:
			composite.add(new GXLString("MSN"));
			composite.add(new GXLString("Yahoo"));
			break;
		case 6:
			composite.add(new GXLString("MSN"));
			composite.add(new GXLString("Skype"));
			break;
		case 7:
			composite.add(new GXLString("MSN"));
			composite.add(new GXLString("AOL"));
			break;
		case 8:
			composite.add(new GXLString("Yahoo"));
			composite.add(new GXLString("Skype"));
			break;
		case 9:
			composite.add(new GXLString("Skype"));
			composite.add(new GXLString("AOL"));
			break;
		// Three connection types
		case 10:
			composite.add(new GXLString("MSN"));
			composite.add(new GXLString("Yahoo"));
			composite.add(new GXLString("Skype"));
			break;
		// Four connection types
		case 11:
			composite.add(new GXLString("MSN"));
			composite.add(new GXLString("Yahoo"));
			composite.add(new GXLString("Skype"));
			composite.add(new GXLString("AOL"));
			break;
		
		}
		return composite;
	}
	
	private int getRandom(int max, int except) {
		return getRandom(max, new int[] {
			except
		});
	}
	
	private int getRandom(int max, int[] excepts) {
		int result = 0;
		do {
			result = (int) (Math.random() * 100) * max / 100;
			// debug("do:max=" + max + ",result=" + result);
		}
		while (contains(excepts, result));
		return result;
	}
	
	private boolean contains(int[] excepts, int el) {
		for (int i = 0; i < excepts.length; i++) {
			if (excepts[i] == el) {
				return true;
			}
		}
		return false;
	}

}
