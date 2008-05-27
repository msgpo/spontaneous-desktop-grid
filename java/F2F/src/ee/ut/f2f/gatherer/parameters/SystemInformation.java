package ee.ut.f2f.gatherer.parameters;

import net.sourceforge.gxl.GXLDocument;

/**
 * Abstract system information gathering class
 * @author Raido Türk
 *
 */
public abstract class SystemInformation {
	
	public static SystemInformation getInstance(String osName) {
		if (osName.startsWith("windows")) {
			return new WindowsImpl();
		} else if(osName.startsWith("linux")) {
			return new LinuxImpl();
		} else if(osName.startsWith("sun")) {
			return new LinuxImpl();
		} else
			throw new RuntimeException("Unsupported operating system: "+osName);
	}

	public abstract GXLDocument gatherInformation(String nodeId);

}
