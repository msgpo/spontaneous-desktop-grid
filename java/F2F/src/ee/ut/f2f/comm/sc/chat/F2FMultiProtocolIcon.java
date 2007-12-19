package ee.ut.f2f.comm.sc.chat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import ee.ut.f2f.util.logging.Logger;

import net.java.sip.communicator.service.protocol.ProtocolIcon;

class F2FMultiProtocolIcon
	implements ProtocolIcon
{
	private static final Logger logger = Logger.getLogger(F2FMultiProtocolIcon.class);
    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private static Hashtable<String, byte[]> iconsTable = new Hashtable<String, byte[]>();
    static
    {
        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,    
            loadIcon("f2f.png"));

        /*iconsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            loadIcon("resources/images/jabber/jabber48x48.png"));*/
    }
    static ImageIcon getImageIcon(String iconSize)
    {
    	if (!iconsTable.containsKey(iconSize))
    		return null;
        try {
			return new ImageIcon(
					ImageIO.read(
			                new ByteArrayInputStream(iconsTable.get(iconSize))
			         		)
			         	);
		}
        catch (IOException e) {}
		return null;
    }
 
    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     * @return an iterator to a set containing the supported icon sizes
     */
    public Iterator<String> getSupportedSizes()
    {
        return iconsTable.keySet().iterator();
    }

    /**
     * Returne TRUE if a icon with the given size is supported, FALSE-otherwise.
     */
    public boolean isSizeSupported(String iconSize)
    {
        return iconsTable.containsKey(iconSize);
    }
    
    /**
     * Returns the icon image in the given size.
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     */
    public byte[] getIcon(String iconSize)
    {
        return iconsTable.get(iconSize);
    }
    
    /**
     * Returns the icon image used to represent the protocol connecting state.
     * @return the icon image used to represent the protocol connecting state
     */
    public byte[] getConnectingIcon()
    {
        return iconsTable.get(ProtocolIcon.ICON_SIZE_16x16);
    }
    
    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    static byte[] loadIcon(String imagePath)
    {
    	InputStream is = F2FMultiProtocolIcon.class.getResourceAsStream(imagePath);
    	if (is == null)
    		is = F2FMultiProtocolIcon.class.getClassLoader()
        		.getResourceAsStream(imagePath);
    	if (is == null)
    		is = F2FMultiProtocolIcon.class.getClassLoader()
        		.getParent().getResourceAsStream(imagePath);
    	
        byte[] icon = null;
        try
        {
            icon = new byte[is.available()];
            is.read(icon);
        }
        catch (IOException e)
        {
            logger.error("Failed to load icon: " + imagePath, e);
        }
        return icon;
    } 
}
