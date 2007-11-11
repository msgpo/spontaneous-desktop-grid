package ee.ut.f2f.util.nat.traversal;

import java.io.Serializable;
import java.net.InetAddress;

import de.javawi.jstun.test.DiscoveryInfo;

public class StunInfo extends DiscoveryInfo implements Serializable{

	private String id;
	
	private static final long serialVersionUID = 4721470383254301632L;

	public StunInfo(InetAddress localIP) {
		super(localIP);
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
