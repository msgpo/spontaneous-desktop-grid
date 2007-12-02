package ee.ut.f2f.util.nat.traversal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import ee.ut.f2f.util.nat.traversal.exceptions.NatMessageException;

import sun.misc.UUDecoder;
import sun.misc.UUEncoder;

public class NatMessage implements Serializable{

	private static final long serialVersionUID = -1844907226725414007L;
	private String from;
	private String to;
	private int type;
	private Object content;
	

	//Type codes
	//COMMANDS
	public static final int COMMAND_GET_STUN_INFO = 601;
	public static final int COMMAND_TRY_CONNECT_TO = 602;
	//REPORTS
	public static final int REPORT_STUN_INFO = 61;
	public static final int REPORT_BROKEN_MESSAGE = 60;
	public static final int REPORT_UNABLE_TO_CONNECT_TO = 62;
	public static final int REPORT_SUCCESS_TO_CONNECT_TO = 63;
	
	/**
	 * @param type
	 * @param contentType
	 * @param content
	 */
	public NatMessage(String from, String to, int type, Object content) {
		this.to = to;
		this.from = from;
		this.type = type;
		this.content = content;
	}
	
	@Deprecated
	public NatMessage (String encoded) throws NatMessageException{
		NatMessage temp = decode(encoded);
		this.to = temp.getTo();
		this.from = temp.getFrom();
		this.content = temp.getContent();
		this.type = temp.getType();
		temp = null;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}
	
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("from=" + getFrom() + ",");
		sbuf.append("to=" + getTo() + ",");
		sbuf.append("type=" + getType() + ",");
		sbuf.append("content=");
		if(getContent() != null) sbuf.append(getContent().toString());
		else sbuf.append("null");
		return "[" + sbuf.toString() + "]";
	}
	
	public String encode() throws NatMessageException {
		return encode(this);
	}
	
	@Deprecated
	static String encode(NatMessage nmsg) throws NatMessageException{
		String encoded = null;
		UUEncoder uenc = new UUEncoder();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try{
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(nmsg);
			byte[] bytes = new byte[0];
			bytes = baos.toByteArray();
			encoded = uenc.encodeBuffer(bytes);
			oos.close();
			baos.close();
		} catch (IOException e){
			throw new NatMessageException("Unable to encode message", e);
		}
		return encoded;
	}
	
	@Deprecated
	static NatMessage decode(String encoded) throws NatMessageException{
		NatMessage nmsg = null;
		UUDecoder udec = new UUDecoder();
		try{
			byte[] bytes = udec.decodeBuffer(encoded);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
			nmsg = (NatMessage) ois.readObject();
			ois.close();
		} catch (IOException e){
			throw new NatMessageException("Unable to decode message", e);
		} catch (ClassNotFoundException e){
			throw new NatMessageException("Unable to decode message", e);
		}
		return nmsg;
	}
	
}
