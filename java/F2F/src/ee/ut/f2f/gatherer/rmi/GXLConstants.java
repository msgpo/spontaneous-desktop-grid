package ee.ut.f2f.gatherer.rmi;

public enum GXLConstants {
	
	ATTR_TOTAL_PHYSICAL_MEMORY("totalPhysicalMemory"),
	
	ATTR_FREE_PHYSICAL_MEMORY("freePhysicalMemory"),
	
	ATTR_TOTAL_PAGING_FILE("totalPagingFile"),
	
	ATTR_FREE_PAGING_FILE("freePagingFile"),
	
	ATTR_TOTAL_VIRTUAL_MEMORY("totalVirtualMemory"),
	
	ATTR_FREE_VIRTUAL_MEMORY("freeVirtualMemory"),
	
	ATTR_TOTAL_DISK_SPACE("totalDiskSpace"),
		
	ATTR_FREE_DISK_SPACE("freeDiskSpace"),
	
	/**
	 * Name of the <code>GXLAttr</code> attribute that represents the
	 * connections on <code>GXLNode</code> and <code>GXLEdge</code>.
	 */	
	ATTR_NAME_CONNECTION("connections"),
	
	/**
     * Name of the <code>GXLAttr</code> attribute that represents the connection
     * band-width on <code>GXLEdge</code>.
     */
	ATTR_NAME_BANDWIDTH("bandwidth");
	
	private final String name;

	GXLConstants(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

}
