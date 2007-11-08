//package ee.ut.f2f.comm.sip;
//
//import java.io.DataInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//import ee.ut.f2f.util.Util;
//
//class SipObjectInput
//{
//	private DataInputStream dis = null;
//	
//	public SipObjectInput(InputStream is)
//	{
//		dis = new DataInputStream(is);
//	}
//	
//	public Object readObject()
//		throws ClassNotFoundException, IOException
//	{
//		while (true)
//		{
//			try
//			{
//				String str = dis.readUTF();
//				return Util.deserializeObject(Util.decode(str));
//			}
//			catch (IOException e)
//			{
//				continue;
//			}
//		}
//	}
//}