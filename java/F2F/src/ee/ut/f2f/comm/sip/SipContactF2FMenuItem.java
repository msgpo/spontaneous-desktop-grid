package ee.ut.f2f.comm.sip;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.gui.ContactAwareComponent;

@SuppressWarnings("serial")
class SipContactF2FMenuItem
	extends JMenuItem
	implements  ContactAwareComponent,
            ActionListener
{
	private MetaContact metaContact;
	
	public SipContactF2FMenuItem()
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
		SipCommunicationProvider.getInstance().makeF2FTest(metaContact);
	}
}