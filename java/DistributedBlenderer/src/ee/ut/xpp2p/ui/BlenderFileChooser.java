package ee.ut.xpp2p.ui;

import javax.swing.JFileChooser;
import javax.swing.JFrame;


/**
 * File selection dialogs
 * 
 * @author Madis, Andres, Jaan Neljandik
 */
public class BlenderFileChooser {

	/**
	 * Dialog for opening a .blend file
	 * 
	 * @return chosen filename with full path
	 */
	public static String openBlendFile() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new BlenderFileFilter());
		int returnVal = fc.showDialog(new JFrame(), "Select input file");
		if (returnVal == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile().getAbsoluteFile().toString();

		return null;
	}

	/**
	 * Dialog for choosing the location for the output file
	 * 
	 * @return chosen directory with full path
	 */
	public static String saveBlendFile() {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = fc.showDialog(new JFrame(), "Select output location");
		if (returnVal == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile().getAbsoluteFile().toString() + "/";

		return null;
	}
}
