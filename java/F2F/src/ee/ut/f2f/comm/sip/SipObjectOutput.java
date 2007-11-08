//package ee.ut.f2f.comm.sip;
//
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//
//import ee.ut.f2f.util.Util;
//
///**
//	 * Use only writeObject method of this class
//	 */
//class SipObjectOutput
//{
//	private DataOutputStream dos = null;
//	
//	SipObjectOutput(OutputStream os)
//	{
//		dos = new DataOutputStream(os);
//	}
//
//	/**
//	 * Transform the given object into String and send it using
//	 * DataOutputStream.writeUTF()
//	 */
//	public void writeObject(Object obj) throws IOException
//	{
//		dos.writeUTF(Util.encode(Util.serializeObject(obj)));
//		dos.flush();
//	}
//}
