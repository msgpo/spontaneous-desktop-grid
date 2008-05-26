package ee.ut.f2f.visualizer.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.gxl.GXLAttr;
import net.sourceforge.gxl.GXLCompositeValue;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLGraphElement;
import net.sourceforge.gxl.GXLValue;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Provides content for NodeInfoView view.
 * 
 * @author Indrek Priks
 */
public class NodeInfoViewContentProvider implements ITreeContentProvider {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(NodeInfoViewContentProvider.class);
	
	/** Empty result */
	private static final Object[] EMPTY = new Object[0];
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	
	public void dispose() {
	}
	
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object parent) {
		log.debug("getElements:" + parent.getClass().getName());
		if (parent instanceof List) {
			List list = (List) parent;
			if (!list.isEmpty()) {
				Object el = list.get(0);
				log.debug("INDREK:" + el.toString());
				if (el instanceof GXLGraphElement) {
					// Sort ascending
					Collections.sort(list, new Comparator<GXLGraphElement>() {
						
						public int compare(GXLGraphElement o1, GXLGraphElement o2) {
							GXLEdge e1 = null;
							GXLEdge e2 = null;
							if (o1 instanceof GXLEdge) {
								e1 = (GXLEdge) o1;
							}
							if (o2 instanceof GXLEdge) {
								e2 = (GXLEdge) o2;
							}
							// GXLEdge-s first, GXLNode-s last.
							if (e1 != null && e2 == null) {
								return -1;
							}
							else if (e1 == null && e2 != null) {
								return 1;
							}
							else if (e1 != null && e2 != null) {
								int result = e1.getSourceID().compareTo(e2.getSourceID());
								if (result == 0) {
									result = e1.getTargetID().compareTo(e2.getTargetID());
								}
								return result;
							}
							return o1.getID().compareTo(o2.getID());
						}
					});
					return list.toArray();
				}
			}
		}
		return EMPTY;// if the input can't be shown
	}
	
	public Object getParent(Object child) {
		// log.debug("getParent:" + child.getClass().getName());
		return null;
	}
	
	public Object[] getChildren(Object parent) {
		// log.debug("getChildren:" + parent.getClass().getName());
		if (parent instanceof GXLGraphElement) {
			GXLGraphElement el = (GXLGraphElement) parent;
			int count = el.getAttrCount();
			if (count > 0) {
				List<GXLAttr> tmp = new ArrayList<GXLAttr>(count);
				for (int i = 0; i < count; i++) {
					tmp.add(el.getAttrAt(i));
				}
				// Sort ascending
				Collections.sort(tmp, new Comparator<GXLAttr>() {
					
					public int compare(GXLAttr o1, GXLAttr o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
				return tmp.toArray();
			}
		}
		else if (parent instanceof GXLAttr) {
			GXLAttr attr = (GXLAttr) parent;
			GXLValue val = attr.getValue();
			if (val instanceof GXLCompositeValue) {
				GXLCompositeValue composite = (GXLCompositeValue) val;
				int count = composite.getValueCount();
				if (count > 0) {
					Object[] values = new Object[count];
					for (int i = 0; i < count; i++) {
						values[i] = composite.getValueAt(i);
					}
					return values;
				}
			}
		}
		return EMPTY;
	}
	
	public boolean hasChildren(Object parent) {
		// log.debug("hasChildren:" + parent.getClass().getName());
		if (parent instanceof GXLGraphElement) {
			GXLGraphElement el = (GXLGraphElement) parent;
			return el.getAttrCount() > 0;
		}
		else if (parent instanceof GXLAttr) {
			GXLAttr attr = (GXLAttr) parent;
			GXLValue val = attr.getValue();
			if (val instanceof GXLCompositeValue) {
				GXLCompositeValue composite = (GXLCompositeValue) val;
				return composite.getValueCount() > 0;
			}
			return false;
		}
		return false;
	}
}