
/***************************************************************************
 *   Filename: main.c
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
 *   As F2FCore is not standalone, here is just some test-stuff for it.
 ***************************************************************************/

#include <stdio.h>

#include "f2fcore.h"
#include "f2fpeerlist.h"

/* F2FError mySendIM ( F2FWord32 localPeerID, F2FString message )
{
	printf("mySendIM sends: %s\n", message);
	return F2FErrOK;
}*/

/*int main()
{
	char *test [2];

	char *test2 = "Hallo";

	test[1] = test2;

	F2FPeer *myPeerId;

	//f2fInit("ulnotes", "", &mySendIM, &myPeerId);

	f2fPeerListNew(10,10);
	f2fPeerListNew(10,3);
	f2fPeerListNew(12,10);
	f2fPeerListNew(2,10);
	f2fPeerListNew(14,0);

	F2FPeer *testpeer = f2fPeerListFindPeer( 12, 10 );

	f2fPeerListRemove( testpeer );

	printf("Hello\n");

	return 0;
}*/

