package net.ulno.jpunch.comm;

import java.io.Serializable;

public class BlockingReply implements Serializable
{
	private static final long serialVersionUID = -4597028311351103891L;
	public long ID;
	public BlockingReply(BlockingMessage msg)
	{
		this.ID = msg.ID;
	}
}
