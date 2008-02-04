package ee.ut.f2f.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import ee.ut.f2f.util.logging.Logger;

public class F2FProperties
{
	public static final String F2F_PROPERTIES_FILE 	= "ee.ut.f2f.F2F_PROPERTIES_FILE";
	final private static Logger log = Logger.getLogger(F2FProperties.class);
	
	private static final String PROPERTIES_FILE_DEFAULT_NAME = "F2FComputing.properties";
	
	public final static int DEFAULT_SOCKET_COMMUNICATION_PORT = 13000;
	
	static F2FProperties instance = null;
	public static F2FProperties getF2FProperties()
	{
		if (instance == null)
		{
			synchronized (F2FProperties.class)
			{
				if (instance == null)
					instance = new F2FProperties();
			}
		}
		return instance;
	}
	
	private CommLayerProperties commLayerProperties = null;
	public CommLayerProperties getCommLayerProperties() { return commLayerProperties; }
	
	private STUNProperties stunProperties = null;
	public STUNProperties getSTUNProperties() { return stunProperties; }
	
	private F2FProperties()
	{
		readProps();
	}
	
	/**
	 * @return reference to the object holding all required properties for the app.
	 */
	private void readProps()
	{
		commLayerProperties = new CommLayerProperties();
		stunProperties = new STUNProperties();
		
		// get the location of the properties file
		// at first check whether user has specified a custom name in the system properties
		String propertiesFileLocation = System.getProperty(F2F_PROPERTIES_FILE);
		if (propertiesFileLocation == null || propertiesFileLocation.trim().length() == 0)
		{
			propertiesFileLocation = PROPERTIES_FILE_DEFAULT_NAME;
		}
		
		// Parse the properties file and fill the gaps.
		Properties propsFromFile = new Properties();
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(new File(propertiesFileLocation));
			propsFromFile.load(fis); 
		}
		catch (IOException ie)
		{  
			// File was not found.
			propsFromFile = null;
		}
		finally
		{
			if (fis!=null)
				try { fis.close(); }
				catch (IOException e) { /* We did the best we could. */ }
		}
		
		// Read the properties if they exist.
		if (propsFromFile == null) return;
		commLayerProperties.bInitSkype = Boolean.parseBoolean(propsFromFile.getProperty("skype"));
		commLayerProperties.bInitSocket = Boolean.parseBoolean(propsFromFile.getProperty("socket"));
		commLayerProperties.iSocketCommunicationDefaultPort = Integer.parseInt(propsFromFile.getProperty("socketCommunicationDefaultPort", ""+DEFAULT_SOCKET_COMMUNICATION_PORT));
		commLayerProperties.bInitSip = Boolean.parseBoolean(propsFromFile.getProperty("sip"));
		// Client's IP and host
		commLayerProperties.socketLayerProps.local = getAddrFromString(propsFromFile.getProperty("local"));
		// Friends
		String friends = propsFromFile.getProperty("friends");
		if (friends != null && !(friends = friends.trim()).equals(""))
		{
			commLayerProperties.socketLayerProps.friends = new ArrayList<InetSocketAddress>();
			for (String friend : friends.split(","))
			{
				InetSocketAddress friendAddr = getAddrFromString(friend);
				if (friendAddr!=null) commLayerProperties.socketLayerProps.friends.add(friendAddr);
			}
		}
		
		// load stun servers from the properties
		String[] sStunServers = propsFromFile.getProperty("stunServers", "").split(",");
		if (sStunServers == null || sStunServers.length == 0)
		{
			log.warn("No STUN servers specified in the properties file");
		}
		else stunProperties.stunServers = Arrays.asList(sStunServers);
	}
	
	/**
	 * @param str address in format <code>[IP|HOSTNAME}:PORT</code>
	 * @return <code>InetSocketAddress</code> from the format; <code>null</code>
	 * if any parsing error occures.
	 */
	private static InetSocketAddress getAddrFromString(String str)
	{
		// Parameter checking.
		if (str==null || (str=str.trim()).equals("") || str.indexOf(":")<=0)
			return null;
		String[] strSplit = str.split(":");
		if (strSplit.length!=2) return null;
		InetSocketAddress interAddrRet = null;
		try
		{
			interAddrRet = new InetSocketAddress(strSplit[0], Integer.parseInt(strSplit[1]));
		}
		catch (Exception e)
		{
			interAddrRet = null;
		}
		return interAddrRet;
	}

	/**
	 * POJO for holding the properties of the program.  
	 */
	public class CommLayerProperties
	{
		/** Shows if Skype communication layer should be initialzed. */
		public boolean bInitSkype = false;
		/** Shows if Socket communication layer should be initialzed. */
		public boolean bInitSocket = false;
		public int iSocketCommunicationDefaultPort = -1;
		/** Shows if Socket communication layer should be initialzed. */
		public boolean bInitSip = false;
		/** Properties for Socket communication layer. */
		public SocketProviderProperties socketLayerProps = new SocketProviderProperties();
	}
	
	/**
	 * POJO for holding the properties of the Socket communication layer.  
	 */
	public class SocketProviderProperties
	{
		/** IP and port of local peer */
		public InetSocketAddress local;
		/** List of friends. <code>null</code> if not defined. */
		public Collection<InetSocketAddress> friends;
	}
	
	public class STUNProperties
	{
		public Collection<String> stunServers = new ArrayList<String>();
	}
}
