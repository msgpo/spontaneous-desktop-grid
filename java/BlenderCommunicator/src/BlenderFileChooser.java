import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * File selection dialogs
 * @author Madis, Andres, Jaan
 */
public class BlenderFileChooser {
  
  /**
   * Dialog for opening a .blend file
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
  
  public static String selectFileFormat(){ 
		Object[] fileFormats = { "AVIJPEG", "TGA", "IRIS", "HAMK", "FTYPE", "JPEG",
					"MOVIE", "IRIZ", "RAWTGA", "AVIRAW", "PNG", "BMP",
					"FRAMESERVER" };
			String fileFormat = (String)JOptionPane.showInputDialog(
					new JFrame(),
	                      "Choose an output file format:\n",
	                      "Choose File Format",
	                      JOptionPane.QUESTION_MESSAGE,
	                      null,
	                      fileFormats,
	                      null);
			return fileFormat;
	  }
  
}
