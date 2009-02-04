package net.ulno.jpunch.test;

import net.ulno.jpunch.comm.udp.UDPConnection;
import net.ulno.jpunch.comm.udp.UDPTester;
import net.ulno.jpunch.core.CommunicationFailedException;
import net.ulno.jpunch.util.logging.Logger;
import net.ulno.jpunch.util.stun.LocalStunInfo;

public class JPunchTest {
	private static final Logger log = Logger.getLogger(JPunchTest.class);
	
	private UDPTester udpTester = new UDPTester();
	private UDPConnection udpConnection = null;
	
	//Constants
	private static final String MASTER_PROPERTY = "net.ulno.jpunch.Master";
	private static final String FILENAME_PROPERTY = "net.ulno.jpunch.Filename";
	
	public void testUDP() throws CommunicationFailedException {
		log.debug(isMaster() ? "Master Node" : "Slave Node");
		log.debug("Waiting for connection");
		udpConnection = udpTester.getUDPConnection();
		udpConnection.start();
		log.debug("Connected");
		//
		if (isMaster()) masterTest();
		else slaveTest();

	}
	
	public void testStunInfo(){
		LocalStunInfo.getInstance().updateReachableServers();
	}
	
	public static void main(String[] args) throws CommunicationFailedException{
		JPunchTest jPunchTest = new JPunchTest();	
		jPunchTest.testUDP();
	}
	
	private void masterTest() throws CommunicationFailedException{
		log.debug("Master will send message");
		String message = "1234567890-qwertyuiop[asdfghjkl;'zxcvbnm,./"
						+ "QWERTYUIOP{ASDFGHJKL:ZXCVBNM<>?~!@#$%^&*()_";
		//send message
		udpConnection.sendMessage(message);
		log.debug("Sent Message \n\t[" + message + "]");
	}
	
	private void slaveTest(){
		log.debug("Slave will receive message");
		String message = (String)udpConnection.receiveMessage();
		log.debug("Received message [" + message + "]");	
	}
	
	private boolean isMaster(){
		String masterProperty = System.getProperty(MASTER_PROPERTY);
		if (masterProperty != null && !"".equals(masterProperty)) return true;
		return false;
	}
}
