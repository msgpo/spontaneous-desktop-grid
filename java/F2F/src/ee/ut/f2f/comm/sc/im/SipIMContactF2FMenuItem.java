package ee.ut.f2f.comm.sc.im;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.gui.ContactAwareComponent;

@SuppressWarnings("serial")
class SipIMContactF2FMenuItem
	extends JMenuItem
	implements  ContactAwareComponent,
            ActionListener
{
	private MetaContact metaContact;
	
	public SipIMContactF2FMenuItem()
	{
		super("F2F test");
		this.addActionListener(this);       
	}
	
	public void setCurrentContact(MetaContact metaContact)
	{   
		this.metaContact = metaContact;
	}

	public void setCurrentContactGroup(MetaContactGroup metaGroup) {}

	public void actionPerformed(ActionEvent e)
	{
		SipIMCommunicationProvider.getInstance().makeF2FTest(metaContact);
	}
}