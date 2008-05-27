package ee.ut.f2f.gatherer.model;

/**
 * 
 * @author Raido Türk
 *
 */
public class WindowsAttributes {
	
	int memoryLoad;
	long totalPhysicalMemory;
	long freePhysicalMemory;
	long totalPagingFile;
	long freePagingFile;
	long totalVirtualMemory;
	long freeVirtualMemory;
	
	//long totalDiskSpace;
	//long freeDiskSpace;
	
	String windowsVersion;

	/**
	 * @return the memoryLoad
	 */
	public int getMemoryLoad() {
		return memoryLoad;
	}

	/**
	 * @return the totalPhysicalMemory
	 */
	public long getTotalPhysicalMemory() {
		return totalPhysicalMemory;
	}

	/**
	 * @return the freePhysicalMemory
	 */
	public long getFreePhysicalMemory() {
		return freePhysicalMemory;
	}

	/**
	 * @return the totalPagingFile
	 */
	public long getTotalPagingFile() {
		return totalPagingFile;
	}

	/**
	 * @return the freePagingFile
	 */
	public long getFreePagingFile() {
		return freePagingFile;
	}

	/**
	 * @return the totalVirtualMemory
	 */
	public long getTotalVirtualMemory() {
		return totalVirtualMemory;
	}

	/**
	 * @return the freeVirtualMemory
	 */
	public long getFreeVirtualMemory() {
		return freeVirtualMemory;
	}

	/**
	 * @return the totalDiskSpace
	 */
	//public long getTotalDiskSpace() {
		//return totalDiskSpace;
	//}

	/**
	 * @return the freeDiskSpace
	 */
	//public long getFreeDiskSpace() {
		//return freeDiskSpace;
	//}

	/**
	 * @return the windowsVersion
	 */
	public String getWindowsVersion() {
		return windowsVersion;
	}
	
	

}
