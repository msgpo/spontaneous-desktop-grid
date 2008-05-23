package ee.ut.f2f.visualizer.provider;

import net.sourceforge.gxl.GXLAttr;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLNode;
import net.sourceforge.gxl.GXLValue;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import ee.ut.f2f.visualizer.gxl.GXLCodec;
import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Provides labels for NodeInfoView view.
 * 
 * @author Indrek Priks
 */
public class NodeInfoViewLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	/** Logger */
	@SuppressWarnings("unused")
	private static final F2FLogger log = new F2FLogger(NodeInfoViewLabelProvider.class);
	
	public Image getColumnImage(Object obj, int columnIndex) {
		String imageKey = null;
		switch (columnIndex) {
		case 0:
			if (obj instanceof GXLNode) {
				imageKey = ISharedImages.IMG_OBJ_FOLDER;
			}
			else {
				imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			}
			break;
		case 1:
			imageKey = null;
			break;
		default:
			imageKey = null;
		}
		
		if (imageKey != null) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
		else {
			return null;
		}
	}
	
	public String getColumnText(Object obj, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (obj instanceof GXLNode) {
				GXLNode el = (GXLNode) obj;
				return el.getID();
			}
			else if (obj instanceof GXLEdge) {
				GXLEdge el = (GXLEdge) obj;
				return el.getSourceID() + "->" + el.getTargetID();
			}
			else if (obj instanceof GXLAttr) {
				GXLAttr el = (GXLAttr) obj;
				return el.getName();
			}
			break;
		case 1:
			if (obj instanceof GXLAttr) {
				GXLAttr el = (GXLAttr) obj;
				return GXLCodec.toString(el.getValue());
			}
			else if (obj instanceof GXLValue) {
				return GXLCodec.toString((GXLValue) obj);
			}
			break;
		}
		return null;
	}
	
}