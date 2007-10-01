import javax.swing.JFileChooser;
import javax.swing.JFrame;

/**
 * @author Madis
 */
public class BlenderFileChooser {
  
  public static String getBlendFile() {
    JFileChooser fc = new JFileChooser();
    fc.setFileFilter(new BlenderFileFilter());
    int returnVal = fc.showDialog(new JFrame(), "Select");
    if (returnVal == JFileChooser.APPROVE_OPTION)
      return fc.getSelectedFile().getAbsoluteFile().toString();
    
    return null;
  }
}
