package net.ulno.jpunch.test;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import net.ulno.jpunch.util.JPunchProperties;

/**
 * Testing jPunch properties
 * @author artjom.lind@ut.ee
 *
 */
public class JPunchPropertiesTest extends TestCase{
	private static final Logger log = Logger.getLogger(JPunchTest.class);
	
	public void testProperties(){
		String itestPingHost = JPunchProperties.getStringProperty(JPunchProperties.ITEST_PING_HOST);
		String stunNotFiltered = JPunchProperties.getStringProperty(JPunchProperties.STUN_NOT_FILTERED_SERVERS);
		Integer hpPingInterval = JPunchProperties.getIntegerProperty(JPunchProperties.HP_PING_INTERVAL);
		log.debug("Internet Test Ping Host [" + itestPingHost + "]");
		log.debug("STUN not filtered servers [" + stunNotFiltered + "]");
		log.debug("Hole Punching ping interval [" + hpPingInterval + "]");
	}
	
	public static void main(String[] args){
		junit.textui.TestRunner.run(JPunchPropertiesTest.class);
	}
}
