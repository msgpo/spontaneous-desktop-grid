package ee.ut.f2f.visualizer.action;

import net.sourceforge.gxl.GXLDocument;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

import ee.ut.f2f.visualizer.Activator;
import ee.ut.f2f.visualizer.command.ICommandIds;
import ee.ut.f2f.visualizer.dao.IF2FNetworkStructureDAO;
import ee.ut.f2f.visualizer.editor.GraphEditor;
import ee.ut.f2f.visualizer.gxl.GXLCodec;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.GraphEditorInput;

/**
 * Action for collecting live data about F2F Network topology and showing it in
 * the editor window.
 * 
 * @author Indrek Priks
 */
public class CollectDataAction extends Action {
	
	private static final F2FLogger log = new F2FLogger(CollectDataAction.class);
	private final IWorkbenchWindow window;
	private final String editorId;
	
	/**
	 * Constructor.
	 * 
	 * @param window
	 *          The window in which to operate
	 * @param label
	 *          Label of the action
	 * @param editorId
	 *          The ID of an editor to open
	 */
	public CollectDataAction(IWorkbenchWindow window, String label, String editorId) {
		this.window = window;
		this.editorId = editorId;
		setText(label);
		setId(ICommandIds.CMD_COLLECT_DATA);
		setActionDefinitionId(ICommandIds.CMD_COLLECT_DATA);
		setImageDescriptor(Activator.getImageDescriptor("/icons/refresh.gif"));
	}
	
	/**
	 * Collects new data about F2F Network topology and shows it in an editor.
	 * 
	 * If there is no live data editor open yet, then a new editor window is
	 * opened. If an live data editor was already open, then the result is shown
	 * in that editor (its content is refreshed).
	 */
	public void run() {
		if (window != null) {
			GXLDocument d = null;
			try {
				IF2FNetworkStructureDAO dao = Activator.getApplicationContext().getF2FNetworkStructureDAO();
				d = dao.getGXLDocument();
			}
			catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error collecting the information:" + e.getMessage());
				return;
			}
			
			if (GXLCodec.getNodesCount(d) < 2) {
				MessageDialog.openInformation(window.getShell(), "No friends are connected", "No friends are connected");
				return;
			}
			
			GraphEditorInput input = new GraphEditorInput(d);
			try {
				GraphEditor part = (GraphEditor) window.getActivePage().findEditor(input);
				if (part == null) {
					log.debug("open editor");
					window.getActivePage().openEditor(input, editorId, true);
				}
				else {
					log.debug("reuse editor");
					window.getActivePage().reuseEditor(part, input);
				}
			}
			catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening editor:" + e.getMessage());
			}
		}
	}
	
}
