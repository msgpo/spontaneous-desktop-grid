/***************************************************************************
 *   Filename: StunInfo.java
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
 *   Value class for holding network specific information
 ***************************************************************************/
package net.ulno.jpunch.util.stun;

import java.io.Serializable;

import de.javawi.jstun.test.DiscoveryInfo;

/**
 * Value class for holding network specific information
 * @author artjom.lind@ut.ee
 *
 */
public class StunInfo implements Serializable
{
	private static final long serialVersionUID = 4721470383254301632L;
	
	private boolean openAccess = false;
	private boolean blockedUDP = false;
	private boolean fullCone = false;
	private boolean restrictedCone = false;
	private boolean portRestrictedCone = false;
	private boolean symmetricCone = false;
	private boolean symmetricUDPFirewall = false;
	private String publicIp = null;
	private String localIp = null;

	public StunInfo(){
		
	}
	
	public StunInfo(DiscoveryInfo discoveryInfo) {
		if(discoveryInfo == null) throw new NullPointerException("Null argument discoveryInfo");
		this.openAccess = discoveryInfo.isOpenAccess();
		this.blockedUDP = discoveryInfo.isBlockedUDP();
		this.fullCone = discoveryInfo.isFullCone();
		this.restrictedCone = discoveryInfo.isRestrictedCone();
		this.portRestrictedCone = discoveryInfo.isPortRestrictedCone();
		this.symmetricCone = discoveryInfo.isSymmetricCone();
		this.symmetricUDPFirewall = discoveryInfo.isSymmetricUDPFirewall();
		if (discoveryInfo.getPublicIP() != null){
			this.publicIp = discoveryInfo.getPublicIP().getHostAddress();
		}
	}
	
	public boolean isOpenAccess() {
		return openAccess;
	}

	public void setOpenAccess(boolean openAccess) {
		this.openAccess = openAccess;
	}

	public boolean isBlockedUDP() {
		return blockedUDP;
	}

	public void setBlockedUDP(boolean blockedUDP) {
		this.blockedUDP = blockedUDP;
	}

	public boolean isFullCone() {
		return fullCone;
	}

	public void setFullCone(boolean fullCone) {
		this.fullCone = fullCone;
	}

	public boolean isRestrictedCone() {
		return restrictedCone;
	}

	public void setRestrictedCone(boolean restrictedCone) {
		this.restrictedCone = restrictedCone;
	}

	public boolean isPortRestrictedCone() {
		return portRestrictedCone;
	}

	public void setPortRestrictedCone(boolean portRestrictedCone) {
		this.portRestrictedCone = portRestrictedCone;
	}

	public boolean isSymmetricCone() {
		return symmetricCone;
	}

	public void setSymmetricCone(boolean symmetricCone) {
		this.symmetricCone = symmetricCone;
	}

	public boolean isSymmetricUDPFirewall() {
		return symmetricUDPFirewall;
	}

	public void setSymmetricUDPFirewall(boolean symmetricUDPFirewall) {
		this.symmetricUDPFirewall = symmetricUDPFirewall;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public String getLocalIp() {
		return localIp;
	}

	public void setLocalIP(String localIp) {
		this.localIp = localIp;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("Local IP address: ");
		sb.append(localIp);
		sb.append("\n");
		
		sb.append("Result: ");
		if (openAccess) sb.append("Open access to the Internet.\n");
		if (blockedUDP) sb.append("Firewall blocks UDP.\n");
		if (fullCone) sb.append("Full Cone NAT handles connections.\n");
		if (restrictedCone) sb.append("Restricted Cone NAT handles connections.\n");
		if (portRestrictedCone) sb.append("Port restricted Cone NAT handles connections.\n");
		if (symmetricCone) sb.append("Symmetric Cone NAT handles connections.\n");
		if (symmetricUDPFirewall) sb.append ("Symmetric UDP Firewall handles connections.\n");
		if (!openAccess && !blockedUDP && !fullCone && !restrictedCone && !portRestrictedCone && !symmetricCone && !symmetricUDPFirewall) sb.append("unkown\n");
		sb.append("Public IP address: ");
		if (publicIp != null) {
			sb.append(publicIp);
		} else {
			sb.append("unknown");
		}
		sb.append("\n]");
		
		return sb.toString();
	}
}
