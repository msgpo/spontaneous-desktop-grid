import java.io.File;
import javax.swing.filechooser.*;

/**
 * @author Madis
 */
public class BlenderFileFilter extends FileFilter {
 
  public String getDescription() {
    return "Blender Files (.blend)";
  }

  public boolean accept(File f) {
    if (f.isDirectory()) 
      return true;
    
    if (f.getName().toLowerCase().endsWith("blend"))
      return true;
    
    return false;
  }
}
