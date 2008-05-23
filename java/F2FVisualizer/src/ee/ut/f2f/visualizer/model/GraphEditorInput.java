package ee.ut.f2f.visualizer.model;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.gxl.GXLDocument;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Provides for the graph editor input model that can be listened for changes.
 * 
 * @author Indrek Priks
 */
public class GraphEditorInput implements IEditorInput {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(GraphEditorInput.class);
	/** The editor window name of the editor of the live data */
	private static final String LIVE_EDITOR_NAME = "live";
	
	private List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
	private final String name;
	private final GXLDocument doc;
	private int layoutAlgorithm = 0;
	
	/**
	 * Constructor for live data editor
	 * 
	 * @param doc
	 *          input GXL document
	 */
	public GraphEditorInput(GXLDocument doc) {
		this(doc, LIVE_EDITOR_NAME);
	}
	
	/**
	 * Constructor for file data editor
	 * 
	 * @param doc
	 *          input GXL document
	 * @param filename
	 *          input filename
	 */
	public GraphEditorInput(GXLDocument doc, String filename) {
		this.doc = doc;
		this.name = filename;
	}
	
	private void notifyListeners() {
		log.debug("notifyListeners");
		for (Iterator<PropertyChangeListener> iterator = listeners.iterator(); iterator.hasNext();) {
			PropertyChangeListener name = iterator.next();
			name.propertyChange(null);
		}
	}
	
	/**
	 * Checks if suchs PropertyChangeListener is registered
	 * 
	 * @param listener
	 *          the PropertyChangeListener to be checked for
	 * @return true if this listener is registered, false otherwise
	 */
	public boolean containsChangeListener(PropertyChangeListener listener) {
		for (Iterator<PropertyChangeListener> iterator = listeners.iterator(); iterator.hasNext();) {
			PropertyChangeListener li = iterator.next();
			if (li == listener) {
				log.debug("containsChangeListener=true");
				return true;
			}
		}
		log.debug("containsChangeListener=false");
		return false;
	}
	
	/**
	 * Adds the PropertyChangeListener to listen for changes.
	 * 
	 * @param newListener
	 *          the PropertyChangeListener
	 */
	public void addChangeListener(PropertyChangeListener newListener) {
		log.debug("addChangeListener");
		listeners.add(newListener);
	}
	
	/**
	 * Removes the PropertyChangeListener from listening the changes.
	 * 
	 * @param oldListener
	 *          the PropertyChangeListener
	 */
	public void removeChangeListener(PropertyChangeListener oldListener) {
		log.debug("removeChangeListener");
		listeners.remove(oldListener);
	}
	
	/**
	 * Sets the new layout algorithm and notifies listeners about the change.
	 * 
	 * @param layoutAlgorithm
	 *          layout algorithm
	 * @see ee.ut.f2f.visualizer.layout.LayoutAlgorithmFactory
	 */
	public void setLayoutAlgorithm(int layoutAlgorithm) {
		this.layoutAlgorithm = layoutAlgorithm;
		notifyListeners();
	}
	
	/**
	 * Returns the layout algorithm currently in use.
	 * 
	 * @return layout algorithm
	 * @see ee.ut.f2f.visualizer.layout.LayoutAlgorithmFactory
	 */
	public int getLayoutAlgorithm() {
		return layoutAlgorithm;
	}
	
	public boolean exists() {
		return doc != null;
	}
	
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	
	/**
	 * Returns the name of the editor
	 * 
	 * @return the name of the editor
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the GXLDocument of this editor input model.
	 * 
	 * @return GXL document
	 */
	public GXLDocument getGXLDocument() {
		return doc;
	}
	
	public IPersistableElement getPersistable() {
		return null;
	}
	
	/**
	 * Returns the tool-tip text
	 * 
	 * @return tool-tip text
	 */
	public String getToolTipText() {
		return name;
	}
	
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		log.debug("equals:" + obj.getClass().getName());
		if (obj instanceof GraphEditorInput) {
			GraphEditorInput in = (GraphEditorInput) obj;
			return name.equals(in.name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return doc.hashCode();
	}
	
}