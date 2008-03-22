package ee.ut.f2f.comm.sc.im;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.gui.ContactAwareComponent;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.PluginComponent;

@SuppressWarnings("serial")
class SipIMContactF2FMenuItem
	implements  ContactAwareComponent,
            ActionListener, PluginComponent
{
	private MetaContact metaContact;
    private JMenuItem menuItem = null;
	
	public SipIMContactF2FMenuItem()
	{
        menuItem = new JMenuItem("F2F test");
        menuItem.addActionListener(this);       
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

    public Object getComponent()
    {
        return menuItem;
    }
    
    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU;
    }

    public String getName()
    {
        return null;
    }
}