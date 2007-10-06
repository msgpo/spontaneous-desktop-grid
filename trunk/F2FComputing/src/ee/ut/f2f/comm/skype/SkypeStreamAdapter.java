package ee.ut.f2f.comm.skype;

import java.io.IOException;
import java.util.Hashtable;

import com.skype.SkypeException;
import com.skype.StreamAdapter;

import ee.ut.f2f.comm.Peer;
import ee.ut.f2f.util.F2FDebug;

/**
 * Manages incoming streams and forwards received messages to CommunicationListener
 * instances.
 *
 */
class SkypeStreamAdapter extends StreamAdapter
{
	Peer peer;
	SkypeServerAdapter serverAdapter;

    Hashtable<String, String[]> receivedPackages;

	/**
	 * Creates new SkypeStreamAdapter, registers incoming stream and
	 * related SkypeServerAdapter (for event forward and for access to
	 * CommunicationLayer instance).
	 *
	 * @param stream
	 * @param serverAdapter
	 */
	SkypeStreamAdapter(Peer peer, SkypeServerAdapter serverAdapter) {
		this.peer = peer;
		this.serverAdapter = serverAdapter;
		this.receivedPackages = new Hashtable<String, String[]>();
	}

	/**
	 * Is fired on Skype API new incoming message event. This method receives messages from
	 * other Skype user, decodes them forwards reassembled messages to
	 * CommunicationListener instances.
	 * <p>
	 *
	 * @see com.skype.StreamAdapter#textReceived(java.lang.String)
	 */
	public void textReceived(String receivedText) throws SkypeException {

		String msgType = receivedText.substring(0, 1);

		if (msgType.equals("1"))
		{
			messageStartReceived(receivedText);
			return;
		}
		try
		{
			if (msgType.equals("2"))
			{
				messagePieceRecieved(receivedText);
			}
			else
			{
				completeMessageReceived(receivedText);
			}
		}
		catch (Exception e)
		{
			F2FDebug.println("Error with message receival in Skype communication layer!" + e);
		}
    }

	/**
	 * Called from textReceived when message piece is received.
	 *
	 * @param receivedText
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	private void messagePieceRecieved(String receivedText) throws IOException, ClassNotFoundException
	{
		// "1;312312;123;fjpajfpoasjfpojasdüfjaspüdfjüjüasdpjfüpasjdfpüjsad"

		int msgIdEnd= receivedText.indexOf(";", 2);
		String msgId = receivedText.substring(2,msgIdEnd );

		String[] messages = receivedPackages.get(peer.getID() + ":" + msgId);

		int msgCounterEnd = receivedText.indexOf(";", msgIdEnd + 1);
		String msgCounterStr = receivedText.substring(msgIdEnd + 1, msgCounterEnd );
		int msgCounter = Integer.parseInt(msgCounterStr);

		String msg = receivedText.substring(msgCounterEnd + 1 );

		messages[msgCounter] = msg;

		serverAdapter.messageReceiveProgress(msgId, msgCounter, peer);

		if (msgCounter == messages.length-1) {

			StringBuffer completeMessage = new StringBuffer();

			for (String glueyMsg : messages) {
				completeMessage.append(glueyMsg);
			}

			SkypeMessage message = new SkypeMessage(completeMessage.toString());
			serverAdapter.messageRecieved(message.getMessage(), peer);
		
			receivedPackages.remove(peer.getID() + ":" + msgId);

		}
	}


	/**
	 * Called from textReceived when message receive start message is received.
	 *
	 * @param receivedText
	 */
	private void messageStartReceived(String receivedText)
	{
		// "1;312312;123"

		int msgIdEnd= receivedText.indexOf(";", 2);
		String msgId = receivedText.substring(2,msgIdEnd );

		String msgCounterStr = receivedText.substring(msgIdEnd + 1);

		int msgCounter = Integer.parseInt(msgCounterStr);

		String[] messages = new String[msgCounter];

		receivedPackages.put(peer.getID() + ":" + msgId, messages);

		serverAdapter.messageReceiveStarted(msgId, msgCounter, peer);
	}

	/**
	 * Called from textReceived when complete message is received.
	 *
	 * @param receivedText
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	private void completeMessageReceived(String receivedText) throws IOException, ClassNotFoundException
	{
		serverAdapter.messageReceiveStarted("", 1, peer);
		SkypeMessage message = new SkypeMessage(receivedText.substring(2));
		serverAdapter.messageRecieved(message.getMessage(), peer);
	}
}

