package ee.ut.f2f.visualizer.provider;

import java.util.Map.Entry;

import net.sourceforge.gxl.GXLNode;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.Property;

/**
 * Provides labels for StatisticsView view.
 * 
 * @author Indrek Priks
 */
public class StatisticsViewLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	/** Logger */
	@SuppressWarnings("unused")
	private static final F2FLogger log = new F2FLogger(StatisticsViewLabelProvider.class);
	
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
	
	@SuppressWarnings("unchecked")
	public String getColumnText(Object obj, int columnIndex) {
		// log.debug("getColumnText:" + obj + ", " + columnIndex);
		switch (columnIndex) {
		case 0:
			if (obj instanceof Entry) {
				Entry e = (Entry) obj;
				return e.getKey().toString();
			}
			else if (obj instanceof Property) {
				return ((Property) obj).getKey();
			}
			else if (obj instanceof Integer) {
				Integer i = (Integer) obj;
				switch (i) {
				case StatisticsViewConstants.EL_CONNECTIONS:
					return StatisticsViewConstants.LABEL_CONNECTIONS;
				case StatisticsViewConstants.EL_NODE_ATTR_STATISTICS:
					return StatisticsViewConstants.LABEL_NODES_ATTRIBUTES;
				default:
					return null;
				}
			}
		case 1:
			if (obj instanceof Entry) {
				Entry e = (Entry) obj;
				return e.getValue().toString();
			}
			else if (obj instanceof Property) {
				return ((Property) obj).getValue();
			}
		default:
			return null;
		}
	}
	
}
