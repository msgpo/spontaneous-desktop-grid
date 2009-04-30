package net.ulno.jpunch.comm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import net.ulno.jpunch.exceptions.ConnectionException;
import net.ulno.jpunch.exceptions.TransferException;
import net.ulno.jpunch.util.Util;

import org.apache.log4j.Logger;

/**
 * Transfer class For Send/Receive files
 * over network
 * 
 * @author artjom85
 *
 */
public class Transfer {
	
	private final static Logger log = Logger.getLogger(Transfer.class);
	
	// Default blockSize
	private final static int DEFAULT_BLOCKSIZE = 1024*1024;
	private final static long DEFALUT_BLOCKING_TIMEOUT = 100000; 
	
	private int blockSize;
	private long blockingTimeout;
	
	private Connection connection;
	
	/**
	 * Default Constructor
	 * Create new transfer using connection, block size and blocking timeout
	 * 
	 * @param messageLength 
	 * @param taskProxy
	 */
	public Transfer(Connection connection, int blocksize, long blockingTimeout) {
		this.connection = connection;
		this.blockSize = blocksize;
		this.blockingTimeout = blockingTimeout;
	}
	
	/**
	 * Default Constructor
	 * Create new transfer using connection.
	 * Uses default block size (1M) and blocking timeout (100s)
	 * 
	 * @param connection 
	 */
	public Transfer(Connection connection){
		this(connection, DEFAULT_BLOCKSIZE, DEFALUT_BLOCKING_TIMEOUT);
	}
	
	/**
	 * Receive and save the file to the hard disk, using link
	 * 
	 * @param file to be written
	 * @return amount of bytes received
	 * @throws FileNotFoundException
	 */
	public int receiveFile(File file) throws FileNotFoundException, IOException,
												TransferException{
		int bytesReceived = 0;
		
		if (!file.exists()) file.createNewFile();
		
		BufferedOutputStream stream = new BufferedOutputStream(
										new FileOutputStream(file));
		while(true){
			long start = System.currentTimeMillis();
			byte[] buffer = receiveBlock();
			long stop = System.currentTimeMillis();
			stream.write(buffer);
			log.debug(String.format("Current transfer speed [%s] kbytes/s", 
					Util.getTransferSpeed(start, stop, buffer.length)));
			bytesReceived = bytesReceived + buffer.length;
			if(buffer.length < blockSize) break;
		}
		
		stream.close();
		return bytesReceived;
	}
	
	/**
	 * Send file using link
	 * 
	 * @param file to send
	 * @return amount of bytes sent
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws TransferException
	 */
	public int sendFile(File file) throws FileNotFoundException, IOException,
											TransferException{
		int bytesSent = 0;
		BufferedInputStream stream = new BufferedInputStream(
									 new FileInputStream(file));
		int read = 0;
		boolean run = true;
		while(run){
			byte[] buffer = new byte[blockSize];
			//read into buffer
			read = stream.read(buffer);
			
			//chop buffer if end reached
			if (read < buffer.length){
				if (read <= 0){
					buffer = new byte[0];
				} else {
					buffer = Arrays.copyOf(buffer, read);
				}
				run = false;
			}
			//send bytes
			long start = System.currentTimeMillis();
			sendBlock(buffer);
			long stop = System.currentTimeMillis();
			log.debug(String.format("Current transfer speed [%s] kbytes/s", 
					Util.getTransferSpeed(start, stop, buffer.length)));
			bytesSent = bytesSent + buffer.length;
			//System.gc();
		}
		stream.close();
		return bytesSent;
	}
	
	private synchronized byte[] receiveBlock () throws TransferException{
		try{
			byte[] block = this.connection.receiveBytes(blockingTimeout);
			if (block == null) block = new byte[0];
	
			log.debug(String.format("Received block length [%d]", block.length));
			return block;
		} catch (ConnectionException e){
			throw new TransferException("Receive Failed",
					TransferException.State.FAILED);
		}
	}
	
	private synchronized void sendBlock(byte[] block) throws TransferException{
		try{
			log.debug(String.format("Send block length [%d]", block.length));
			this.connection.sendBytes(block);
		} catch (ConnectionException e2){
			throw new TransferException("Send Failure",
					TransferException.State.FAILED,e2);
		}
	}
}
