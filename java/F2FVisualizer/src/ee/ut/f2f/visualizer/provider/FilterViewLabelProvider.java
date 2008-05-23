package ee.ut.f2f.visualizer.provider;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import ee.ut.f2f.visualizer.Activator;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.FilterTableRow;
import ee.ut.f2f.visualizer.view.FilterView;

/**
 * Provides labels and images for the table in FilterView view
 * 
 * @author Indrek Priks
 */
public class FilterViewLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	/** Logger */
	@SuppressWarnings("unused")
	private static final F2FLogger log = new F2FLogger(FilterViewLabelProvider.class);
	
	private static ImageDescriptor IMG_OK;
	private static ImageDescriptor IMG_NO;
	private static ImageDescriptor IMG_PLAY;
	private static ImageDescriptor IMG_PAUSE;
	private static ImageDescriptor IMG_FADE;
	private static ImageDescriptor IMG_REMOVE;
	
	static {
		IMG_OK = Activator.getImageDescriptor("icons/ok.gif");
		IMG_NO = Activator.getImageDescriptor("icons/no.gif");
		IMG_PLAY = Activator.getImageDescriptor("icons/play.gif");
		IMG_PAUSE = Activator.getImageDescriptor("icons/pause.gif");
		IMG_FADE = Activator.getImageDescriptor("icons/fade.gif");
		IMG_REMOVE = Activator.getImageDescriptor("icons/remove.gif");
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}
	
	private void debug(String s) {
		// log.debug(s);
	}
	
	public Image getColumnImage(Object obj, int columnIndex) {
		debug("getColumnImage:" + obj + ", " + columnIndex);
		if (obj instanceof FilterTableRow && obj != null) {
			FilterTableRow row = (FilterTableRow) obj;
			
			switch (columnIndex) {
			case FilterView.COLUMN_FILTER_MODE_IDX:
				if (FilterTableRow.FILTER_MODE_PASS.equals(row.getFilterMode())) {
					return IMG_OK.createImage();
				}
				else {
					return IMG_NO.createImage();
				}
			case FilterView.COLUMN_ACTIVE_IDX:
				if (row.isActive()) {
					return IMG_PLAY.createImage();
				}
				else {
					return IMG_PAUSE.createImage();
				}
			case FilterView.COLUMN_FILTER_TYPE_IDX:
				if (FilterTableRow.FILTER_TYPE_FADE.equals(row.getFilterType())) {
					return IMG_FADE.createImage();
				}
				else {
					return IMG_REMOVE.createImage();
				}
			}
		}
		return null;
	}
	
	public String getColumnText(Object obj, int columnIndex) {
		debug("getColumnText:" + obj + "," + columnIndex);
		if (obj instanceof FilterTableRow) {
			FilterTableRow row = (FilterTableRow) obj;
			switch (columnIndex) {
			case FilterView.COLUMN_ACTIVE_IDX:
				return null;
			case FilterView.COLUMN_FILTER_MODE_IDX:
				return null;
			case FilterView.COLUMN_FILTER_TYPE_IDX:
				return null;
			case FilterView.COLUMN_MATCH_MODE_IDX:
				return row.getPropertyFilter().getMatchModeText();
			case FilterView.COLUMN_NAME_IDX:
				return row.getPropertyFilter().getKey();
			case FilterView.COLUMN_VALUE_IDX:
				return row.getPropertyFilter().getValuePattern();
			}
		}
		return null;
	}
}
