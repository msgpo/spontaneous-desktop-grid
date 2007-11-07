//package ee.ut.f2f.comm.sip;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//import org.p2psockets.P2PServerSocket;
//
//import ee.ut.f2f.comm.CommunicationException;
//import ee.ut.f2f.util.F2FDebug;
//
//class SipIDPublisher implements Runnable
//{
//	/*
//	 * This socket listenes for incoming connections from other peers.
//	 */
//	private ServerSocket serverSocket;
//	private String localUID;
//	private String localDisplayName;
//	
//	SipIDPublisher(String localAddress, String localUID, String localDisplayName) throws CommunicationException
//	{
//		this.localUID = localUID;
//		this.localDisplayName = localDisplayName;
//		try
//		{
//			// create a publisher socket
//			//F2FDebug.println("\t\tCREATE PUBLISHER SOCKET ["+ localAddress + "]...");
//			serverSocket = new P2PServerSocket(localAddress, SipCommunicationLayer.SIP_LAYER_NETWORK_PORT);
//			//F2FDebug.println("\t\tSIP PUBLISHER INITIALIZED.");
//		}
//		catch (IOException e)
//		{
//			throw new CommunicationException("Could not create JXTA's P2PServerSocket for "+localAddress+" to publish ID "+localUID+" of SIP communication layer! ", e);
//		}
//	}
//	
//	public void run()
//	{
//		while(true)
//		{
//			try
//			{
//				// wait while someone tries to connect
//				Socket socket = serverSocket.accept();
//				//F2FDebug.println("\t\tACCEPTED A request CONNECTION ...");
//				SipObjectInput oi = new SipObjectInput(socket.getInputStream());
//				
//				// handshake
//				//F2FDebug.println("\t\tREAD PASSWORD ...");
//				String password = (String)oi.readObject();
//				//F2FDebug.println("\t\tREAD PASSWORD '" + password + "'.");
//				boolean bHandshake = password.equals(SipCommunicationLayer.SIP_LAYER_NETWORK_PASSWORD);
//				if (bHandshake)
//				{
//					// get output stream and write our SipID into it
//					SipObjectOutput oo = new SipObjectOutput(socket.getOutputStream());
//					oo.writeObject(localUID);
//					oo.writeObject(localDisplayName);
//				}
//				else
//				{
//					F2FDebug.println("\t\tUnauthorized connection attempt. Read password was '" + 
//							password + "'.");
//				}
//				socket.close();
//			}
//			catch (Exception e)
//			{
//				F2FDebug.println("\t\tProblems with publishing SIP layer ID: " + e);
//			}
//		}
//	}
//}