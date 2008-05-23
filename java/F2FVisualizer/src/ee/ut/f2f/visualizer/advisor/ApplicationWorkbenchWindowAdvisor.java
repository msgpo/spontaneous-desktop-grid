package ee.ut.f2f.visualizer.advisor;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * Responsible for creating action bar and workbench window configuration.
 * 
 * @author Indrek Priks
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	
	/**
	 * Default constructor.
	 * 
	 * @param configurer
	 */
	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}
	
	/**
	 * Creates actionbar advisor.
	 * 
	 * @param configurer
	 *          actionbar configurer
	 */
	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}
	
	/**
	 * Configures workbench window.
	 * 
	 * Sets the initial size and whether to show coolbar etc.
	 */
	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(900, 600));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(false);
	}
	
}
