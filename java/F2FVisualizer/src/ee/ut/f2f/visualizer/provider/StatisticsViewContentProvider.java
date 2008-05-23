package ee.ut.f2f.visualizer.provider;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.gxl.GXLAttr;
import net.sourceforge.gxl.GXLCompositeValue;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLElement;
import net.sourceforge.gxl.GXLGraph;
import net.sourceforge.gxl.GXLNode;
import net.sourceforge.gxl.GXLValue;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import ee.ut.f2f.visualizer.gxl.GXLCodec;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.GraphEditorInput;
import ee.ut.f2f.visualizer.model.Property;

/**
 * Provides content for StatisticsView view.
 * 
 * @author Indrek Priks
 */
public class StatisticsViewContentProvider implements ITreeContentProvider, PropertyChangeListener {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(StatisticsViewContentProvider.class);
	
	private static final Object[] EMPTY = new Object[0];
	
	private GraphEditorInput input;
	private TreeViewer viewer;
	
	/**
	 * Default constructor.
	 * 
	 * @param viewer
	 *          of StatistcsView view
	 */
	public StatisticsViewContentProvider(TreeViewer viewer) {
		this.viewer = viewer;
	}
	
	public void propertyChange(PropertyChangeEvent arg0) {
		log.debug("propertyChange");
		viewer.refresh();
	}
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (oldInput != null) {
			log.debug("OldInput=");
			GraphEditorInput in = (GraphEditorInput) oldInput;
			in.removeChangeListener(this);
		}
		if (newInput != null) {
			log.debug("NewInput");
			GraphEditorInput in = (GraphEditorInput) newInput;
			in.addChangeListener(this);
		}
		input = (GraphEditorInput) newInput;
	}
	
	public void dispose() {
	}
	
	public Object[] getElements(Object parent) {
		log.debug("getElements:" + parent.getClass().getName());
		if (parent instanceof GraphEditorInput) {
			Property nodesCount = getNodesCountElement();
			Object[] result = new Object[] //
			{
					nodesCount, //
					StatisticsViewConstants.EL_CONNECTIONS, //
					StatisticsViewConstants.EL_NODE_ATTR_STATISTICS
			//
			};
			return result;
		}
		return EMPTY;
	}
	
	public Object getParent(Object child) {
		return null;
	}
	
	public Object[] getChildren(Object parent) {
		// log.debug("getChildren:" + parent.getClass().getName() + ":" +
		// parent.toString());
		if (parent instanceof Integer) {
			Integer i = (Integer) parent;
			switch (i) {
			case StatisticsViewConstants.EL_CONNECTIONS:
				return getChildrenConnections();
			case StatisticsViewConstants.EL_NODE_ATTR_STATISTICS:
				return getChildrenNodeAttrStatistics();
			default:
				return EMPTY;
			}
		}
		return EMPTY;
	}
	
	public boolean hasChildren(Object parent) {
		// log.debug("hasChildren:" + parent.getClass().getName());
		if (parent instanceof Integer) {
			Integer i = (Integer) parent;
			switch (i) {
			case StatisticsViewConstants.EL_NODE_COUNT:
				return false;
			case StatisticsViewConstants.EL_CONNECTIONS:
				return true;
			case StatisticsViewConstants.EL_NODE_ATTR_STATISTICS:
				return true;
			default:
				return false;
			}
		}
		return false;
	}
	
	private Property getNodesCountElement() {
		int count = GXLCodec.getNodesCount(input.getGXLDocument());
		return new Property(StatisticsViewConstants.LABEL_NODES_COUNT, Integer.toString(count));
	}
	
	/**
	 * Returns the children (connection types) for connections element.
	 * 
	 * @return array of Map.Entry<String, Integer> objects.
	 */
	private Object[] getChildrenConnections() {
		log.debug("getChildrenConnections");
		if (input != null) {
			GXLGraph graph = GXLCodec.extractGXLGraph(input.getGXLDocument());
			if (graph != null) {
				Map<String, Integer> connections = calculateConnections(graph);
				return connections.entrySet().toArray();// Entry<String, Integer>
			}
		}
		return EMPTY;
	}
	
	private Map<String, Integer> calculateConnections(GXLGraph graph) {
		Map<String, Integer> connections = new TreeMap<String, Integer>();
		
		if (graph != null) {
			for (int i = 0; i < graph.getChildCount(); i++) {
				GXLElement el = graph.getChildAt(i);
				if (el instanceof GXLEdge) {
					GXLEdge e = (GXLEdge) el;
					GXLAttr a = e.getAttr(GXLCodec.ATTR_NAME_CONNECTION);
					
					String[] types = new String[] {
						StatisticsViewConstants.LABEL_UNKNOWN
					};
					
					if (a != null) {
						GXLCompositeValue cons = (GXLCompositeValue) a.getValue();
						if (cons != null && cons.getValueCount() > 0) {
							types = new String[cons.getValueCount()];
							
							for (int k = 0; k < types.length; k++) {
								GXLValue v = cons.getValueAt(k);
								String type = GXLCodec.toString(v);
								if (type == null) {
									type = StatisticsViewConstants.LABEL_UNKNOWN;
								}
								types[k] = type;
							}
							
						}
					}
					
					for (String type : types) {
						Integer count = connections.get(type);
						if (count == null) {
							count = 0;
						}
						count++;
						// log.debug("Type=" + type + ",count=" + count);
						connections.put(type, count);
					}
					
				}
			}
		}
		return connections;
	}
	
	private Object[] getChildrenNodeAttrStatistics() {
		log.debug("getChildrenNodeAttrStatistics");
		if (input != null) {
			GXLGraph graph = GXLCodec.extractGXLGraph(input.getGXLDocument());
			if (graph != null) {
				Map<String, Integer> attrs = calculateAttributeCounts(graph);
				return attrs.entrySet().toArray(); // Entry<String, Integer>
			}
		}
		return EMPTY;
	}
	
	private Map<String, Integer> calculateAttributeCounts(GXLGraph graph) {
		Map<String, Integer> attrs = new TreeMap<String, Integer>();
		
		if (graph != null) {
			for (int i = 0; i < graph.getChildCount(); i++) {
				GXLElement el = graph.getChildAt(i);
				if (el instanceof GXLNode) {
					GXLNode e = (GXLNode) el;
					for (int j = 0; j < e.getAttrCount(); j++) {
						GXLAttr a = e.getAttrAt(j);
						GXLValue val = a.getValue();
						String prefix = a.getName() + "=";
						
						String[] types = null;
						
						if (val != null) {
							if (val instanceof GXLCompositeValue) {
								GXLCompositeValue composite = (GXLCompositeValue) val;
								types = new String[composite.getValueCount()];
								for (int k = 0; k < composite.getValueCount(); k++) {
									types[k] = prefix + GXLCodec.toString(composite.getValueAt(k));
								}
							}
							else {
								types = new String[] {
									prefix + GXLCodec.toString(val)
								};
							}
						}
						
						if (val == null || types.length == 0) {
							types = new String[] {
								prefix
							};
						}
						
						for (String type : types) {
							Integer count = attrs.get(type);
							if (count == null) {
								count = 0;
							}
							count++;
							// log.debug("Type=" + type + ",count=" + count);
							attrs.put(type, count);
						}
						
					}
				}
			}
		}
		return attrs;
	}
	
}
