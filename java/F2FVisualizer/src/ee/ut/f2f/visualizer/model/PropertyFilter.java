package ee.ut.f2f.visualizer.model;

/**
 * A general filter object for some class' some kind of property.
 * 
 * Defines a key and value pair (both String) and a value matching mode. Then
 * this object can be used to check if some objects' some property matches this
 * key's value pattern by the matching mode (see the matching modes defined as
 * constants).
 * 
 * The comparison is made through the toMatchable and match method.
 * 
 * @author Indrek Priks
 */
public class PropertyFilter {
	
	/** The text representation for the <code>MATCH_MODE_EQUALS</code> mode */
	public static final String MATCH_MODE_EQUALS_STR = "Equals";
	/** The text representation for the <code>MATCH_MODE_CONTAINS</code> mode */
	public static final String MATCH_MODE_CONTAINS_STR = "Contains";
	/** The text representation for the <code>MATCH_MODE_STARTS_WITH</code> mode */
	public static final String MATCH_MODE_STARTS_WITH_STR = "Starts with";
	/** The text representation for the <code>MATCH_MODE_ENDS_WITH</code> mode */
	public static final String MATCH_MODE_ENDS_WITH_STR = "Ends with";
	/**
	 * Value matching mode where the input string value and this value must be
	 * equal (see String.equals)
	 */
	public static final int MATCH_MODE_EQUALS = 0;
	/**
	 * Value matching mode where the input String value must contain this value
	 * (see String.contains)
	 */
	public static final int MATCH_MODE_CONTAINS = 1;
	/**
	 * Value matching mode where the input string value must start with this
	 * value(see String.startsWith)
	 */
	public static final int MATCH_MODE_STARTS_WITH = 2;
	/**
	 * Value matching mode where the input string value must end with this
	 * value(see String.endsWith)
	 */
	public static final int MATCH_MODE_ENDS_WITH = 3;
	/** The default matching mode */
	public static final int MATCH_MODE_DEFAULT = MATCH_MODE_EQUALS;
	
	/** Array of possible matching modes */
	public static final String[] MATCH_MODES;
	static {
		MATCH_MODES = new String[4];
		MATCH_MODES[MATCH_MODE_EQUALS] = MATCH_MODE_EQUALS_STR;
		MATCH_MODES[MATCH_MODE_CONTAINS] = MATCH_MODE_CONTAINS_STR;
		MATCH_MODES[MATCH_MODE_STARTS_WITH] = MATCH_MODE_STARTS_WITH_STR;
		MATCH_MODES[MATCH_MODE_ENDS_WITH] = MATCH_MODE_ENDS_WITH_STR;
	}
	
	private String key;
	private String valuePattern;
	private Integer matchMode = MATCH_MODE_DEFAULT;
	
	/**
	 * Constructor for empty filter (yet undefined filter).
	 */
	public PropertyFilter() {
	}
	
	/**
	 * Constructor for fully defining the filter.
	 * 
	 * @param key
	 *          the key that will be filtered (the key of the key-value pair)
	 * @param valuePattern
	 *          the value pattern that the comparable objects will be matched
	 *          against
	 * @param matchMode
	 *          the value matching mode (see the constants in this class)
	 */
	public PropertyFilter(String key, String valuePattern, int matchMode) {
		setKey(key);
		setValuePattern(valuePattern);
		setMatchMode(matchMode);
	}
	
	/**
	 * Returns a matchable Property object that can be matched against this
	 * FilterKey.
	 * 
	 * @param key
	 *          the key (of the key-value pair)
	 * @param valuePattern
	 *          the value pattern (of the key-value pair)
	 * @return Property object that can be matched against the PropertyFilter
	 */
	public static Property toMatchable(String key, String valuePattern) {
		return new Property(key, valuePattern);
	}
	
	/**
	 * Checks if this FilterKey matches for the input property.
	 * 
	 * See MATCH_MODE constants in this class.
	 * 
	 * @param property
	 *          Some property that will be matched against this FilterKey
	 * @return true if this FilterKey matched this property, false otherwise
	 */
	public boolean match(Property property) {
		if (property != null) {
			String valPattern = getValuePattern();
			String val = property.getValue();
			
			if (val != null && valPattern != null //
					&& property.getKey() != null && property.getKey().equalsIgnoreCase(getKey())//
			) {
				
				switch (getMatchMode()) {
				case MATCH_MODE_EQUALS:
					return val.equalsIgnoreCase(valPattern);
				case MATCH_MODE_STARTS_WITH:
					return val.startsWith(valPattern);
				case MATCH_MODE_CONTAINS:
					return val.contains(valPattern);
				case MATCH_MODE_ENDS_WITH:
					return val.endsWith(valPattern);
				}
				
			}
			
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getMatchMode() + ";" + getKey() + ";" + getValuePattern();
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
	 * Sets the key (of the key-value pair).
	 * 
	 * @param key
	 *          the key
	 */
	public void setKey(String key) {
		this.key = key;
	}
	
	/**
	 * Returns the value pattern ((of the key-value pair).
	 * 
	 * @return the value pattern
	 */
	public String getValuePattern() {
		return valuePattern;
	}
	
	/**
	 * Sets the value pattern (of the key-value pair).
	 * 
	 * @param valuePattern
	 *          value pattern
	 */
	public void setValuePattern(String valuePattern) {
		this.valuePattern = valuePattern;
	}
	
	/**
	 * Returns the text representation of the value pattern matching mode.
	 * 
	 * @return text representation of the value pattern matching mode
	 */
	public String getMatchModeText() {
		return MATCH_MODES[getMatchMode()];
	}
	
	/**
	 * Gets the value pattern matching mode.
	 * 
	 * @return value pattern matching mode
	 */
	public Integer getMatchMode() {
		return matchMode;
	}
	
	/**
	 * Sets the value pattern matching mode.
	 * 
	 * See MATCH_MODE constants in this class for the input.
	 * 
	 * @param matchMode
	 *          match mode
	 */
	public void setMatchMode(Integer matchMode) {
		this.matchMode = matchMode == null ? MATCH_MODE_DEFAULT : matchMode;
	}
	
}
