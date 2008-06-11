package ee.ut.f2f.gatherer.model;

/**
 * 
 * @author Raido Türk
 *
 */
public class LinuxAttributes {
	
	int memoryLoad;
	long totalPhysicalMemory;
	long freePhysicalMemory;
	long totalSwapFile;
	long freeSwapFile;
	long totalDiskSpace;
	long freeDiskSpace;


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
	 * @return the totalSwapFile
	 */
	public long getTotalSwapFile() {
		return totalSwapFile;
	}

	/**
	 * @return the freeSwapFile
	 */
	public long getFreeSwapFile() {
		return freeSwapFile;
	}

	/**
	 * @return the totalDiskSpace
	 */
	public long getTotalDiskSpace() {
		return totalDiskSpace;
	}

	/**
	 * @return the freeDiskSpace
	 */
	public long getFreeDiskSpace() {
		return freeDiskSpace;
	}

}
