package ee.ut.f2f.comm.udp;

import junit.framework.TestCase;

public class UDPConnectionTest extends TestCase
{
	public void testIntToBytes()
	{
		int intVal = 1902029904;
		byte[] byteVal = UDPConnection.intToBytes(intVal);
		assertEquals(UDPConnection.bytesToInt(byteVal), intVal);
		
		intVal = 1151489497;
		byteVal = UDPConnection.intToBytes(intVal);
		assertEquals(UDPConnection.bytesToInt(byteVal), intVal);
		
		intVal = -1076311331;
		byteVal = UDPConnection.intToBytes(intVal);
		assertEquals(UDPConnection.bytesToInt(byteVal), intVal);
	}
}
