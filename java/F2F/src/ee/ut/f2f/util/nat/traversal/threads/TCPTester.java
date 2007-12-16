package ee.ut.f2f.util.nat.traversal.threads;

import ee.ut.f2f.activity.Activity;
import ee.ut.f2f.activity.ActivityEvent;
import ee.ut.f2f.activity.ActivityManager;
import ee.ut.f2f.ui.F2FComputingGUI;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.nat.traversal.StunInfo;

public class TCPTester extends Thread implements Activity{
	
	final private static Logger log = Logger.getLogger(TCPTester.class);
	
	//statuses
	final private static int THREAD_STARTED = 30;
	final private static int PRETEST_CHECK = 31;
	final private static int AWAITING_ORDERS = 32;
	final private static int TESTING_TCP = 32;
	final private static int THREAD_STOPPED = -1;
	
	//reports
	final public static int TCP_TESTER_ALIVE = 20;
	final public static int TCP_TESTER_DEAD = 21;
	final private static int NO_REPORT = -1;
	
	private String peerId = null;
	private int status = THREAD_STOPPED;
	private int lastReport = NO_REPORT;
	
	public TCPTester (String peerId){
		super("TCPTester [" + peerId + "]");
		if (peerId == null) throw new NullPointerException("Peer id is null");
		this.peerId = peerId;
	}
	
	public void peerReported(Integer report){
		if(status == PRETEST_CHECK && 
		   report.intValue() >= TCP_TESTER_ALIVE &&
		   report.intValue() <= TCP_TESTER_DEAD){
			lastReport = report.intValue();
			this.interrupt();
		} else {
			log.error(getActivityName() + "received illegal report [" + report.intValue() + "]");
		}
	}
	
	public void run(){
		status = THREAD_STARTED;
		ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.STARTED));
		log.debug(getActivityName() + "started");
		//local stunInfo check
		stunInfoCheck();
		if(status == THREAD_STOPPED){
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
			return;
		}
		
		//pretest check
		preTestCheck();
		if(status == THREAD_STOPPED){
			ActivityManager.getDefault().emitEvent(new ActivityEvent(this,ActivityEvent.Type.FAILED));
			return;
		}
		
		/*
		while(true){
			//F2FComputingGUI.natMessageProcessor.getConnectionManager().sendTcpTestMessage(peerId);
			try {
				log.debug(getActivityName() + "waiting for answer from peer [" + peerId + "]");
				Thread.sleep(15000);
				log.debug(getActivityName() + " timeout ");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
		*/
	}
	
	private void stunInfoCheck(){
		log.debug(getActivityName() + "checking local stun info ...");
		StunInfo sinf = F2FComputingGUI.natMessageProcessor.getConnectionManager().getLocalStunInfo(false);
		if(sinf == null){
			log.warn(this + " local stun info is null, stopping thread");
			status = THREAD_STOPPED;
		}
	}
	
	private void preTestCheck(){
		log.debug(getActivityName() + "checking if peer has allready TCP tester running");
		//NatMessage nmsg = new NatMessage(F2FComputing.getLocalPeer().getID().toString(), peerId, NatMessage.COMMAND_IS_TCP_TESTER_ALIVE, null);
		//F2FComputingGUI.natMessageProcessor.sendNatMessage(nmsg);
		status = PRETEST_CHECK;
		log.debug("Waiting for peer's respond [30 sec] ... ");
		try{
			Thread.sleep(30000);
			log.warn(getActivityName() + "[30 sec] timeout, no answer from peer, stopping thread");
			status = THREAD_STOPPED;
			return;
		} catch (InterruptedException ex){
			log.debug(getActivityName() + "peer reported");
			if(lastReport == NO_REPORT){
				log.warn(this + " was interrupted by reporting peer, but no report received");
				status = THREAD_STOPPED;
				return;
			} else if (lastReport == TCP_TESTER_ALIVE) {
				log.debug(getActivityName() + "peer has running TCPTester, awaiting orders");
				lastReport = NO_REPORT;
				status = AWAITING_ORDERS;
				return;
			} else if (lastReport == TCP_TESTER_DEAD){
				log.debug(getActivityName() + "peer has dead TCPTester, initiating TCP testing");
				lastReport = NO_REPORT;
				status = TESTING_TCP;
				return;
			} else {
				log.warn(this + " was interrupted by reporting peer, but report is illegal for this thread's status");
				status = THREAD_STOPPED;
				return;
			}
		}
	}
	
	public String getPeerId(){
		return this.peerId;
	}
	
	public int getStatus(){
		return status;
	}
	
	public String getActivityName() {
		return this.getName() + " thread ";
	}

	public Activity getParentActivity() {
		return null;
	}
	
	public String toString(){
		String status = null;
		String lastReport = null;
		switch(this.status) {
			case THREAD_STARTED : {
				status = "Thread started";
				break;
			}
			case THREAD_STOPPED : {
				status = "Thread stopped";
				break;
			}
			case PRETEST_CHECK : {
				status = "Pretest check";
				break;
			}
			default : {
				status = "Unknown: " + this.status;
				break;
			}
		}
		
		switch(this.lastReport){
			case NO_REPORT : {
				lastReport = "No Report";
				break;
			}
			case TCP_TESTER_ALIVE : {
				lastReport = "TCP Tester alive";
				break;
			}
			case TCP_TESTER_DEAD : {
				lastReport= "TCP Tester dead";
				break;
			}
			default : {
				lastReport = "Unknown: " + this.lastReport;
				break;
			}
		}
		
		return getActivityName() + "status [" + status + "] last report [" + lastReport + "] ";
	}
}
