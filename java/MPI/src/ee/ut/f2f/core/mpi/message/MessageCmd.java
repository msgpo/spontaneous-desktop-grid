package ee.ut.f2f.core.mpi.message;

public class MessageCmd {
	static public final int NOCOMMAND		= -1;
	static public final int GOSSIP_REGISTER	= 0;
	static public final int GOSSIP			= 1;
	static public final int GOSSIP_UNREGISTER	= 2;
	static public final int GOSSIP_NOTIFY		= 3;
	static public final int FT_REGISTER		= 4;
	static public final int FT_TRANSFER		= 5;
	static public final int FT_DONE			= 6;
	static public final int MPI_REQPEER		= 7;
	static public final int MPD_REQPEER		= 8;
	static public final int MPD_ACCEPT		= 9;
	static public final int MPD_DENY		= 10;
	static public final int MPI_SYN1		= 11;
	static public final int MPI_SYN2		= 12;
	static public final int GOSSIP_PING		= 13;
}
