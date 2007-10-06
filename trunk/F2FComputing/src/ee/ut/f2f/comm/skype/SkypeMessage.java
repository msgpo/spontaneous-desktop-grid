package ee.ut.f2f.comm.skype;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ee.ut.f2f.util.Util;

/**
 * Is used only internaly to encapsulate Skype messages.
 */
class SkypeMessage
{
	int id;
	Object message;

	/**
	 * Creates Message instance from id and message object.
	 *
	 * @param id
	 * @param message
	 */
	SkypeMessage(int id, Object message) {
		this.id = id;
		this.message = message;
	}

	/**
	 * Creates Message instance from serialized String (will deserialize
	 * String into message object and id).
	 *
	 * @param packet
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	SkypeMessage(String packet) throws IOException, ClassNotFoundException {
		readFromPacket(packet);
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.Message#getID()
	 */
	public int getID() {
		return id;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.Message#getMessage()
	 */
	public Object getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.Message#setID(int)
	 */
	public void setID(int ID) {
		this.id = ID;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.comm.Message#setMessage(java.lang.Object)
	 */
	public void setMessage(Object message) {
		this.message = message;
	}

	/**
	 * Initializes id and message object from serialized String (will deserialize
	 * String into message object and id).
	 *
	 * @param packet
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void readFromPacket(String packet) throws IOException, ClassNotFoundException 
	{
		byte[] data = Util.decode(packet);
		ByteArrayInputStream stream = new ByteArrayInputStream (data);
		DataInputStream convert = new DataInputStream (stream);

		id   = convert.readInt();
		byte[] dataObjBytes = new byte[stream.available()];

		convert.readFully(dataObjBytes);
		message = Util.deserializeObject(dataObjBytes);
	}

	/*
	 * @see ee.ut.f2f.comm.Message#createPacket()
	 */
	public String createPacket() throws IOException  {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream convert = new DataOutputStream (stream);

		convert.writeInt(id);
		convert.write(Util.serializeObject(message));
		return Util.encode(stream.toByteArray());

	}

}
