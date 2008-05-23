package ee.ut.f2f.visualizer.provider;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Provides content to the FilterView view.
 * 
 * @author Indrek Priks
 */
public class FilterViewContentProvider implements IStructuredContentProvider {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(FilterViewContentProvider.class);
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (newInput != null) {
			log.debug("inputChanged:" + newInput.getClass().getName());
		}
	}
	
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object parent) {
		if (parent instanceof List) {
			return ((List) parent).toArray();
		}
		log.debug("Warning! getElements: Unmapped type=" + parent.getClass().getName());
		return new Object[0];
	}
	
	public void dispose() {
	}
	
}
