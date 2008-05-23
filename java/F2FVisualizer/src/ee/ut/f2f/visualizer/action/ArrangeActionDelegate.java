package ee.ut.f2f.visualizer.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import ee.ut.f2f.visualizer.editor.GraphEditor;
import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * 
 * Action for switching between active editors available layout algorithms.
 * 
 * @author Indrek Priks
 */
public class ArrangeActionDelegate extends Action implements IEditorActionDelegate {
	
	private static final F2FLogger log = new F2FLogger(ArrangeActionDelegate.class);
	private GraphEditor editor;
	
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.editor = (GraphEditor) targetEditor;
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	/**
	 * Switches active editor's graph layout algorithm to the next available
	 * layout algorithm.
	 * 
	 * When the last available algorithm is reached, then the cycle starts all
	 * over again.
	 */
	public void run(IAction action) {
		log.debug("run");
		if (editor != null) {
			editor.nextGraphLayoutAlgorithm();
		}
	}
	
}
