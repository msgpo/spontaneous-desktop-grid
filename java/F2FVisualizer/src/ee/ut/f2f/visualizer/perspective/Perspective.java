package ee.ut.f2f.visualizer.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import ee.ut.f2f.visualizer.view.FilterView;
import ee.ut.f2f.visualizer.view.NodeInfoView;
import ee.ut.f2f.visualizer.view.StatisticsView;

/**
 * Provides for the (initial) perspective of the workbench.
 * 
 * @author Indrek Priks
 */
public class Perspective implements IPerspectiveFactory {
	
	/**
	 * Creates initial layout for the page
	 * 
	 * @param layout
	 *          page layout
	 */
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		
		layout.setEditorAreaVisible(true);
		
		IFolderLayout folder = layout.createFolder("Properties", IPageLayout.LEFT, 0.35f, editorArea);
		folder.addView(NodeInfoView.ID);
		folder.addView(StatisticsView.ID);
		folder.addView(FilterView.ID);
		
		layout.getViewLayout(NodeInfoView.ID).setCloseable(false);
		layout.getViewLayout(StatisticsView.ID).setCloseable(false);
		layout.getViewLayout(FilterView.ID).setCloseable(false);
	}
}
