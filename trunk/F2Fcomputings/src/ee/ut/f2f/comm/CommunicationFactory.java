package ee.ut.f2f.comm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import ee.ut.f2f.comm.sip.SipCommunicationLayer;
import ee.ut.f2f.comm.skype.SkypeCommunicationLayer;
import ee.ut.f2f.comm.socket.SocketCommunicationLayer;
import ee.ut.f2f.util.F2FDebug;

/**
 * Factory for creating new CommunicationLayer instances.
 */
public class CommunicationFactory
{
	private CommunicationFactory(){};
	
	private static final String PROPERTIES_FILE_DEFAULT_NAME = "F2FComputing.properties";

	public static Collection<CommunicationLayer> getInitializedCommunicationLayers()
	{
		Collection<CommunicationLayer> commLayers = new ArrayList<CommunicationLayer>();
		
		// initialize layers according to properties file
		final CommLayerProperties props = readProps();
		if (props == null)
		{
			return commLayers;
		}
		
		CommunicationLayer commLayer = null;
		if (props.bInitSkype)
		{
			F2FDebug.println("\t\tINIT SKYPE");
			try
			{
				commLayer = GetSkypeCommununication();
				commLayers.add(commLayer);
			}
			catch (CommunicationException e)
			{
				F2FDebug.println("\t\tGetSkypeCommununication() throwed CommunicationInitException! " + e);
			}
		}
		if (props.bInitSocket)
		{
			F2FDebug.println("\t\tINIT SOCKET");
			try
			{
				commLayers.add(GetSocketCommununication(props.socketLayerProps));
			}
			catch (CommunicationInitException e)
			{
				F2FDebug.println("\t\tGetSocketCommununication() throwed CommunicationInitException!" + e);
			}
		}
		if (props.bInitSip)
		{
			F2FDebug.println("\t\tINIT SIP");
			commLayer = GetSipCommununication();
			if (commLayer != null) commLayers.add(commLayer);
			else
			{
				F2FDebug.println("\t\tGetSipCommununication() returned NULL");
			}
		}
		return commLayers;
	}
	
	/**
	 * @return reference to the object holding all required properties for the app.
	 */
	private static CommLayerProperties readProps()
	{
		// The return POJO
		CommLayerProperties props = new CommLayerProperties();
		
		String propertiesFileLocation = PROPERTIES_FILE_DEFAULT_NAME;
		
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
		if (propsFromFile == null) return null;
		props.bInitSkype = Boolean.parseBoolean(propsFromFile.getProperty("skype"));
		props.bInitSocket = Boolean.parseBoolean(propsFromFile.getProperty("socket"));
		props.bInitSip = Boolean.parseBoolean(propsFromFile.getProperty("sip"));
		// Client's IP and host
		props.socketLayerProps.local = getAddrFromString(propsFromFile.getProperty("local"));
		// Friends
		String friends = propsFromFile.getProperty("friends");
		if (friends != null && !(friends = friends.trim()).equals(""))
		{
			props.socketLayerProps.friends = new ArrayList<InetSocketAddress>();
			for (String friend : friends.split(","))
			{
				InetSocketAddress friendAddr = getAddrFromString(friend);
				if (friendAddr!=null) props.socketLayerProps.friends.add(friendAddr);
			}
		}
			
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
	
	private static CommunicationLayer GetSkypeCommununication() throws CommunicationException
	{
		return new SkypeCommunicationLayer();
	}

	private static CommunicationLayer GetSocketCommununication(SocketLayerProperties props) throws CommunicationInitException
	{
		return new SocketCommunicationLayer(
				new InetSocketAddress(
					props.local.getAddress().getHostAddress(),
					props.local.getPort()),
				props.friends);
	}

	private static CommunicationLayer GetSipCommununication()
	{
		return SipCommunicationLayer.getInstance();
		/*return new SipCommunicationLayer(
				new InetSocketAddress(
					props.local.getAddress().getHostAddress(),
					props.local.getPort()),
				props.friends);*/
	}

	/**
	 * POJO for holding the properties of the program.  
	 */
	private static class CommLayerProperties
	{
		/** Shows if Skype communication layer should be initialzed. */
		boolean bInitSkype = false;
		/** Shows if Socket communication layer should be initialzed. */
		boolean bInitSocket = false;
		/** Shows if Socket communication layer should be initialzed. */
		boolean bInitSip = false;
		/** Properties for Socket communication layer. */
		SocketLayerProperties socketLayerProps = new SocketLayerProperties();
	}
	
	/**
	 * POJO for holding the properties of the Socket communication layer.  
	 */
	private static class SocketLayerProperties
	{
		/** This program's IP and port. */
		InetSocketAddress local;
		/** List of friends. <code>null</code> if not defined. */
		Collection<InetSocketAddress> friends;
	}
}
