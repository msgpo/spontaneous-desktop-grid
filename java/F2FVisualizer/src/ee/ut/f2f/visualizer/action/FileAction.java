package ee.ut.f2f.visualizer.action;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Base class for actions that have to let the user to choose a file.
 * 
 * @author Indrek Priks
 */
public abstract class FileAction extends Action {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(FileAction.class);
	/** Last directory path that was used in action */
	protected String lastUsedDirectory;
	/** Selectable file extensions */
	protected String[] extensions = new String[] {
			"*.gxl", "*.xml", "*"
	};
	/** Selectable file extension descriptions */
	protected String[] extensionNames = new String[] {
			"GXL files (*.gxl)", "XML files (*.xml)", "All files (*)"
	};
	/** Default file extension */
	private static final String DEFAULT_FILE_EXT = ".gxl";
	
	/**
	 * Returns the default file extension
	 * 
	 * @return the default file extension
	 */
	protected String getDefaultExtension() {
		return DEFAULT_FILE_EXT;
	}
	
	/**
	 * Opens file choose dialog for "Save as" purpose.
	 * 
	 * Lets the user to choose a filename and path where to save a file.
	 * 
	 * @param shell
	 *          Window which opens the file choose dialog.
	 * @return <code> File </code> object, if a file was chosen.
	 *         <code> null </code> if no file was chosen.
	 */
	public File chooseSaveFile(Shell shell) {
		return chooseSaveFile(shell, null);
	}
	
	/**
	 * Opens file choose dialog for "Save as" purpose.
	 * 
	 * Lets the user to choose a filename and path where to save a file.
	 * 
	 * @param shell
	 *          Window which opens the file choose dialog.
	 * @param fileName
	 *          Default file name to display in the save file dialog.
	 * @return <code> File </code> object, if a file was chosen.
	 *         <code> null </code> if no file was chosen.
	 */
	public File chooseSaveFile(Shell shell, String fileName) {
		return chooseFile(shell, SWT.SAVE, fileName);
	}
	
	/**
	 * Opens file choose dialog for "Open file" purpose.
	 * 
	 * Lets the user to choose a single file for opening.
	 * 
	 * @param shell
	 *          Window which opens the file choose dialog.
	 * @return <code> File </code> object, if a file was chosen.
	 *         <code> null </code> if no file was chosen.
	 */
	public File chooseOpenFile(Shell shell) {
		return chooseFile(shell, SWT.OPEN, null);
	}
	
	private File chooseFile(Shell shell, int style, String fileName) {
		FileDialog dialog = new FileDialog(shell, style);
		
		dialog.setFilterExtensions(extensions);
		dialog.setFilterNames(extensionNames);
		dialog.setFileName(fileName);
		if (lastUsedDirectory != null)
			dialog.setFilterPath(lastUsedDirectory);
		
		String selectedFile = dialog.open();
		
		if (selectedFile == null) {
			return null;
		}
		
		File file = new File(selectedFile);
		
		lastUsedDirectory = file.getParent();
		
		String filePath = file == null ? null : file.getAbsolutePath();
		log.debug("Choosed file:" + filePath);
		
		return file;
	}
}
