package ee.ut.f2f.core;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.UUID;

import ee.ut.f2f.comm.CommunicationFailedException;
import ee.ut.f2f.comm.CommunicationProvider;
import ee.ut.f2f.comm.socket.SocketCommInitiator;
import ee.ut.f2f.comm.socket.TCPTester;
import ee.ut.f2f.util.F2FMessage;
import ee.ut.f2f.util.logging.Logger;
import ee.ut.f2f.util.stun.StunInfo;
import ee.ut.f2f.util.stun.StunInfoClient;

public class F2FPeer
{
	private static final Logger logger = Logger.getLogger(F2FPeer.class);
	
	private UUID id = null;
	public UUID getID() { return id; }
	
	private String displayName = null;
	public String getDisplayName() { return displayName; }
	public String toString() { return displayName; }

	/**
	 * Used only for the local peer creation.
	 *
	 * TODO: remove the hack for Chat
	 * make this method not public
	 */	
	public F2FPeer(String displayName)
	{
		this.id = UUID.randomUUID();
		this.displayName = displayName;
		reportSTUNPeers = new ArrayList<F2FPeer>();
	}
	
	/**
	 * Used for a remote peer creation.
	 * @param id
	 * @param displayName
	 */
	F2FPeer(UUID id, String displayName, CommunicationProvider provider)
	{
		this.id = id;
		this.displayName = displayName;
		this.commProviders = new ArrayList<CommunicationProvider>();
		addCommProvider(provider);
		updateSTUNInfo();
		initiateTCPTester();
	}
	
	boolean isContactable()
	{
		return commProviders.size() > 0;
	}
	
	private ArrayList<CommunicationProvider> commProviders = null;
	/**
	 * 
	 * @param comm The provider to add
	 * @return The place in queue where the new provider was added, or -1 if already present
	 * or -2 if comm is null.
	 */
	int addCommProvider(CommunicationProvider comm)
	{
		if (comm == null) return -2;
		synchronized (commProviders)
		{
			if (!commProviders.contains(comm))
			{
				int place = 0;
				for (; place < commProviders.size(); place++)
					if (commProviders.get(place).getWeight() <= comm.getWeight()) break;
				commProviders.add(place, comm);
				return place;
			}
			return -1;
		}
	}
	void removeCommProvider(CommunicationProvider comm)
	{
		synchronized (commProviders)
		{
			if (commProviders.contains(comm))
			{
				commProviders.remove(comm);
				//logger.debug("Removed CommunicationProvider [" + comm + "] from F2FPeer [" + this.getID().toString() + "]");
			}
			else
			{
				//logger.debug("CommunicationProvider [" + comm + "] not found in list, nothing removed");
			}
			//TODO: remove, if it is not needed
			//if (commProviders.size() == 1 && commProviders.contains(getSocketCommunicationProvider()))
			//	commProviders.remove(getSocketCommunicationProvider());
		}
	}
	public void sendMessage(Object message) throws CommunicationFailedException
	{
		logger.info("TO "+getDisplayName()+": "+message);
		
		// Loopback
		if (this.id.equals(F2FComputing.getLocalPeer().getID()))
		{
			logger.debug("Sending F2FMessage to MYSELF - loopback");
			F2FComputing.messageRecieved(message, this.getID());			
			return;
		}
		
		// try to send the message to the receiver
		// use high-weight comm providers before low-weight ones
		for (int i = 0; i < commProviders.size(); i++)
		{
			CommunicationProvider commProvider = commProviders.get(i);
			try
			{	
				//logger.debug("Using SipCommucationProvider sending F2FMessage to [" + this.getID().toString() + "]");
				commProvider.sendMessage(id, message);
				//logger.debug("Message successfully sent using SipCommunicationProvider");
			}
			catch (Exception e)
			{
				logger.warn("Error sending message to "+getDisplayName()+" through "+commProvider.getClass());
				e.printStackTrace();
				// try again with different communication provider
				continue;
			}
			// return if message was sent successfully
			return;
		}
		// throw an exception if message is not sent
		throw new CommunicationFailedException("peer " + getDisplayName() + " is not reachable");
	}
	
