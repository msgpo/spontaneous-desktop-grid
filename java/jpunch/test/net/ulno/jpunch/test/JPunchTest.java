package net.ulno.jpunch.test;

import net.ulno.jpunch.comm.udp.UDPTester;
import net.ulno.jpunch.util.stun.LocalStunInfo;

public class JPunchTest {
	//private static final Logger log = Logger.getLogger(JPunchTest.class);
	
	private UDPTester udpTester = new UDPTester();
	
	public void testUDP() {
		udpTester.start();
		
	}
	
	public void testStunInfo(){
		LocalStunInfo.getInstance().updateReachableServers();
	}
	
	public static void main(String[] args){
		JPunchTest jPunchTest = new JPunchTest();
		
		//String vmargs = System.getProperty("net.ulno.jpunch.StunServers");
		
		//log.debug("Vmargs = [" + vmargs + "]");
		
		//String[] sservers = vmargs.split(",");
		
		//log.debug("Sservers = [" + Arrays.toString(sservers) + "]");
		
		//Collection<String> rawServers = Arrays.asList(sservers);
		
		//log.debug("RawServers = [" + rawServers + "]");
		
		//LocalStunInfo.getInstance().setRawStunServers(rawServers);
		
		//jPunchTest.testStunInfo();
		jPunchTest.testUDP();
	}
}
