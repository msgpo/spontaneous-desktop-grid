package ee.ut.f2f.comm;

import java.util.ArrayList;
import java.util.Collection;

import ee.ut.f2f.comm.sc.im.SipIMCommunicationProvider;
import ee.ut.f2f.comm.tcp.SocketCommunicationProvider;
import ee.ut.f2f.util.F2FProperties;
import ee.ut.f2f.util.F2FProperties.CommLayerProperties;
import ee.ut.f2f.util.F2FProperties.SocketProviderProperties;
import ee.ut.f2f.util.logging.Logger;

/**
 * Factory for creating new CommunicationLayer instances.
 */
public class CommunicationFactory
{
	final private static Logger log = Logger.getLogger(CommunicationFactory.class);
	
	private CommunicationFactory(){};
	
	public static Collection<CommunicationProvider> getInitializedCommunicationProviders()
	{
		Collection<CommunicationProvider> commProviders = new ArrayList<CommunicationProvider>();
		
		// initialize layers according to properties file
		final CommLayerProperties props = F2FProperties.getF2FProperties().getCommLayerProperties();
		if (props == null)
		{
			log.debug("props == null");
			return commProviders;
		}
		
		CommunicationProvider commProvider = null;
		if (props.bInitSocket)
		{
			log.debug("INIT SOCKET");
			try
			{
				commProvider = GetSocketCommununication(props.socketLayerProps);
				if (commProvider != null) commProviders.add(commProvider);
			}
			catch (CommunicationInitException e)
			{
				log.debug("GetSocketCommununication() throwed CommunicationInitException!" + e);
			}
		}
		
		if (props.bInitSip)
		{
			log.debug("INIT SIP");
			commProvider = GetSipCommununication();
			if (commProvider != null) commProviders.add(commProvider);
			else
			{
				log.debug("GetSipCommununication() returned NULL");
			}
		}

		
		return commProviders;
	}	
	
	private static CommunicationProvider GetSocketCommununication(SocketProviderProperties props) throws CommunicationInitException
	{
		SocketCommunicationProvider socketProvider = SocketCommunicationProvider.getInstance();
		if (props != null)
		{
			socketProvider.addServerSocket(props.local);
			socketProvider.addFriends(props.friends);
		}
		return socketProvider;
	}

	private static CommunicationProvider GetSipCommununication()
	{
		// also init TCP communication provider
		SocketCommunicationProvider.getInstance();
		return SipIMCommunicationProvider.getInstance();
	}
}
