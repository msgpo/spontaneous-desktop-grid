package ee.ut.f2f.gatherer.util;

/**
 * Enums used elsewhere in application
 * @author Raido Türk
 *
 */
public enum Constants {
	
	/**
	 * Windows dll file name
	 */
	WINDOWS_DLL_NAME("f2fwininf"),
	
	LINUX_SO_NAME("f2flinuxinf"),
	
	FRIEND_DATA_CACHE_VALID_TIME_IN_MINUTES("60"),
	
	IS_DUMMY_NODEINFO_ATTR("isDummyNodeInfo"),
	
	EDGE_WITH_WHOM_ATTR("edgeWithWhom");

	private final String name;

	Constants(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

}
