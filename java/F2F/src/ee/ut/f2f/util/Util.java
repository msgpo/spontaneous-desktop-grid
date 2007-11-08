package ee.ut.f2f.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import org.apache.commons.codec.binary.Base64;

import ee.ut.f2f.util.CustomObjectInputStream;

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
		oi = new CustomObjectInputStream(is);
		return  oi.readObject();
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

}
