package ee.ut.f2f.util.nat.traversal;

import java.io.Serializable;

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
	public static final int COMMAND_IS_TCP_TESTER_ALIVE = 603;
	public static final int COMMAND_IS_F2FPEER_IN_LIST = 604;
	
	//REPORTS
	public static final int REPORT_STUN_INFO = 61;
	public static final int REPORT_BROKEN_MESSAGE = 60;
	public static final int REPORT_UNABLE_TO_CONNECT_TO = 62;
	public static final int REPORT_SUCCESS_TO_CONNECT_TO = 63;
	public static final int REPORT_F2FPEER_IS_IN_LIST = 64;
	public static final int REPORT_F2FPEER_IS_NOT_IN_LIST = 65;
	
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
}
