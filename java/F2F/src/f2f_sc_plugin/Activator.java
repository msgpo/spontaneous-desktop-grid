package f2f_sc_plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ee.ut.f2f.comm.sc.im.SipIMCommunicationProvider;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.ui.F2FComputingGUI;

public class Activator implements BundleActivator {

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		// init GUI
		F2FComputingGUI.main(new String[]{});
		// init F2F framework
		F2FComputing.initiateF2FComputing();
		// init SIP Communicator stuff
		SipIMCommunicationProvider.initiateSipIMCommunicationProvider(context);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		System.out.println("f2f-plugin: Stop F2F SC-plugin");
	}

}
