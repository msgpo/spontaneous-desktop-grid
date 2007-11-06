package ee.ut.f2f.comm.socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import ee.ut.f2f.util.F2FDebug;

/**
 * POJO for holding the properties of the program.  
 */
public final class SocketLayerProperties
{
	//private static final Logger LOG = LogManager.getLogger(SocketLayerProperties.class);
	private SocketLayerProperties() {}
	
	/** This program's IP and port. */
	public InetSocketAddress local;
	/** List of friends. <code>null</code> if not defined. */
	public Collection<InetSocketAddress> friends;
	
	/**
	 * The default name of the properties file.
	 */
	private static final String PROPERTIES_FILE_DEFAULT_NAME = "f2f.properties";
	private static final String PARAMETER_FOR_CUSTOM_PROPERTY_FILE = "-p";
	
	/**
	 * @param args parameters from the command line.
	 * @return reference to the object holding all required properties for the app.
	 * <code>null</code> if some parameters were errornous.
	 */
	public static SocketLayerProperties readProps(String[] args)
	{
		// The return POJO
		SocketLayerProperties props = new SocketLayerProperties();
		
		// Scan for PARAMETER_FOR_CUSTOM_PROPERTY_FILE.
		// If encountered, read the properties file defined next after it.
		// At the same time continue reading the parameters.
		String propertiesFileLocation = PROPERTIES_FILE_DEFAULT_NAME;
		String localIP = null;
		int localPORT = -1;
		
		// Read all the arguments.
		if (args != null)
		{
			for (int i=0; i<args.length; i++)
			{
				if (args[i].equals(PARAMETER_FOR_CUSTOM_PROPERTY_FILE))
				{
					if (++i<args.length)
						propertiesFileLocation=args[i];
					else
						propertiesFileLocation=null;
					continue;
				}
				
				// Else, another parameter to read in sequence.
				if (localIP==null) localIP=args[i];
				else if (localPORT==-1) localPORT=Integer.parseInt(args[i]);
			}
		}
		
		// Properties file was reffered but its location was not set.
		if (propertiesFileLocation==null)
		{
			F2FDebug.println("\t\tCustom property file was not defined!");
			return null;
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
		if (propsFromFile!=null)
		{
			// Client's IP and host defined?
			props.local = getAddrFromString(propsFromFile.getProperty("local"));
			if (props.local != null && localIP == null && localPORT<0)
			{
				localIP = props.local.getHostName();
				localPORT = props.local.getPort();
			}
			// Friends?
			String friends = propsFromFile.getProperty("friends");
			if (friends != null && !(friends = friends.trim()).equals(""))
			{
				props.friends = new ArrayList<InetSocketAddress>();
				for (String friend : friends.split(","))
				{
					InetSocketAddress friendAddr = getAddrFromString(friend);
					if (friendAddr!=null) props.friends.add(friendAddr);
				}
			}
		}
		
		// Final check.
		if (localIP == null || localPORT < 0)
		{
			F2FDebug.println("\t\tLocal IP and/or PORT is undefined!");
			return null;
		}
		
		// All properties read. Create the return POJO properties.
		props.local = new InetSocketAddress(localIP, localPORT);
		
		// All done - Return properties POJO
		return props;
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
}