package ee.ut.f2f.visualizer.layout;

import org.eclipse.mylyn.zest.layouts.LayoutStyles;
import org.eclipse.mylyn.zest.layouts.algorithms.AbstractLayoutAlgorithm;
import org.eclipse.mylyn.zest.layouts.algorithms.GridLayoutAlgorithm;
import org.eclipse.mylyn.zest.layouts.algorithms.HorizontalLayoutAlgorithm;
import org.eclipse.mylyn.zest.layouts.algorithms.HorizontalShift;
import org.eclipse.mylyn.zest.layouts.algorithms.HorizontalTreeLayoutAlgorithm;
import org.eclipse.mylyn.zest.layouts.algorithms.RadialLayoutAlgorithm;
import org.eclipse.mylyn.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.eclipse.mylyn.zest.layouts.algorithms.TreeLayoutAlgorithm;
import org.eclipse.mylyn.zest.layouts.algorithms.VerticalLayoutAlgorithm;

import ee.ut.f2f.visualizer.log.F2FLogger;

/**
 * Provides for instantiating the layout algorithms.
 * 
 * @author Indrek Priks
 */
public class LayoutAlgorithmFactory {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(LayoutAlgorithmFactory.class);
	
	/** Count of the algorithms this factory knows about */
	public static final int ALGORITHMS_COUNT = 8;
	
	/** Defines the Grid LayoutAlgorithm */
	public static final int GRID_LAYOUT = 0;
	/** Defines the Radial LayoutAlgorithm */
	public static final int RADIAL_LAYOUT = 1;
	/** Defines the Tree LayoutAlgorithm */
	public static final int TREE_LAYOUT = 2;
	/** Defines the Spring LayoutAlgorithm */
	public static final int SPRING_LAYOUT = 3;
	/** Defines the Horizontal LayoutAlgorithm */
	public static final int HORIZONTAL_LAYOUT = 4;
	/** Defines the Horizontal Shift LayoutAlgorithm */
	public static final int HORIZONTAL_SHIFT_LAYOUT = 5;
	/** Defines the Horizontal Tree LayoutAlgorithm */
	public static final int HORIZONTAL_TREE_LAYOUT = 6;
	/** Defines the Vertical LayoutAlgorithm */
	public static final int VERTICAL_LAYOUT = 7;
	
	/**
	 * Returns the LayoutAlgorithm defined by the input parameter.
	 * 
	 * See constants in this class.
	 * 
	 * @param layoutAlgorithm
	 *          the type of algorithm desired
	 * @return LayoutAlgorithm object
	 * @throws IllegalArgumentException
	 *           if no such algorithm is defined
	 */
	public static AbstractLayoutAlgorithm newLayoutAlgorithm(int layoutAlgorithm) {
		debug("newLayoutAlgorithm(" + layoutAlgorithm + ")");
		AbstractLayoutAlgorithm algorithm = new GridLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
		switch (layoutAlgorithm) {
		case GRID_LAYOUT:
			debug("Grid layout");
			algorithm = new GridLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		case RADIAL_LAYOUT:
			debug("Radial layout");
			algorithm = new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		case TREE_LAYOUT:
			debug("Tree layout");
			algorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		case SPRING_LAYOUT:
			debug("Spring layout");
			algorithm = new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		case HORIZONTAL_LAYOUT:
			debug("Horizontal layout");
			algorithm = new HorizontalLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		case HORIZONTAL_SHIFT_LAYOUT:
			debug("Horizontal shift layout");
			algorithm = new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		case HORIZONTAL_TREE_LAYOUT:
			debug("Horizontal tree layout");
			algorithm = new HorizontalTreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		case VERTICAL_LAYOUT:
			debug("Vertical layout");
			algorithm = new VerticalLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
			break;
		default:
			throw new IllegalArgumentException("Invalid layout algorithm type: " + layoutAlgorithm);
		}
		return algorithm;
	}
	
	private static void debug(String s) {
		log.debug(s);
	}
	
}
