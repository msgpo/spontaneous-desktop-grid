/***************************************************************************
 *   Filename: TransferException.java
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
 *   Thrown if UDP file transmission fails
 ***************************************************************************/
package net.ulno.jpunch.exceptions;

/**
 * Thrown if UDP file transmission fails
 * @author artjom.lind@ut.ee
 *
 */
public class TransferException extends Exception {
	private static final long serialVersionUID = -8371831623367892322L;

	public static enum State {
		FAILED,
		ABORTED
	}
	
	private State state;
	
	public TransferException(State state) {
		super();
		this.state = state;
	}

	public TransferException(String message, State state, Throwable cause) {
		super(message, cause);
		this.state = state;
	}

	public TransferException(String message, State state) {
		super(message);
		this.state = state;
	}

	public TransferException(State state, Throwable cause) {
		super(cause);
		this.state = state;
	}
	
	public State getState(){
		return this.state;
	}
}
