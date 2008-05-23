package ee.ut.f2f.visualizer.model;

import ee.ut.f2f.visualizer.dao.F2FGathererDao;
import ee.ut.f2f.visualizer.dao.F2FNetworkStructureDAO;
import ee.ut.f2f.visualizer.dao.IF2FNetworkStructureDAO;

/**
 * The context model that is always available while the application is running.
 * 
 * @author Indrek Priks
 */
public class ApplicationContext {
	
	private F2FGathererDao f2fGathererDao;
	private IF2FNetworkStructureDAO f2fNetworkStructureDao;
	
	public ApplicationContext() {
		f2fGathererDao = new F2FGathererDao();
		f2fNetworkStructureDao = new F2FNetworkStructureDAO();
	}
	
	public F2FGathererDao getF2FGathererDao() {
		return f2fGathererDao;
	}
	
	public IF2FNetworkStructureDAO getF2FNetworkStructureDAO() {
		return f2fNetworkStructureDao;
	}
}
