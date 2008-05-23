package ee.ut.f2f.visualizer.dao;

import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.eclipse.core.runtime.FileLocator;

import ee.ut.f2f.gatherer.rmi.IF2FGatherer;

/**
 * Provides access to the F2F network data gathering tool.
 * 
 * @author Indrek Priks
 */
public class F2FGathererDao {
	
	private static final String POLICY_FILE = "mypolicy.policy";
	
	private IF2FGatherer server;
	
	/**
	 * Returns the RMI F2F gatherer instance.
	 * 
	 * If the instance is not available wraps the Exception into a
	 * RuntimeException.
	 * 
	 * @return RMI F2F gatherer instance
	 */
	public synchronized IF2FGatherer getF2FGatherer() {
		if (server == null) {
			System.out.println("Starting up F2FGatherer client server..");
			try {
				if (System.getSecurityManager() == null) {
					// URL pluginURL = ClassLoader.getSystemResource(POLICY_FILE);
					URL pluginURL = this.getClass().getClassLoader().getResource(POLICY_FILE);
					URL nativeURL = FileLocator.resolve(pluginURL);
					System.setProperty("java.security.policy", nativeURL.toExternalForm());
					System.out.println(System.getProperty("java.security.policy"));
					
					System.setSecurityManager(new SecurityManager());
				}
				Registry registry = LocateRegistry.getRegistry();
				IF2FGatherer server = (IF2FGatherer) registry.lookup(IF2FGatherer.RMI_BIND_NAME);
				// If successfully created, modify the class field too
				this.server = server;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return server;
	}
	
}