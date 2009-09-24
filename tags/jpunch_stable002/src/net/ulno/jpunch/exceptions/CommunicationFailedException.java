/***************************************************************************
 *   Filename: CommunicationFailedException.java
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
 *   This exception is thrown if back-end communication between peers fails.
 ***************************************************************************/
package net.ulno.jpunch.exceptions;

/**
 * This exception is thrown if back-end communication between peers fails.
 * 
 * @author artjom.lind@ut.ee
 */
@SuppressWarnings("serial")
public class CommunicationFailedException extends Exception 
{
	public CommunicationFailedException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CommunicationFailedException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public CommunicationFailedException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public CommunicationFailedException(String msg)
	{
		super(msg);
	}

	public CommunicationFailedException(Exception e)
	{
		super(e);
	}
}
