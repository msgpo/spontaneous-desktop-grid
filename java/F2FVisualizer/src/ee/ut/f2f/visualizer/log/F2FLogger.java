package ee.ut.f2f.visualizer.log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple base class for primitive logging.
 * 
 * @author Indrek Priks
 */
public class F2FLogger {
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	private final String name;
	
	/**
	 * Default constructor.
	 * 
	 * @param clazz
	 *          The class from which logging will take place.
	 */
	@SuppressWarnings("unchecked")
	public F2FLogger(Class clazz) {
		this.name = clazz.getSimpleName();
	}
	
	/**
	 * Logs the input string s with the debug level.
	 * 
	 * @param s
	 *          Message to log
	 */
	public void debug(String s) {
		System.out.println(sdf.format(new Date()) + " " + name + ":" + s);
	}
}
