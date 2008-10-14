/***************************************************************************
 *   Filename: f2ftypes.h
 *   Author: ulno
 ***************************************************************************
 *   Copyright (C) 2008 by Ulrich Norbisrath 
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
 *   Description: The types, which are uesd in F2FCore
 *   
 ***************************************************************************/

#ifndef F2FTYPES_H_
#define F2FTYPES_H_

#include <stdint.h> // We need to know what 32bit-word is, so we can still run on 64 and more
#include <time.h>

#include "f2fconfig.h"

typedef int32_t F2FWord32;

typedef char *F2FString; // The string of F2F on C layer is just an array of characters terminated with 0

typedef long F2FSize; // a length of something (is signed as it could be negative in an error case)

/** Error return codes */
typedef enum  {
	F2FErrOK = 0, /** Successfull operation */
	F2FErrCreationFailed = -1, /** an object could not be created */
	F2FErrNotEnoughSeed = -2, /** Not enough input to create a seed */
	F2FErrInitNotCalled = -3, /** init must be called first */
	F2FErrListEmpty = -4, /** Trying to access an object from an empty list */
	F2FErrNotFound = -5, /** Could not find the object searched for */
	F2FErrListFull = -6, /** Not enough memory reserved to add this object */
	F2FErrNothingAvail = -7, /** Not enough memory reserved to add this object */
	F2FErrBufferFull = -8, /** Can't send this message as one of the
		* communications buffer is already full. Call f2fGroupReceive to receive
		* stuff from the buffer to empty it. */
	F2FErrNotF2FMessage = -9, /** This was not a F2F message */
	F2FErrNotAuthenticated = -10, /** This messag eis not authenticated */
	F2FErrMessageTypeUnknown = -11, /** This message type was unknown */
	F2FErrMessageTooLong = -12, /** This message is too long */
	F2FErrMessageTooShort = -13, /** This message is too short to be parsed */
	F2FErrFileOpen = -14, /** Problems opening a file */
	F2FErrFileRead = -15, /** Problems reading from a file */
	F2FErrPathTooLong = -16, /** Path too long */
	F2FErrWierdError = -200, /** This error should not happen */
} F2FError;

/** the unique identifier used in F2F 
 * first this will be only 64 bits, which should be ok, if there are about max. 1024 clients in a frid */
typedef struct F2FUIDStruct
{
	F2FWord32 hi;
	F2FWord32 lo;
} F2FUID;

#include "f2fticket.h"

#endif /*F2FTYPES_H_*/
