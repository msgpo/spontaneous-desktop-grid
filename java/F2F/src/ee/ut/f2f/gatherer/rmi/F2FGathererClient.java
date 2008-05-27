package ee.ut.f2f.gatherer.rmi;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import net.sourceforge.gxl.GXLDocument;

public class F2FGathererClient {

	public static void main(String args[]) {
		//ei tea kui vajalik see osa on
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            String name = IF2FGatherer.RMI_BIND_NAME;
            Registry registry = LocateRegistry.getRegistry();
            IF2FGatherer server = (IF2FGatherer) registry.lookup(name);
            byte[] data = server.getWholeF2FTopologyGXL();
            System.out.println("received data size: "+data.length);
            
            InputStream in = new ByteArrayInputStream(data);
    	    ObjectInputStream ois = new ObjectInputStream(in);
            GXLDocument doc = new GXLDocument(ois);
        } catch (Exception e) {
            System.err.println("Test exception: ");
            e.printStackTrace();
        }
    }
}
