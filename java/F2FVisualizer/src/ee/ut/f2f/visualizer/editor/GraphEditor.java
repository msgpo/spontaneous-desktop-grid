package ee.ut.f2f.visualizer.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.mylyn.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.mylyn.zest.core.viewers.GraphViewer;
import org.eclipse.mylyn.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.mylyn.zest.core.widgets.ZestStyles;
import org.eclipse.mylyn.zest.layouts.algorithms.AbstractLayoutAlgorithm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import ee.ut.f2f.visualizer.filter.GraphViewerFilter;
import ee.ut.f2f.visualizer.layout.LayoutAlgorithmFactory;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.GraphEditorInput;
import ee.ut.f2f.visualizer.provider.GraphEditorContentProvider;
import ee.ut.f2f.visualizer.provider.GraphEditorLabelProvider;

/**
 * Editor that displays F2F network topology graph and provides public methods
 * to modify the model and display format.
 * 
 * @author Indrek Priks
 */
public class GraphEditor extends EditorPart implements IReusableEditor, IZoomableWorkbenchPart {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(GraphEditor.class);
	/** Editor type ID */
	public static final String ID = "ee.ut.f2f.visualizer.editor.graphEditor";
	/** Name of the filter that handles the fading of graph elements. */
	public static final String FILTER_TYPE_NAME_FADE = "fade";
	/** Name of the filter that handles the removing of elements from the graph. */
	public static final String FILTER_TYPE_NAME_REMOVE = "remove";
	
	private GraphViewer viewer;
	private GraphEditorInput input;
	private GraphViewerFilter faderFilter = new GraphViewerFilter(FILTER_TYPE_NAME_FADE);
	private GraphViewerFilter removerFilter = new GraphViewerFilter(FILTER_TYPE_NAME_REMOVE);
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}
	
	/**
	 * Sets the input model for this editor.
	 */
	@Override
	public void setInput(IEditorInput input) {
		log.debug("setInput");
		super.setInput(input);
		this.input = (GraphEditorInput) input;
		firePropertyChange(PROP_INPUT);
		if (viewer != null) {
			viewer.setInput(getEditorInput());
		}
	}
	
	/**
	 * Returns the input model of this editor.
	 * 
	 * @return input model of this editor
	 */
	public GraphEditorInput getGraphEditorInput() {
		return input;
	}
	
	@Override
	public boolean isDirty() {
		return false;
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}
	
	@Override
	public void doSaveAs() {
	}
	
	@Override
	public void createPartControl(Composite parent) {
		viewer = new GraphViewer(parent, SWT.NONE);
		viewer.setNodeStyle(ZestStyles.NODES_HIGHLIGHT_ADJACENT);
		viewer.setContentProvider(new GraphEditorContentProvider(this));
		viewer.setLabelProvider(new GraphEditorLabelProvider(this));
		viewer.setLayoutAlgorithm(LayoutAlgorithmFactory.newLayoutAlgorithm(LayoutAlgorithmFactory.GRID_LAYOUT));
		viewer.setInput(getEditorInput());
		
		getSite().setSelectionProvider(viewer);
	}
	
	@Override
	public void dispose() {
	}
	
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	/**
	 * Returns the viewer of this editor that is zoomable.
	 * 
	 * @return viewer that is zoomable
	 */
	public AbstractZoomableViewer getZoomableViewer() {
		return viewer;
	}
	
	/**
	 * Returns the viewer of this editor.
	 * 
	 * @return viewer of this editor
	 */
	public GraphViewer getViewer() {
		return viewer;
	}
	
	/**
	 * Returns the <code>GraphViewerFilter</code> filter which defines the
	 * fading rules of graph elements.
	 * 
	 * @return filter for fading graph elements
	 */
	public GraphViewerFilter getFaderFilter() {
		return faderFilter;
	}
	
	/**
	 * Switches to next available graph layout algorithm.
	 */
	public void nextGraphLayoutAlgorithm() {
		if (input != null) {
			int layout = (input.getLayoutAlgorithm() + 1) % (LayoutAlgorithmFactory.ALGORITHMS_COUNT);
			changeGraphLayoutAlgorithm(layout);
		}
	}
	
	private void changeGraphLayoutAlgorithm(int layoutAlgorithm) {
		AbstractLayoutAlgorithm algorithm = LayoutAlgorithmFactory.newLayoutAlgorithm(layoutAlgorithm);
		viewer.setLayoutAlgorithm(algorithm, true);
		if (input != null) {
			input.setLayoutAlgorithm(layoutAlgorithm);
		}
	}
	
	/**
	 * Sets the filters for this editor.
	 * 
	 * If filters differ from old filters, filters are applied and view is
	 * refreshed.
	 * 
	 * @param faderFilter
	 *          filter with fading rules
	 * @param removerFilter
	 *          filter with removing rules
	 */
	public void setFilters(GraphViewerFilter faderFilter, GraphViewerFilter removerFilter) {
		log.debug("setFilters");
		boolean refresh = false;
		
		synchronized (this.faderFilter) {
			boolean oldFiltersExist = this.faderFilter != null && this.faderFilter.filtersExist();
			boolean newFiltersExist = faderFilter != null && faderFilter.filtersExist();
			boolean apply = newFiltersExist || !newFiltersExist && oldFiltersExist;
			if (apply) {
				log.debug("Appling fader filters-" + oldFiltersExist + " " + newFiltersExist);
				this.faderFilter = faderFilter;
				refresh = true;
			}
			else {
				log.debug("Same fader filters as before, skipping fader filters change.");
			}
		}
		
		synchronized (this.removerFilter) {
			boolean oldFiltersExist = this.removerFilter != null && this.removerFilter.filtersExist();
			boolean newFiltersExist = removerFilter != null && removerFilter.filtersExist();
			boolean apply = newFiltersExist || !newFiltersExist && oldFiltersExist;
			if (apply) {
				log.debug("Appling remover filters-" + oldFiltersExist + " " + newFiltersExist);
				this.removerFilter = removerFilter;
				ViewerFilter[] filters = newFiltersExist ? new ViewerFilter[] {
					removerFilter
				} : new ViewerFilter[0];
				viewer.setFilters(filters);
				refresh = false;// setFilters triggers refresh already!
			}
			else {
				log.debug("Same remover filters as before, skipping remover filters change.");
			}
		}
		
		if (refresh) {
			viewer.refresh();
		}
	}
}
