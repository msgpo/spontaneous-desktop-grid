package ee.ut.f2f.comm.sip;

import java.net.Socket;

import org.p2psockets.P2PSocket;

class SipIDRequester
{
	private String peerAddress; 
	SipIDRequester(String peerAddress)
	{
		this.peerAddress = peerAddress;
	}
	
	String getSipID()
	{
		String id = null;
		try
		{
			// connect to a SipIDPublisher according to peerAddress 
			Socket socket = new P2PSocket(peerAddress, SipCommunicationLayer.SIP_LAYER_NETWORK_PORT);
			SipObjectOutput oo = new SipObjectOutput(socket.getOutputStream());
			oo.writeObject(SipCommunicationLayer.SIP_LAYER_NETWORK_PASSWORD);
			SipObjectInput oi = new SipObjectInput(socket.getInputStream());
			id = (String)oi.readObject();
			//read also display name
			oi.readObject();
			socket.close();
		}
		catch (Exception e)
		{
			return null;
		}
		return id;
	}
}