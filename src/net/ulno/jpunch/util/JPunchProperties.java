/***************************************************************************
 *   Filename: JPunchProperties.java
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
 *   Handles jPunch properties
 ***************************************************************************/
package net.ulno.jpunch.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Handles jPunch properties
 * @author artjom.lind@ut.ee
 *
 */
public class JPunchProperties {
	private final static Logger log = Logger.getLogger(JPunchProperties.class);
	
	// Constants
	// property names
	private final static String JPUNCH_PROPERTIES_FILE = "net.ulno.jpunch.PropertiesFile";
	
	// network discovery properties
	public final static String STUN_NOT_FILTERED_SERVERS = 
		"net.ulno.jpunch.stun.NotFilteredServers";
	public final static String STUN_SERVER_PORT = "net.ulno.jpunch.stun.ServerPort";
	public final static String STUN_FILTERING_TIMEOUT = 
		"net.ulno.jpunch.stun.FilteringTimeout";

	// hole punching properties
	public final static String HP_PING_INTERVAL = "net.ulno.jpunch.hp.PingInterval";
	public final static String HP_BINDING_RULE = "net.ulno.jpunch.hp.BindingRule";
	public final static String HP_MAX_BINDING_ERRORS = 
		"net.ulno.jpunch.hp.MaxBindingErrors";
	public final static String HP_BOUND_ADDRESS_RESOLVE_SO_TIMEOUT = 
		"net.ulno.jpunch.hp.BoundAddressResolveSoTimeout";
	public final static String HP_TEST_SO_TIMEOUT = 
		"net.ulno.jpunch.hp.TestSoTimeout";
	public final static String HP_TEST_RUN_TIMEOUT =
		"net.ulno.jpunch.hp.TestRunTimeout";
	
	// Internet connection test properties
	public final static String ITEST_MAX_TTL = "net.ulno.jpunch.itest.MaxTTL";
	public final static String ITEST_PING_HOST = "net.ulno.jpunch.itest.PingHost";
	public final static String ITEST_PING_PORT = "net.ulno.jpunch.itest.PingPort";
	public final static String ITEST_PING_TIMEOUT = "net.ulno.jpunch.itest.PingTimeout";
	
	// UDP Connection properties
	public final static String UDP_MAX_PACKET_SIZE = "net.ulno.jpunch.udp.MaxPacketSize";

	private static Properties jPunchProperties = null;
	
	/*
	 * Returns JPunch Properties
	 *  
	 * @return Properties, null if error occurs or property set is empty  
	 */
	private static Properties getProperties() {
		if (jPunchProperties == null) {
			String filename = System.getProperty(JPUNCH_PROPERTIES_FILE);
			if ( filename == null ){
				log.fatal("No filename in property [" + JPUNCH_PROPERTIES_FILE + "]");
				return null;
			}
			try {
				jPunchProperties = loadPoperties(filename);
				log.debug("Loaded [" + jPunchProperties.size() + "] "
									+ "properties from "
									+ "[" + filename + "]");
			} catch (FileNotFoundException e) {
				log.fatal("[" + filename + "] file not found",e);
				return null;
			} catch (IOException e) {
				log.fatal("I/O error reading file [" + filename + "]",e);
				return null;
			}
		}
		// if no properties in file
		if (jPunchProperties.size() == 0) {
			log.fatal("No properties in file [" + JPUNCH_PROPERTIES_FILE + "]");
			return null;
		}
		return jPunchProperties;
	}
	
	/*
	 * Returns properties loaded form the given file
	 * throws exception if no files found with such
	 * throws IOException in case of I/O errors
	 */
	private static Properties loadPoperties(String file) throws FileNotFoundException, IOException{
		Properties properties = new Properties();
		BufferedInputStream bufferedInputStream = 
			new BufferedInputStream(new FileInputStream(file));
		properties.load(bufferedInputStream);
		bufferedInputStream.close();
		return properties;
	}
	
	/**
	 * Returns numeric property by name
	 * Reads properties from <code>jpunch.properties</code> file
	 * 
	 * @param String propertyName
	 * @return Integer property numeric value
	 */
	public static Integer getIntegerProperty(String propertyName){
		String value = getStringProperty(propertyName);
		if (value == null) return null;
		try{
			int intv = Integer.parseInt(value);
			return new Integer(intv);
		} catch (NumberFormatException e){
			log.error("No numeric property by name [" + propertyName + "]");
			return null;
		}
	}
	
	/**
	 * Returns property by name
	 * Reads properties from <code>jpunch.properties</code> file
	 * 
	 * @param String propertyName
	 * @return String property value
	 */
	public static String getStringProperty(String propertyName){
		Properties props = getProperties();
		if (props != null){
			String value = props.getProperty(propertyName);
			if (value == null) log.error("No value by property name [" + propertyName + "]");
			return value;
		}
		return null; 
	}
}
