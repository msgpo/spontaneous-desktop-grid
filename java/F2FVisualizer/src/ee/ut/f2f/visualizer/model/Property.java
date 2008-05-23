package ee.ut.f2f.visualizer.model;

/**
 * Plain java object that represents some simple key-value pair property.
 * 
 * @author Indrek Priks
 */
public class Property {
	
	private String key;
	private String value;
	
	/**
	 * Constructor that defines the property.
	 * 
	 * @param key
	 *          the key (of the key-value pair)
	 * @param value
	 *          the value ((of the key-value pair)
	 */
	public Property(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	/**
	 * Returns the key (of the key-value pair).
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Sets the key (of the key-value pair)
	 * 
	 * @param key
	 *          the key
	 */
	public void setKey(String key) {
		this.key = key;
	}
	
	/**
	 * Gets the value (of the key-value pair)
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Sets the value (of the key-value pair)
	 * 
	 * @param value
	 *          the value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
}
