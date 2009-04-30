package net.ulno.jpunch.comm;

import net.ulno.jpunch.exceptions.ConnectionException;

/**
 * Interface for send/receive using network
 * 
 * @author artjom.lind
 *
 */
public interface Connection extends Runnable {
	
	/**
	 * Returns amount of bytes successfully sent
	 * 
	 * @param bytes to be sent
	 * @return amount of bytes sent
	 * @throws ConnectionException in case of connection failures
	 */
	public int sendBytes(byte[] bytes) throws ConnectionException;
	
	/**
	 * Returns received array of bytes
	 * blocks until the timeout occurs
	 * 
	 * @param long timeout, time in milliseconds 
	 * @return array of bytes received
	 * @throws ConnectionException in case of connection failures
	 */
	public byte[] receiveBytes(long timeout) throws ConnectionException;
}
