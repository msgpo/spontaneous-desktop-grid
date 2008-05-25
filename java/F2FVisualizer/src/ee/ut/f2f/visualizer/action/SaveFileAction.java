package ee.ut.f2f.visualizer.action;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import ee.ut.f2f.visualizer.Activator;
import ee.ut.f2f.visualizer.command.ICommandIds;
import ee.ut.f2f.visualizer.editor.GraphEditor;
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
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
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
			IEditorPart part = window.getActivePage().getActiveEditor();
			
			boolean write = false;
			GraphEditorInput input = null;
			String defaultFileName = sdf.format(new Date()) + getDefaultExtension();
			if (part != null) {
				input = (GraphEditorInput) part.getEditorInput();
				write = input != null;
				if (!GraphEditorInput.LIVE_EDITOR_NAME.equals(part.getTitle())) {
					defaultFileName = part.getTitle();
				}
			}
			
			File f = chooseSaveFile(window.getShell(), defaultFileName);
			
			if (f != null) {
				if (write) {
					try {
						log.debug("writing file..");
						input.getGXLDocument().write(f);
					}
					catch (Exception e) {
						MessageDialog.openError(window.getShell(), "Error", "Error saving to file:" + e.getMessage());
					}
				}
				else {
					MessageDialog.openInformation(window.getShell(), "File saving failed!",
							"Could not save the file, editor contains no data!");
				}
			}
		}
	}
}
