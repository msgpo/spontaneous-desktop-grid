package ee.ut.f2f.visualizer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.ApplicationContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Indrek Priks
 */
public class Activator extends AbstractUIPlugin {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(Activator.class);
	
	/** The plug-in ID */
	public static final String PLUGIN_ID = "ee.ut.f2f.visualizer";
	
	/** The shared instance */
	private static Activator plugin;
	
	/**
	 * The applications context thats available at all times while the application
	 * is running
	 */
	private static ApplicationContext applicationContext;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		applicationContext = new ApplicationContext();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		applicationContext = null;
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/**
	 * Returns the application context.
	 * 
	 * @return Returns the application context
	 */
	public static ApplicationContext getApplicationContext() {
		log.debug("getApplicationContext");
		return applicationContext;
	}
	
	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *          the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		log.debug("getImageDescriptor:" + path);
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
