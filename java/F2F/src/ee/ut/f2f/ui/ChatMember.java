package ee.ut.f2f.ui;

class ChatMember
{
	String name = null;
	
	ChatMember(String name)
	{
		this.name = name;
	}
	
	String getDisplayName() { return name; }
	
	public String toString() { return name; }
}
