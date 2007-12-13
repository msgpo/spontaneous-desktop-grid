package ee.ut.f2f.ui;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.F2FComputingException;
import ee.ut.f2f.core.F2FPeer;
import ee.ut.f2f.ui.model.FriendModel;
import ee.ut.f2f.util.F2FDebug;
import ee.ut.f2f.util.nat.traversal.ConnectionManager;
import ee.ut.f2f.util.nat.traversal.NatMessage;
import ee.ut.f2f.util.nat.traversal.NatMessageProcessor;
import ee.ut.f2f.util.nat.traversal.exceptions.ConnectionManagerException;

public class F2FComputingGUI {
	public static UIController controller;
	//temporary for testing purposes
	public static NatMessageProcessor natMessageProcessor;
	/**
	 * Different Print streams for different log4j appenders. This could be done better (?)
	 */
	//private static PrintStream debugPrintStream  = new PrintStream(new FilteredStream(new ByteArrayOutputStream()));
	//static Logger log = LogManager.getLogger(UIController.class);
	
	public static void main(final String[] args) {
		//Redirect System.out and System.err to our GUI. Do this before initializing log4j
		//because BasicConfigurator.configure() adds an appender that uses System.out
		//System.setOut(debugPrintStream);
		//System.setErr(debugPrintStream);
		
		//BasicConfigurator.configure();
		
		/*
		Layout layout = new org.apache.log4j.PatternLayout("%d [%t] %-5p %c - %m%n");
		Appender console = new ConsoleAppender (layout);
		RollingFileAppender file = new RollingFileAppender ();
		file.setLayout(layout);
		file.setFile("./log/f2f.log");
		file.setAppend(true);
		file.setMaxFileSize("500KB");
		file.setMaxBackupIndex(10);
		log.addAppender(console);
		log.addAppender(file);
		*/
		
		//LogManager.getRootLogger().setLevel(Level.DEBUG);
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		//log.debug("TEST");
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				runF2F();
			}
		});
	}

	private static void runF2F()
	{
		Thread updatePeersThread = 
			new Thread(new Runnable()
			{
				public void run()
				{
					controller = new UIController("GUI");
					try{
						natMessageProcessor = new NatMessageProcessor(new ConnectionManager("../F2F/conf/nat-traversal.properties"));
					} catch (ConnectionManagerException e){
						controller.writeNatLog(e.getMessage() + "\nUnable to get NatMessageProcessor instance ...");
						return;
					}
					try {
						F2FComputing.initiateF2FComputing();
					} catch (F2FComputingException e) {
						controller.error(e.toString());
						return;
					}
					FriendModel friendModel = controller.getFriendModel();
					
					//NAT Traversal stun info request for yourself
					natMessageProcessor.getConnectionManager().refreshLocalStunInfo();
					
					//Init the SocketCommunicationProvider
					natMessageProcessor.getConnectionManager().initiateSocketCommunicationProvider();
					
					while (true)
					{
						try {
							Collection<F2FPeer> peersF2F = F2FComputing.getPeers();
							Collection<F2FPeer> peersGUI = friendModel.getPeers();
							// at first check if someone has to be removed
							for (F2FPeer peer: peersGUI)
							{
								if (!peersF2F.contains(peer)){
									friendModel.remove(peer);
									//Remove SocketPeer form socketCommunication layer
									//natMessageProcessor.getConnectionManager().getSocketCommunicationProvider().removeFriend(peer.getID().toString());
									//Remove also NAT Traversal Stun info
									F2FComputingGUI.controller.getStunInfoTableModel().remove(peer.getID().toString());
								}
							}
							// then check if someone has to be added
							for (F2FPeer peer: peersF2F)
							{
								if (!peersGUI.contains(peer)){
									friendModel.add(peer);
									//NAT Traversal automatic stun info request for new client
									String localId = F2FComputing.getLocalPeer().getID().toString();
									if(!localId.equals(peer.getID().toString())){
										NatMessage nmsg = new NatMessage(localId, peer.getID().toString(),NatMessage.COMMAND_GET_STUN_INFO,null);
										natMessageProcessor.sendNatMessage(nmsg);
									}
								}
							}
							Thread.sleep(1000);
						} catch (Exception e) {
							F2FDebug.println(e.toString());
						}
					}
				}
			});
		updatePeersThread.start();
	}

	static class FilteredStream extends FilterOutputStream {
		public FilteredStream(OutputStream outputStream) {
            super(outputStream);
          }

        public void write(byte b[]) throws IOException {
            writeOut(new String(b));
        }

        public void write(byte b[], int off, int len) throws IOException {
            writeOut(new String(b , off , len));
        }
        
        private void writeOut(String msg) {
            if(controller!=null) {
            	controller.debug(msg);
            }
        }
   	}
}
