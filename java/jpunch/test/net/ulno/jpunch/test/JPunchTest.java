package net.ulno.jpunch.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
	
	public void testUDP() throws CommunicationFailedException, IOException {
		log.debug(isMaster() ? "Master Node" : "Slave Node");
		//log.debug("Waiting for connection");
		//udpConnection = udpTester.getUDPConnection();
		//udpConnection.start();
		//log.debug("Connected");
		//
		log.debug("Filename [" + getFilename() + "]");
		if (isMaster()) masterTest();
		else slaveTest();

	}
	
	public void testStunInfo(){
		LocalStunInfo.getInstance().updateReachableServers();
	}
	
	public static void main(String[] args) throws CommunicationFailedException, IOException{
		JPunchTest jPunchTest = new JPunchTest();	
		jPunchTest.testUDP();
	}
	
	private void masterTest() throws CommunicationFailedException, IOException{
		//log.debug("Master will send message");
		//String message = "1234567890-qwertyuiop[asdfghjkl;'zxcvbnm,./"
		//				+ "QWERTYUIOP{ASDFGHJKL:ZXCVBNM<>?~!@#$%^&*()_";
		//send message
		//udpConnection.sendMessage(message);
		//log.debug("Sent Message \n\t[" + messge + "]");
		
		String filename = getFilename();
		BufferedInputStream bInput = new BufferedInputStream(new FileInputStream(filename));
		
		log.debug("Master will send file [" + filename + "]");
		byte[] buffer = new byte[bInput.available()];
		
		while(bInput.read(buffer) != -1){
			log.debug("Bytes for sending [" + buffer.length + "] bytes");
		}
		bInput.close();
		
		udpConnection.sendMessage(buffer);
		log.debug("Sent Message \n\t[" + buffer.length + "]");
	}
	
	private void slaveTest() throws IOException{
		log.debug("Slave will receive message");
		String filename = getFilename();
		File file = new File(filename);
		if (!file.exists()){
			file.createNewFile();
		}
		log.debug("Message will be saved in file [" + filename + "]");
		//while (true) {
			//byte[] bytes = (byte[]) udpConnection.receiveMessage();
			byte[] bytes = "asdfghjk".getBytes();
			BufferedOutputStream bOutput = 
				new BufferedOutputStream(new FileOutputStream(file,true));
			log.debug("Received message [" + bytes.length + "]");
			bOutput.write(bytes);
			bOutput.close();
		//}
		
	}
	
	private String getFilename(){
		String filename = System.getProperty(FILENAME_PROPERTY);
		if (filename == null || "".equals(filename)){
			log.error("No files for sending");
			throw new NullPointerException("No filename specified");
		}
		return filename;
	}
	
	private boolean isMaster(){
		String masterProperty = System.getProperty(MASTER_PROPERTY);
		if (masterProperty != null && !"".equals(masterProperty)) return true;
		return false;
	}
}
