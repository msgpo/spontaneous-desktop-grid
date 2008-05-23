package ee.ut.f2f.visualizer.view;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.GraphEditorInput;
import ee.ut.f2f.visualizer.provider.StatisticsViewContentProvider;
import ee.ut.f2f.visualizer.provider.StatisticsViewLabelProvider;

/**
 * View that provides statistics about the whole graph.
 * 
 * For example the count of nodes, types of connections and statistics of other
 * attributes of the nodes.
 * 
 * @author Indrek Priks
 */
public class StatisticsView extends ViewPart implements ISelectionListener, INullSelectionListener {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(StatisticsView.class);
	
	/** The ID of the view */
	public static final String ID = "ee.ut.f2f.visualizer.view.statisticsView";
	
	private TreeViewer viewer;
	private StatisticsViewContentProvider contentProvider = null;
	
	public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
		log.debug("selectionChanged");
		if (sourcepart != StatisticsView.this) {
			IEditorInput input = findActiveEditorInput();
			
			if (input == null) {
				log.debug("selectionchanged-null");
				viewer.setInput(null);
			}
			else if (input instanceof GraphEditorInput) {
				GraphEditorInput in = (GraphEditorInput) input;
				if (!in.containsChangeListener(contentProvider)) {
					log.debug("selectionchanged:containsChangeListener=false");
					setInput(in);
				}
			}
		}
	}
	
	private void setInput(IEditorInput input) {
		log.debug("setInput");
		viewer.setInput(input);
	}
	
	private IEditorInput findActiveEditorInput() {
		IEditorPart part = getSite().getPage().getActiveEditor();
		if (part != null) {
			return part.getEditorInput();
		}
		return null;
	}
	
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.NONE);
		
		Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		
		TreeColumn col1 = new TreeColumn(tree, SWT.NONE);
		col1.setText("Parameter name");
		col1.setWidth(220);
		
		TreeColumn col2 = new TreeColumn(tree, SWT.NONE);
		col2.setText("Value");
		col2.setWidth(80);
		
		contentProvider = new StatisticsViewContentProvider(viewer);
		
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new StatisticsViewLabelProvider());
		viewer.setAutoExpandLevel(2);
		viewer.setInput(null);
		
		getSite().getPage().addSelectionListener(this);
	}
	
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
}