	private StunInfoClient stunInfoClient = null;
	private StunInfo stunInfo = null;
	public StunInfo getSTUNInfo()
	{
		return stunInfo; 
	}
	public void setSTUNInfo(StunInfo stunInf)
	{
		stunInfo = stunInf;
		if (F2FComputing.getLocalPeer().equals(this))
		{
		// init the processes that have to be done
		// after the local peer has has got its STUN info
			
			// send the STUN info to the peers that have asked for it
			synchronized (reportSTUNPeers)
			{
				for (F2FPeer peer: reportSTUNPeers)
					reportSTUNInfo(peer);
				reportSTUNPeers.clear();
			}
		}
		else
		{
			logger.info("received STUN info from " + this.displayName);
		// init the processes that have to be done
		// after a remote peer has sent its STUN info
		
			
		}
	}
	
	private TCPTester tcpTester = null;
	TCPTester getTCPTester()
	{
		if (tcpTester != null) return tcpTester;
		synchronized(this)
		{
			if (tcpTester != null) return tcpTester;
			tcpTester = new TCPTester(this);
		}
		return tcpTester;
	}
	public void initiateTCPTester()
	{
		TCPTester tcpTester = getTCPTester();
		if (tcpTester.isAlive())
			return;
		tcpTester.start();
	}
	
	private Collection<InetAddress> localIPs = null;
	public Collection<InetAddress> getLocalIPs() { return localIPs; }
	void updateLocalIPInfo()
	{
		localIPs = new ArrayList<InetAddress>();
		// get the network interfaces
		Enumeration<NetworkInterface> interfaces;
		try
		{
			interfaces = NetworkInterface.getNetworkInterfaces();
		}
		catch (SocketException e)
		{
			e.printStackTrace();
			logger.debug(e.getMessage());
			return;
		}
		
		// get local IP's
		while (interfaces.hasMoreElements())
		{
			NetworkInterface inet = interfaces.nextElement();
			Enumeration<InetAddress> ips = inet.getInetAddresses();
			while(ips.hasMoreElements())
			{
				InetAddress ip = ips.nextElement();
				if( !ip.isLinkLocalAddress() && !ip.isLoopbackAddress() )
				{
					if(ip instanceof Inet4Address) localIPs.add(ip);
				}
			}
		}
		
		// start a SocketComm provider on each local IP
		new SocketCommInitiator().start();
		
		// start STUN info update thread
		updateSTUNInfo();
	}
	
	public void updateSTUNInfo()
	{
		if (F2FComputing.getLocalPeer() == null) return;
		// update the local STUN info ...
		if(F2FComputing.getLocalPeer().equals(this))
		{
			if (stunInfoClient == null)
			{
				try
				{
					stunInfoClient = new StunInfoClient();
				}
				catch (Exception e)
				{
					logger.error(e.getMessage());
					return;
				}
			}
			stunInfoClient.updateSTUNInfo();
		}
		// ... or ask update for a remote peer's STUN info 
		else
		{
			F2FMessage msg = new F2FMessage(F2FMessage.Type.GET_STUN_INFO, null, null, null, null);
			try
			{
				sendMessage(msg);
			}
			catch (CommunicationFailedException e)
			{
				logger.debug("could not send GET_STUN_INFO to " + getDisplayName());
			}
		}
	}

	private ArrayList<F2FPeer> reportSTUNPeers = null;
	void reportSTUNInfo(F2FPeer remotePeer)
	{
		if(!F2FComputing.getLocalPeer().equals(this)) return;
		
		if (stunInfo != null)
		{
			F2FMessage msg = new F2FMessage(F2FMessage.Type.REPORT_STUN_INFO, null, null, null, stunInfo);
			try
			{
				remotePeer.sendMessage(msg);
			}
			catch (CommunicationFailedException e)
			{
				logger.error("could not send REPORT_STUN_INFO to " + remotePeer.getDisplayName());
			}
		}
		else
		{
			synchronized (reportSTUNPeers)
			{
				reportSTUNPeers.add(remotePeer);
			}
		}
	}
}