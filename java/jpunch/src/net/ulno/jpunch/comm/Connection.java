/***************************************************************************
 *   Filename: Connection.java
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
 *   Connection interface of the jPunch library
 ***************************************************************************/
package net.ulno.jpunch.comm;

import net.ulno.jpunch.exceptions.ConnectionException;

/**
 * Connection interface of the jPunch library
 * 
 * @author artjom.lind@ut.ee
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
