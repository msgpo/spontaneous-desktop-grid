package ee.ut.f2f.ui;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.logging.Logger;

public class F2FComputingGUI {
	
	final private static Logger log = Logger.getLogger(F2FComputingGUI.class);
	
	public static UIController controller;
	
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
		//new Thread(new Runnable()
		//{
			//public void run()
			//{
				controller = new UIController("F2FComputing GUI");
				F2FComputing.initiateF2FComputing();
			//}
		//}).start();
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
