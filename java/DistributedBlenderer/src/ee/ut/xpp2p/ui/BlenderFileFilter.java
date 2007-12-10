package ee.ut.xpp2p.ui;
import java.io.File;
import javax.swing.filechooser.*;

/**
 * @author Madis, Vladimir Ðkarupelov
 */
public class BlenderFileFilter extends FileFilter {
 
  public static final String BLENDER_EXTENSION = "blend";
	
  public String getDescription() {
    return "Blender Files (." + BLENDER_EXTENSION + ")";
  }

  public boolean accept(File f) {
    if (f.isDirectory()) 
      return true;
    
    if (f.getName().toLowerCase().endsWith(BLENDER_EXTENSION))
      return true;
    
    return false;
  }
}
