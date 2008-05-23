package ee.ut.f2f.visualizer.provider;

import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLNode;

import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.mylyn.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.swt.graphics.Color;

import ee.ut.f2f.visualizer.editor.GraphEditor;
import ee.ut.f2f.visualizer.gxl.GXLCodec;
import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Provides labels and colors for GraphEditor editor.
 * 
 * @author Indrek Priks
 */
public class GraphEditorLabelProvider extends LabelProvider implements IColorProvider, IConnectionStyleProvider {
	
	/** Logger */
	@SuppressWarnings("unused")
	private static final F2FLogger log = new F2FLogger(GraphEditorLabelProvider.class);
	
	private static final Color black = new Color(null, 0, 0, 0);
	private static final Color extraLightGray = new Color(null, 240, 240, 240);
	private static final Color lightGray = new Color(null, 140, 140, 140);
	private static final Color lightBlue = new Color(null, 155, 222, 255);
	
	private final GraphEditor editor;
	
	/**
	 * Default constructor.
	 * 
	 * @param editor
	 *          the editor to provide labels for
	 */
	public GraphEditorLabelProvider(GraphEditor editor) {
		this.editor = editor;
	}
	
	private void debug(String s) {
		//log.debug(s);
	}
	
	public String getText(Object element) {
		String result = null;
		if (element instanceof GXLNode) {
			GXLNode el = (GXLNode) element;
			result = el.getID();
		}
		return result;
	}
	
	public Color getForeground(Object element) {
		boolean select = editor.getFaderFilter().select(element);
		Color c = black;
		if (element instanceof GXLNode) {
			c = select ? black : lightGray;
		}
		debug("getForeground:" + element.getClass().getName() + ",select=" + select + ",color=" + c);
		return c;
	}
	
	public Color getBackground(Object element) {
		boolean select = editor.getFaderFilter().select(element);
		Color c = black;
		if (element instanceof GXLNode) {
			c = select ? lightBlue : extraLightGray;
		}
		debug("getBackground:" + element.getClass().getName() + ",select=" + select + ",color=" + c);
		return c;
	}
	
	public Color getColor(Object rel) {
		debug("getColor:" + rel.getClass().getName());
		return lightGray;
	}
	
	public int getConnectionStyle(Object rel) {
		debug("getConnectionStyle:" + rel.getClass().getName());
		return 0;
	}
	
	public Color getHighlightColor(Object rel) {
		debug("getHighlightColor:" + rel.getClass().getName());
		return black;
	}
	
	public int getLineWidth(Object rel) {
		int result = -1;
		int bandwith = 0;
		if (rel instanceof GXLEdge) {
			bandwith = GXLCodec.getConnectionBandwith((GXLEdge) rel);
			// bytes/sec
			if (bandwith < 1000) {
				result = 0;
			}
			else if (bandwith < 10000) {
				result = 1;
			}
			else if (bandwith < 100000) {
				result = 2;
			}
			else if (bandwith < 1000000) {
				result = 3;
			}
			else {
				result = 4;
			}
		}
		debug("getLineWidth:" + rel.getClass().getName() + ", bandwith=" + bandwith + " bytes/sec, result=" + result);
		return result;
	}
	
	public IFigure getTooltip(Object entity) {
		debug("getTooltip:" + entity.getClass().getName());
		return null;
	}
}
