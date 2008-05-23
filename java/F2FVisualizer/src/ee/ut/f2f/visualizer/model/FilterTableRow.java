package ee.ut.f2f.visualizer.model;

/**
 * A property filters table row.
 * 
 * Enables to set the filter active/inactive and choose between filter types and
 * modes.
 * 
 * @author Indrek Priks
 */
public class FilterTableRow {
	
	/**
	 * Defines such a filter which passes through only those elements which match
	 * this property filter
	 */
	public static final Boolean FILTER_MODE_PASS = Boolean.FALSE;
	/**
	 * Defines such a filter which rejects elements that match this property
	 * filter
	 */
	public static final Boolean FILTER_MODE_REJECT = Boolean.TRUE;
	/**
	 * Defines such a property filter that fades out the elements that can't pass
	 * the filter
	 */
	public static final Boolean FILTER_TYPE_FADE = Boolean.FALSE;
	/**
	 * Defines such a property filter that removes the elements that can't pass
	 * the filter
	 */
	public static final Boolean FILTER_TYPE_REMOVE = Boolean.TRUE;
	
	private long rowid;
	private Boolean active = Boolean.FALSE;
	private Boolean filterMode = FILTER_MODE_PASS;
	private Boolean filterType = FILTER_TYPE_FADE;
	private PropertyFilter propertyFilter = new PropertyFilter();
	
	/**
	 * Constructor that defines new empty row.
	 * 
	 * @param rowid
	 *          The unique rowid of the row in the table
	 */
	public FilterTableRow(long rowid) {
		setRowid(rowid);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FilterTableRow) {
			FilterTableRow r = (FilterTableRow) o;
			return r.rowid == rowid;
		}
		return false;
	}
	
	/**
	 * Returns the property filter.
	 * 
	 * @return the property filter
	 */
	public PropertyFilter getPropertyFilter() {
		return propertyFilter;
	}
	
	/**
	 * Sets the property filter.
	 * 
	 * @param propertyFilter
	 *          property filter
	 */
	public void setPropertyFilter(PropertyFilter propertyFilter) {
		this.propertyFilter = propertyFilter;
	}
	
	/**
	 * Returns the rowid of this table row.
	 * 
	 * @return the rowid
	 */
	public long getRowid() {
		return rowid;
	}
	
	/**
	 * Sets the rowid value for this table row.
	 * 
	 * It is a private method because a table row can not exists without a rowid.
	 * Rowid has to be already assigned in the constructor.
	 * 
	 * @param rowid
	 *          the rowid
	 */
	private void setRowid(long rowid) {
		this.rowid = rowid;
	}
	
	/**
	 * Returns if this property filter is active.
	 * 
	 * @return true if this property filter is active, false otherwise
	 */
	public Boolean isActive() {
		return active;
	}
	
	/**
	 * Sets the property filter active or inactive, as is the input parameter.
	 * 
	 * @param active
	 *          this property filters new active status
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}
	
	/**
	 * Sets the property filter active
	 */
	public void setActive() {
		this.active = Boolean.TRUE;
	}
	
	/**
	 * Sets this property filter inactive.
	 */
	public void setInActive() {
		this.active = Boolean.FALSE;
	}
	
	/**
	 * Gets the property filter mode.
	 * 
	 * See the FILTER_MODE constants in this class.
	 * 
	 * @return this property filter mode
	 */
	public Boolean getFilterMode() {
		return filterMode;
	}
	
	/**
	 * Sets the property filter mode.
	 * 
	 * See the FILTER_MODE constants in this class.
	 * 
	 * @param filterMode
	 *          this property filter mode
	 */
	public void setFilterMode(Boolean filterMode) {
		this.filterMode = filterMode;
	}
	
	/**
	 * Gets the property filter type.
	 * 
	 * See FILTER_TYPE constants in this class.
	 * 
	 * @return this property filter type
	 */
	public Boolean getFilterType() {
		return filterType;
	}
	
	/**
	 * Sets the property filter type.
	 * 
	 * See FILTER_TYPE constants in this class.
	 * 
	 * @param filterType
	 *          property filter type
	 */
	public void setFilterType(Boolean filterType) {
		this.filterType = filterType;
	}
	
}
