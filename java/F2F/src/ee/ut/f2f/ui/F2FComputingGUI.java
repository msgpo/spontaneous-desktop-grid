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
import ee.ut.f2f.util.logging.Logger;

public class F2FComputingGUI {
	
	final private static Logger log = Logger.getLogger(F2FComputingGUI.class);
	
	public static UIController controller;
	
	private static Thread upThread;
	private static boolean threadAsleep = false;
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
	
	public static void forceSynchronization(){
		if(upThread != null && threadAsleep){
			upThread.interrupt();
		}
	}

	private static void runF2F()
	{
		upThread = 
			new Thread(new Runnable()
			{
				public void run()
				{
					controller = new UIController("GUI");
					try {
						F2FComputing.initiateF2FComputing();
					} catch (F2FComputingException e) {
						log.error(e.toString());
						return;
					}
					FriendModel friendModel = controller.getFriendModel();
					
					/*TODO: remove, this is done after the local peer is created automatically
					//NAT Traversal stun info request for yourself
					natMessageProcessor.getConnectionManager().refreshLocalStunInfo();
					*/
										
					while (true)
					{
						try {
							Collection<F2FPeer> peersF2F = F2FComputing.getPeers();
							Collection<F2FPeer> peersGUI = friendModel.getPeers();
							// at first check if someone has to be removed
							for (F2FPeer peer: peersGUI)
							{
								if (!peersF2F.contains(peer))
								{
									/*TODO: remove
									//Remove SocketPeer form socketCommunication layer
									natMessageProcessor.getConnectionManager().getSocketCommunicationProvider().removeFriend(peer.getID().toString());
									//Remove socket communication provider from F2FPeer
									peer.removeCommProvider(natMessageProcessor.getConnectionManager().getSocketCommunicationProvider());
									//Remove also NAT Traversal Stun info
									F2FComputingGUI.controller.getStunInfoTableModel().remove(peer.getID().toString());
									*/
									//remove F2F peer from list
									friendModel.remove(peer);
								}
							}
							// then check if someone has to be added
							for (F2FPeer peer: peersF2F)
							{
								if (!peersGUI.contains(peer)){
									friendModel.add(peer);
									/*TODO: remove, why this is needed?
									if(!peer.getID().equals(F2FComputing.getLocalPeer().getID())){
										//Peer  should also add me to his list
										//Check if peer added me to the list
										NatMessage nmsg = new NatMessage(F2FComputing.getLocalPeer().getID().toString(),
																	 peer.getID().toString(),
																	 NatMessage.COMMAND_IS_F2FPEER_IN_LIST,
																	 null);
										F2FComputingGUI.natMessageProcessor.sendNatMessage(nmsg);
									}
									*/
								}
							}
							threadAsleep = true;
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							threadAsleep = false;
							log.debug("Synchrnization thread : interrupted sleep");
						} catch (Exception e){
							F2FDebug.println(e.toString());
						}
						threadAsleep = false;
					}
				}
			});
		upThread.start();
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
            log.debug(msg);
        }
   	}
}
