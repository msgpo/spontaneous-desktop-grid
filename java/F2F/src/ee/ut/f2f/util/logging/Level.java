/**
 * 
 */
package ee.ut.f2f.util.logging;

/**
 * @author olegus
 *
 */
public enum Level {
	
	TRACE(20),
	DEBUG(40),
	INFO(50),
	WARN(60),
	ERROR(70),
	FATAL(80);
	
	int number;
	/** Level of actual logger where this enum is mapped. */
	Object mapping;

	Level(int num) {
		number = num;
	}
	
	static {
		Logger.initializeMapping();
	}
}
