package ee.ut.f2f.visualizer.action;

import java.io.File;

import net.sourceforge.gxl.GXLDocument;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

import ee.ut.f2f.visualizer.Activator;
import ee.ut.f2f.visualizer.command.ICommandIds;
import ee.ut.f2f.visualizer.gxl.GXLCodec;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.GraphEditorInput;

/**
 * Action for letting the user to choose a file in a dialog and open it in an
 * editor.
 * 
 * @author Indrek Priks
 */
public class OpenFileAction extends FileAction {
	
	private static final F2FLogger log = new F2FLogger(OpenFileAction.class);
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
	public OpenFileAction(IWorkbenchWindow window, String label, String editorId) {
		this.window = window;
		this.editorId = editorId;
		setText(label);
		setId(ICommandIds.CMD_OPEN_FILE);
		setActionDefinitionId(ICommandIds.CMD_OPEN_FILE);
		setImageDescriptor(Activator.getImageDescriptor("/icons/openFolder.gif"));
	}
	
	/**
	 * Opens "Open file" dialog window and if a file is selected opens it in new
	 * editor window.
	 */
	public void run() {
		if (window != null) {
			File f = chooseOpenFile(window.getShell());
			if (f != null) {
				GXLDocument d = GXLCodec.read(f);
				try {
					log.debug("open editor");
					window.getActivePage().openEditor(new GraphEditorInput(d, f.getName()), editorId, true);
				}
				catch (Exception e) {
					MessageDialog.openError(window.getShell(), "Error", "Error opening editor:" + e.getMessage());
				}
			}
		}
	}
	
}
