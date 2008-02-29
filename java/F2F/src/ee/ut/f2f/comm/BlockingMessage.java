package ee.ut.f2f.comm;

import java.io.Serializable;

public class BlockingMessage implements Serializable
{
	private static final long serialVersionUID = 211304165877716438L;
	public Object data;
	public long ID;
	public BlockingMessage(Object data, long id)
	{
		this.data = data;
		this.ID = id;
	}
}