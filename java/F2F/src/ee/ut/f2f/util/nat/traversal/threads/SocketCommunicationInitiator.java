package ee.ut.f2f.util.nat.traversal.threads;

import java.net.InetSocketAddress;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.comm.CommunicationInitException;
import ee.ut.f2f.comm.socket.SocketCommunicationProvider;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.StunInfo;

public class SocketCommunicationInitiator extends Thread implements Activity{
	
	final private static Logger log = Logger.getLogger(SocketCommunicationInitiator.class);
	
	final private static int DEFAULT_SOCKET_COMMUNICATION_PORT = 13000;
	
	private int localPort = -1;
	
	public SocketCommunicationInitiator(){
		this(DEFAULT_SOCKET_COMMUNICATION_PORT);
	}
	
	public SocketCommunicationInitiator(int localPort){
		super("SocketCommunicationInitiator");
		this.localPort = localPort;
	}
	
	public void run(){
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.STARTED));
		log.debug("Starting [" + this.getName() + "] thread, id [" + this.getId() + "]");
		log.debug(getActivityName() + " thread  : obtaining local StunInfo");
		StunInfo sinf = F2FComputingGUI.natMessageProcessor.getConnectionManager().getLocalStunInfo(false);
		if(sinf == null){
			log.error("obtained null StunInfo, stopping " + getActivityName());
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED));
			return;
		}
		log.debug(getActivityName() + " thread obtained StunInfo \n" + sinf.toString());
		InetSocketAddress inetSoc = new InetSocketAddress(sinf.getLocalIp(),localPort);
		try {
			log.debug(getActivityName() + " thread : Initiating SocketCommunicationProvider");
			log.debug(getActivityName() + " thread : binding on local ip [" + inetSoc.getAddress().getHostAddress() + ":" + inetSoc.getPort() + "]");
			F2FComputingGUI.natMessageProcessor.getConnectionManager().setSocketCommunicationProvider(new SocketCommunicationProvider(inetSoc, null));
			log.debug(getActivityName() + " thread : initiated");
		} catch (CommunicationInitException e) {
			log.error(getActivityName() + " thread : Failed to initate SocketCommunicationProvider");
			log.error(getActivityName() + " thread : Failed on local ip [" + inetSoc.getAddress().getHostAddress() + ":" + inetSoc.getPort() + "]",e);
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FAILED));
			return;
		}
		log.debug(getActivityName() + " thread : Stopping ...");
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this, ActivityEvent.Type.FINISHED));
	}

	public String getActivityName() {
		return this.getName() + " id [" + this.getId() + "]";
	}

	public Activity getParentActivity() {
		// TODO Auto-generated method stub
		return null;
	}
}
