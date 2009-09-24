/***************************************************************************
 *   Filename: JPunchTest.java
 *   Author: artjom.lind@ut.ee
 ***************************************************************************
 *   Copyright (C) 2009 by Ulrich Norbisrath
 *   devel@mail.ulno.net
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as
 *   published by the Free Software Foundation; either version 2 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the
 *   Free Software Foundation, Inc.,
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ***************************************************************************
 *   Description:
 *   Testing jPunch library
 *   (use run.sh script to run the test) s
 ***************************************************************************/

package net.ulno.jpunch.test;

import java.io.File;
import java.io.IOException;

import net.ulno.jpunch.comm.Transfer;
import net.ulno.jpunch.comm.udp.UDPConnection;
import net.ulno.jpunch.comm.udp.UDPTester;
import net.ulno.jpunch.exceptions.CommunicationFailedException;
import net.ulno.jpunch.exceptions.TransferException;
import net.ulno.jpunch.exceptions.UdpTestException;
import net.ulno.jpunch.util.Util;

import org.apache.log4j.Logger;

/**
 * Testing jPunch library
 * (use run.sh script to run the test) 
 * @author artjom.lind@ut.ee
 *
 */
public class JPunchTest extends junit.framework.TestCase{
	private static final Logger log = Logger.getLogger(JPunchTest.class);
	
	private UDPTester udpTester;
	private UDPConnection udpConnection;
	private Transfer transfer;
	private File file;
	
	//Constants
	private static final String MASTER_PROPERTY = "net.ulno.jpunch.Master";
	private static final String FILENAME_PROPERTY = "net.ulno.jpunch.Filename";
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		udpTester = new UDPTester();
	}
	
	public void testUDP() throws TransferException,CommunicationFailedException,
										IOException, UdpTestException{
		log.debug(isMaster() ? "Master Node" : "Slave Node");
		log.debug("Waiting for connection ...");
		assertNotNull(udpTester);
		udpConnection = udpTester.getUDPConnection();
		assertNotNull(udpConnection);
		udpConnection.start();
		log.debug("Connected");
		file = new File(getFilename());
		assertNotNull(file);
		log.debug("Filename [" + getFilename() + "]");
		transfer = new Transfer(udpConnection);
		assertNotNull(transfer);
		int size = -1;
		long start = System.currentTimeMillis();
		if (isMaster()) size = masterTest();
		else size = slaveTest();
		long stop = System.currentTimeMillis();
		log.info(Util.getTransferStat(start, stop, size));
		// stop UDPMessageReceiverThread // exit the application
		udpTester.stopUDPTestMessageReceiverThread();
	}
	
	public static void main(String[] args) throws CommunicationFailedException, 
									IOException, UdpTestException{
		junit.textui.TestRunner.run(JPunchTest.class);
	}
	
	private int masterTest() throws TransferException, IOException{		
		log.debug(String.format("Master will send file [%s]",file.getName()));
		int size = transfer.sendFile(file);
		log.debug(String.format("Sent File \n\t[%s] size [%d]",file.getName(),size));
		return size;
	}
	
	private int slaveTest() throws TransferException, IOException{
		log.debug(String.format("Slave will receive file [%s]",file.getName()));
		int size = transfer.receiveFile(file);
		log.debug(String.format("Received file [%s] size [%d]",file.getName(),size));
		return size;
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
