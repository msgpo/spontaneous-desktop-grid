package ee.ut.f2f.util.nat.traversal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import ee.ut.f2f.ui.F2FComputingGUI;

public class NatLogger {
	
	private Class<?> clazz = null;
	private String LEVEL = "DEBUG";
	private String prefix = "NATLOG:[" + LEVEL + "] ";
	
	
	public NatLogger(Class<?> clazz){
		this.clazz = clazz;
		if (clazz != null) prefix = prefix + clazz.getSimpleName() + " ";
	}
	
	public void debug(String msg){
		this.LEVEL = "DEBUG";
		log(msg);
	}
	
	public void error(String msg, Throwable e){
		this.LEVEL = "ERROR";
		log(msg + "\n" + e.getMessage() + "\n" + e.getStackTrace());
	}
	
	private void log(String msg){
		String out = (new Date(System.currentTimeMillis())).toString() + " - " + msg;
		File logDir = new File("./log");
		if(!logDir.exists() && !logDir.isDirectory()) logDir.mkdir();
		File logfile = new File("./log/f2fnat.log");
		BufferedWriter bufOut = null;
		try{
			bufOut = new BufferedWriter(new FileWriter(logfile,true));
			bufOut.write(prefix + out + "\n");
		} catch (IOException e) {
			
		} finally {
			try {
				bufOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(prefix + out);
		//F2FComputingGUI.controller.writeNatLog(out);
	}
}
