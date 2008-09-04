/***************************************************************************
 *   Filename: f2fcommunicationprovider.h
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
 *   Description:
 *   Control of the different communication providers.
 ***************************************************************************/

#ifndef F2FCOMMUNICATIONPROVIDER_H_
#define F2FCOMMUNICATIONPROVIDER_H_

/* static list with providers including their send and receive stuff*/ 
/** every peer has this info (used by all providers) */
typedef struct
{
	int activeprovider; /** the active provider. The one over which data is send and from which data is received */
	/** Local Ip number */
	/** Ip number of router */
	/** which providers are trying to connect, in which state are they ... */
} F2FPeerCommunicationProviderInfo;

#endif /*F2FCOMMUNICATIONPROVIDER_H_*/
