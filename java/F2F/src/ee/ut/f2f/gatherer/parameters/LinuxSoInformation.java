package ee.ut.f2f.gatherer.parameters;

import ee.ut.f2f.gatherer.model.LinuxAttributes;
import ee.ut.f2f.gatherer.util.Constants;

/**
 * Linux specific gathering
 * @author Raido Türk
 *
 */
public class LinuxSoInformation {
	
	private native LinuxAttributes findData(LinuxAttributes attr);
	
	public LinuxSoInformation() {
		System.loadLibrary(Constants.LINUX_SO_NAME.getName());
	}
	
	public LinuxAttributes gatherInformationFromSo() {
		LinuxAttributes atr2 = new LinuxAttributes();
		LinuxAttributes atr = new LinuxSoInformation().findData(atr2);
		return atr;
	}

}
