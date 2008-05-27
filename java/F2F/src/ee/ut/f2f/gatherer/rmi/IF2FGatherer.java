package ee.ut.f2f.gatherer.rmi;

import java.rmi.Remote;

public interface IF2FGatherer extends Remote{
	
	public final String RMI_BIND_NAME = "f2f_gatherer_output";
	
	public byte[] getWholeF2FTopologyGXL() throws Exception;

}
