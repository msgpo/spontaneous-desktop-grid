package ee.ut.f2f.gatherer.parameters;

import ee.ut.f2f.gatherer.model.WindowsAttributes;
import ee.ut.f2f.gatherer.util.Constants;

/**
 * 
 * @author Raido T�rk
 *
 */
public class WinDllInformation {

	private native void gatherSystemInformation();
	private native WindowsAttributes findData(WindowsAttributes attr);
	
	public WinDllInformation() {
		System.loadLibrary(Constants.WINDOWS_DLL_NAME.getName());
	}
	
	public WindowsAttributes gatherInformationFromDll() {
		WindowsAttributes atr2 = new WindowsAttributes();
		WindowsAttributes atr = new WinDllInformation().findData(atr2);
		return atr;
	}
	
}
