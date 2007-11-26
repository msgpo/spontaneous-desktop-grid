package ee.ut.f2f.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;

import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.util.logging.Logger;

/**
 * Custom input object stream reader in order to make use of the custom
 * classloaders.
 */
public class CustomObjectInputStream extends java.io.ObjectInputStream
{
	private final static Logger logger = Logger.getLogger(CustomObjectInputStream.class);
	
	/**
	 * If set, then the class has to be resolved using the classloader of this job.
	 * TODO Total absent of SYNCHRONIZATION - IT IS A NIGHTMARE CURRENLTY!!!
	 */
	private String jobID = null;
	
    /**
     * Constructor to create this custom object from input stream.
     */
    public CustomObjectInputStream(InputStream theStream) throws IOException, StreamCorruptedException
    {
        super(theStream);
    }
    
    /**
     * Resolves our unknown classes sent in jars. 
     * 
     * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
     */
    protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException
    {
    	if(logger.isTraceEnabled()) {
    		logger.trace("Resolving custom class: " + osc.getName());
    	}
    	
        Class theClass = null;
        try
        {
            theClass = Class.forName(osc.getName(), true,
            		getClassLoader(jobID)
         		);
        }
        catch (Exception e)
        {
        	logger.warn("Could not reslove the class: "+osc.getName());
        }
        return theClass;
    }
    
    /**
     * @param jobID the ID passed from the {@link F2FMessage} read object overrides.
     * @return object loaded with the correct classloader. 
     */
    public synchronized Object readObject(String jobID) throws IOException, ClassNotFoundException
    {
    	this.jobID = jobID;
    	Object ret = readObject();
    	this.jobID = null;
    	return ret;
    }
    
    /**
     * @return classloader based on the given job ID.
     */
    private static ClassLoader getClassLoader(String jobID)
    {
    	ClassLoader jobClassLoader = F2FComputing.getJobClassLoader(jobID);
		if (jobClassLoader==null)
		{
			return CustomObjectInputStream.class.getClassLoader();
		}
		return jobClassLoader;
	}
	
}
