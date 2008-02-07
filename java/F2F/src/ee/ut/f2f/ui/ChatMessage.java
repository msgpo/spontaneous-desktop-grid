package ee.ut.f2f.ui;

import java.io.Serializable;

public class ChatMessage implements Serializable
{
	private static final long serialVersionUID = 1539167807512578175L;
	public String msg = null;
	public ChatMessage(String message)
	{
		msg = message;
	}
}
