package net.ulno.jpunch.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;

public abstract class Util
{
	/**
	 * Serializes Object into byte array.
	 * @throws IOException 
	 */
	public static byte[] serializeObject(Object obj) throws IOException 
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		ObjectOutput oo;
		oo = new ObjectOutputStream(os);
		oo.writeObject(obj);
	
		return os.toByteArray();
	}
	
    /**
     * Deserializes Object from byte array.
     * @throws IOException 
     * @throws StreamCorruptedException 
     * @throws ClassNotFoundException 
     */
	public static Object deserializeObject(byte[] serializedObj) throws StreamCorruptedException, IOException, ClassNotFoundException
    {
    	InputStream is = new ByteArrayInputStream(serializedObj);
    	ObjectInput oi;
		oi = new ObjectInputStream(is);
		return  oi.readObject();
	}
	
	public static byte[] zip(byte[] data) throws IOException 
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		GZIPOutputStream gz = new GZIPOutputStream(os);
		gz.write(data);
		gz.finish();
		return os.toByteArray();
	}
	
	public static byte[] unzip(byte[] data) throws IOException 
	{
		InputStream is = new ByteArrayInputStream(data);
		GZIPInputStream gz = new GZIPInputStream(is);
		List<Byte> l = new ArrayList<Byte>();
		while (gz.available() != 0)
			l.add((byte)gz.read());
		// last byte is unnecessary (it is -1)
		byte[] uncmp = new byte[l.size() - 1];
		for (int i = 0; i < l.size() - 1; i++) uncmp[i] = l.get(i);
		return uncmp;
	}
	
    /**
     * Encodes byte array into String.
     */
	public static String encode(byte[] bytes)
    {
    	return new String(Base64.encodeBase64(bytes));
    }

    /**
     * Decodes byte array from String.
     */
	public static byte[] decode(String str) 
    //	throws MessageCreateException
    {
       	return Base64.decodeBase64(str.getBytes());
    }
	
	/**
	 * Returns transfer speed,
	 * computed using start/stop time stamps in milliseconds and
	 * transfered amount of data in bytes  
	 * 
	 * @param start
	 * @param stop
	 * @param size
	 * @return
	 */
	public static double getTransferSpeed(long start, long stop, int size){
		long time = stop - start;
		double secs = time*1.0/1000;
		double kbytes = size*1.0/1024;
		return kbytes/secs;
	}
	
	public static String getTransferStat(long start, long stop, int size){
		long time = stop - start;
		double secs = time*1.0/1000;
		double kbytes = size*1.0/1024;
		double speed = kbytes/secs;
		StringBuffer sb = new StringBuffer();
		sb.append("\nTransfer Statistics:\n");
		sb.append(String.format("\tTransfer completed in [%s] sec\n", secs));
		sb.append(String.format("\tTransfered size is [%s] %s\n",  
				kbytes >= 1024 ? kbytes/1024 : kbytes,
						kbytes >= 1024 ? "mbytes" :"kbytes"));
		sb.append(String.format("\tAverage speed is [%s] kbytes/s\n", speed));
		return sb.toString();
	}
}
