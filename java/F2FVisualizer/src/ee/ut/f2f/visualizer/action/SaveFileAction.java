package ee.ut.f2f.visualizer.action;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import ee.ut.f2f.visualizer.Activator;
import ee.ut.f2f.visualizer.command.ICommandIds;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.GraphEditorInput;

/**
 * Action for saving the content of an active editor into the file that user
 * selects.
 * 
 * @author Indrek Priks
 */
public class SaveFileAction extends FileAction {
	
	private static final F2FLogger log = new F2FLogger(SaveFileAction.class);
	private final IWorkbenchWindow window;
	
	/**
	 * Constructor.
	 * 
	 * @param window
	 *          The window in which to operate
	 * @param label
	 *          Label of the action
	 */
	public SaveFileAction(IWorkbenchWindow window, String label) {
		this.window = window;
		setText(label);
		setId(ICommandIds.CMD_SAVE_FILE);
		setActionDefinitionId(ICommandIds.CMD_SAVE_FILE);
		setImageDescriptor(Activator.getImageDescriptor("/icons/saveas.gif"));
	}
	
	/**
	 * Opens "Save as" dialog window and if a file is selected writes the input of
	 * an active editor into the selected file.
	 */
	public void run() {
		if (window != null) {
			File f = chooseSaveFile(window.getShell());
			if (f != null) {
				try {
					boolean write = false;
					IEditorPart part = window.getActivePage().getActiveEditor();
					if (part != null) {
						GraphEditorInput input = (GraphEditorInput) part.getEditorInput();
						if (input != null) {
							write = true;
							log.debug("writing file..");
							input.getGXLDocument().write(f);
						}
					}
					if (!write) {
						MessageDialog.openInformation(window.getShell(), "File saving failed!",
								"Could not save the file, editor contains no data!");
					}
				}
				catch (Exception e) {
					MessageDialog.openError(window.getShell(), "Error", "Error saving to file:" + e.getMessage());
				}
			}
		}
	}
}
