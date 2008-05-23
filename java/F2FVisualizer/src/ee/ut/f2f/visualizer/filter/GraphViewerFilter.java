package ee.ut.f2f.visualizer.filter;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import net.sourceforge.gxl.GXLAttr;
import net.sourceforge.gxl.GXLCompositeValue;
import net.sourceforge.gxl.GXLGraphElement;
import net.sourceforge.gxl.GXLValue;
import ee.ut.f2f.visualizer.gxl.GXLCodec;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.Property;
import ee.ut.f2f.visualizer.model.PropertyFilter;

/**
 * Filter that provides for filtering out these graph elements that are assigned
 * this filter to be passed or rejected.
 * 
 * @author Indrek Priks
 */
public class GraphViewerFilter extends ViewerFilter {
	
	private static final F2FLogger log = new F2FLogger(GraphViewerFilter.class);
	private Collection<PropertyFilter> passFilters = new ArrayList<PropertyFilter>();
	private Collection<PropertyFilter> rejectFilters = new ArrayList<PropertyFilter>();
	private boolean passFiltersExist = false;
	private boolean rejectFiltersExist = false;
	private String name;
	
	/**
	 * Default constructor that assigns a name to the filter (for logging purpose
	 * only)
	 * 
	 * @param name
	 */
	public GraphViewerFilter(String name) {
		this.name = name;
	}
	
	@SuppressWarnings("unused")
	private void debug(String s) {
		log.debug("[" + name + "]:" + s);
	}
	
	/**
	 * Adds property filter that passes through only those elements that match
	 * these property filters.
	 * 
	 * @param propertyFilter
	 *          property filter
	 */
	public void addPassFilter(PropertyFilter propertyFilter) {
		if (propertyFilter != null) {
			passFilters.add(propertyFilter);
			passFiltersExist = true;
		}
	}
	
	/**
	 * Removes the passing property filter.
	 * 
	 * @param propertyFilter
	 *          property filter to be removed
	 */
	public void removePassFilter(PropertyFilter propertyFilter) {
		if (propertyFilter != null) {
			passFilters.remove(propertyFilter);
			passFiltersExist = passFilters.size() > 0;
		}
	}
	
	/**
	 * Adds property filter that rejects all elements that match any of these
	 * property filters.
	 * 
	 * @param propertyFilter
	 *          property filter
	 */
	public void addRejectFilter(PropertyFilter propertyFilter) {
		if (propertyFilter != null) {
			rejectFilters.add(propertyFilter);
			rejectFiltersExist = true;
		}
	}
	
	/**
	 * Removes the rejecting property filter.
	 * 
	 * @param propertyFilter
	 *          property filter to be removed
	 */
	public void removeRejectFilter(PropertyFilter propertyFilter) {
		if (propertyFilter != null) {
			rejectFilters.remove(propertyFilter);
			rejectFiltersExist = rejectFilters.size() > 0;
		}
	}
	
	/**
	 * Checks if any passing filters exist.
	 * 
	 * @return true if pass filters exist, false otherwise
	 */
	public boolean passFiltersExist() {
		return passFiltersExist;
	}
	
	/**
	 * Checks if any rejecting filters exist.
	 * 
	 * @return true if reject filters exist, false otherwise
	 */
	public boolean rejectFiltersExist() {
		return rejectFiltersExist;
	}
	
	/**
	 * Checks if any kind of filters exist at all.
	 * 
	 * @return true if any kind of filters exist, false otherwise
	 */
	public boolean filtersExist() {
		return passFiltersExist() || rejectFiltersExist();
	}
	
	/**
	 * Checks if the element passes the filters or not.
	 * 
	 * The viewer and parentElement input parameters are not used.
	 * 
	 * @see GraphViewerFilter#select(Object element)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return select(element);
	}
	
	/**
	 * Checks if the element passes the filters or not.
	 * 
	 * The principles:
	 * 
	 * 1. All property filter rules are applied as an AND operations.
	 * 
	 * 2. If pass filter is empty then elements are allowed by default.
	 * 
	 * 3. If the element contains ALL of the pass filter values, then this element
	 * is selected.
	 * 
	 * 4. If the element does not contain SOME of the pass filter values, then
	 * this element is NOT selected.
	 * 
	 * 5. If reject filter contains the value, then this element is NOT selected.
	 * 
	 * Thus, if both filters contain the value, then this element is NOT selected.
	 * 
	 * This is because filtering has to eliminate all elements, that are not
	 * expected to pass.
	 * 
	 * @param element
	 *          the graph element to be checked
	 * @return true, if the element passes the filter, false otherwise
	 */
	public boolean select(Object element) {
		if (element != null && element instanceof GXLGraphElement) {
			GXLGraphElement e = (GXLGraphElement) element;
			
			boolean select = !passFiltersExist;
			
			if (passFiltersExist) {
				for (PropertyFilter f : passFilters) {
					GXLAttr a = e.getAttr(f.getKey());
					
					if (a != null) {
						Property[] ks = asProperties(a);
						for (Property k : ks) {
							select = f.match(k);
							// debug("check pass: select=" + select + ",k=" + k);
							if (select)
								break;
						}
						if (!select)
							break;
					}
					
				}
			}
			
			if (rejectFiltersExist && select) {
				for (PropertyFilter f : rejectFilters) {
					GXLAttr a = e.getAttr(f.getKey());
					if (a != null) {
						Property[] ks = asProperties(a);
						for (Property k : ks) {
							select = !f.match(k);
							// debug("check reject: select=" + select + ",k=" + k);
							if (!select)
								break;
						}
						if (!select)
							break;
					}
				}
			}
			return select;
		}
		return true;
	}
	
	/**
	 * Returns GXLAttr name and its values as Properties array.
	 * 
	 * @param a
	 *          GXLAttr attribute with value(s)
	 * @return array of Property
	 */
	private Property[] asProperties(GXLAttr a) {
		Property[] keys = null;
		String name = a.getName();
		GXLValue v = a.getValue();
		if (v == null) {
			keys = new Property[0];
		}
		else if (v instanceof GXLCompositeValue) {
			GXLCompositeValue c = (GXLCompositeValue) v;
			keys = new Property[c.getValueCount()];
			for (int i = 0; i < c.getValueCount(); i++) {
				GXLValue v2 = c.getValueAt(i);
				String val = getStringValue(v2);
				keys[i] = PropertyFilter.toMatchable(name, val);
			}
		}
		else {
			keys = new Property[] {
				PropertyFilter.toMatchable(name, getStringValue(v))
			};
		}
		return keys;
	}
	
	/**
	 * Decodes the GXLValue as String value.
	 * 
	 * @param v
	 *          GXLValue
	 * @return GXLValue as a String
	 */
	private String getStringValue(GXLValue v) {
		return GXLCodec.toString(v);
	}
	
}
