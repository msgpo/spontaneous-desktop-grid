package ee.ut.f2f.visualizer.view;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.provider.NodeInfoViewContentProvider;
import ee.ut.f2f.visualizer.provider.NodeInfoViewLabelProvider;

/**
 * View that displays detailed info about the selected node or nodes.
 * 
 * @author Indrek Priks.
 */
public class NodeInfoView extends ViewPart implements ISelectionListener {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(NodeInfoView.class);
	
	/** The ID of the view */
	public static final String ID = "ee.ut.f2f.visualizer.view.nodeInfoView";
	
	private TreeViewer viewer;
	
	public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
		log.debug("selectionChanged");
		if (sourcepart != NodeInfoView.this && selection instanceof IStructuredSelection) {
			setInput(((IStructuredSelection) selection).toList());
		}
	}
	
	@SuppressWarnings("unchecked")
	private void setInput(List input) {
		log.debug("setInput");
		viewer.setInput(input);
	}
	
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.NONE);
		
		Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		
		TreeColumn col1 = new TreeColumn(tree, SWT.NONE);
		col1.setText("Parameter name");
		col1.setWidth(150);
		
		TreeColumn col2 = new TreeColumn(tree, SWT.NONE);
		col2.setText("Value");
		col2.setWidth(150);
		
		viewer.setContentProvider(new NodeInfoViewContentProvider());
		viewer.setLabelProvider(new NodeInfoViewLabelProvider());
		viewer.setAutoExpandLevel(3);
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