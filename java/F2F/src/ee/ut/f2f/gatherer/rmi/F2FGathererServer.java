package ee.ut.f2f.gatherer.rmi;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

import ee.ut.f2f.gatherer.DataGathering;
import ee.ut.f2f.util.logging.Logger;

import net.sourceforge.gxl.GXLDocument;

/**
 * F2F Gatherer server implementation
 * @author Raido Türk
 *
 */
public class F2FGathererServer implements IF2FGatherer {

	private static final Logger logger = Logger.getLogger(F2FGathererServer.class);

	public F2FGathererServer() throws RemoteException{
		super();
	}

	public byte[] getWholeF2FTopologyGXL() throws Exception{
		GXLDocument doc = DataGathering.getInstance().gatherAllData();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		doc.write(oos);
		return out.toByteArray();
	}

	public static void main(String[] args) throws Exception{
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
		try {
			IF2FGatherer server = new F2FGathererServer();
			IF2FGatherer stub =
                (IF2FGatherer) UnicastRemoteObject.exportObject(server, 0);
			LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind(IF2FGatherer.RMI_BIND_NAME, stub);
			System.out.println("Server started...");
		}catch (ExportException e){
			logger.error("Binding object to registry failed, probably another process is using the same port: ",e);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
