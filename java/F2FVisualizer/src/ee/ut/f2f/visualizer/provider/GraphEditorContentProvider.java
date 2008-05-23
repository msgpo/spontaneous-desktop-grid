package ee.ut.f2f.visualizer.provider;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import net.sourceforge.gxl.GXLDocument;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLGXL;
import net.sourceforge.gxl.GXLGraph;
import net.sourceforge.gxl.GXLGraphElement;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.mylyn.zest.core.viewers.IGraphContentProvider;

import ee.ut.f2f.visualizer.editor.GraphEditor;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.GraphEditorInput;

/**
 * Provides content to the graph editor.
 * 
 * @author Indrek Priks
 */
public class GraphEditorContentProvider implements IGraphContentProvider, PropertyChangeListener {
	
	// or perhaps implement IGraphEntityContentProvider?
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(GraphEditorContentProvider.class);
	private final GraphEditor editor;
	private GraphEditorInput input;
	
	/**
	 * Default constructor.
	 * 
	 * @param editor
	 *          the editor to provide content for
	 */
	public GraphEditorContentProvider(GraphEditor editor) {
		this.editor = editor;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (oldInput != null) {
			log.debug("Old input");
			GraphEditorInput in = (GraphEditorInput) oldInput;
			in.removeChangeListener(this);
		}
		if (newInput != null) {
			log.debug("New input");
			GraphEditorInput in = (GraphEditorInput) newInput;
			in.addChangeListener(this);
		}
		input = (GraphEditorInput) newInput;
	}
	
	public void dispose() {
		log.debug("dispose");
		if (input != null) {
			input.removeChangeListener(this);
		}
	}
	
	public Object[] getElements(Object input) {
		Collection<GXLEdge> edges = new ArrayList<GXLEdge>();
		if (input != null && input instanceof GraphEditorInput) {
			GraphEditorInput in = (GraphEditorInput) input;
			GXLDocument doc = in.getGXLDocument();
			GXLGXL gxl = doc.getDocumentElement();
			for (int i = 0; i < gxl.getGraphCount(); i++) {
				GXLGraph graph = gxl.getGraphAt(i);
				for (int j = 0; j < graph.getGraphElementCount(); j++) {
					GXLGraphElement el = graph.getGraphElementAt(j);
					if (el instanceof GXLEdge) {
						GXLEdge edge = (GXLEdge) el;
						edges.add(edge);
					}
				}
			}
		}
		return edges.toArray();
	}
	
	public Object getDestination(Object rel) {
		GXLEdge edge = (GXLEdge) rel;
		return edge.getTarget();
	}
	
	public Object getSource(Object rel) {
		GXLEdge edge = (GXLEdge) rel;
		return edge.getSource();
	}
	
	public double getWeight(Object connection) {
		return 0;
	}
	
	public void propertyChange(PropertyChangeEvent arg0) {
		log.debug("propertyChange");
		editor.getViewer().refresh();
	}
}
