package ee.ut.f2f.visualizer.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

import ee.ut.f2f.visualizer.editor.GraphEditor;
import ee.ut.f2f.visualizer.filter.GraphViewerFilter;
import ee.ut.f2f.visualizer.log.F2FLogger;
import ee.ut.f2f.visualizer.model.FilterTableRow;
import ee.ut.f2f.visualizer.model.PropertyFilter;
import ee.ut.f2f.visualizer.provider.FilterViewCellModifier;
import ee.ut.f2f.visualizer.provider.FilterViewContentProvider;
import ee.ut.f2f.visualizer.provider.FilterViewLabelProvider;

/**
 * Filter view that enables to apply many different kind of filters on the
 * GraphEditor editor to increase the readability of the graph etc.
 * 
 * @author Indrek Priks
 */
public class FilterView extends ViewPart {
	
	/** Logger */
	private static final F2FLogger log = new F2FLogger(FilterView.class);
	
	/** The ID of this view */
	public static final String ID = "ee.ut.f2f.visualizer.view.filterView";
	
	/** The label for filter table column - active */
	public static final String COLUMN_ACTIVE = "Active";
	/** The tool-tip for filter table column - active */
	public static final String COLUMN_ACTIVE_TOOLTIP = "(De)activate rule";
	/** The label for filter table column - filter mode */
	public static final String COLUMN_FILTER_MODE = "Filter mode";
	/** The tool-tip for filter table column - filter mode */
	public static final String COLUMN_FILTER_MODE_TOOLTIP = "Show when passes/fails this rule";
	/** The label for filter table column - filter type */
	public static final String COLUMN_FILTER_TYPE = "Filter type";
	/** The tool-tip for filter table column - filter type */
	public static final String COLUMN_FILTER_TYPE_TOOLTIP = "Fade or remove failed element";
	/** The label for filter table column - match mode */
	public static final String COLUMN_MATCH_MODE = "Match";
	/** The tool-tip for filter table column - match mode */
	public static final String COLUMN_MATCH_MODE_TOOLTIP = "Value matching mode";
	/** The label for filter table column - parameter name */
	public static final String COLUMN_NAME = "Parameter name";
	/** The tool-tip for filter table column - parameter name */
	public static final String COLUMN_NAME_TOOLTIP = "Parameter name";
	/** The label for filter table column - value pattern */
	public static final String COLUMN_VALUE = "Value pattern";
	/** The tool-tip for filter table column - value pattern */
	public static final String COLUMN_VALUE_TOOLTIP = "Value pattern";
	
	/** The filter table column index of column - active */
	public static final int COLUMN_ACTIVE_IDX = 0;
	/** The filter table column index of column - filter mode */
	public static final int COLUMN_FILTER_MODE_IDX = 1;
	/** The filter table column index of column - filter type */
	public static final int COLUMN_FILTER_TYPE_IDX = 2;
	/** The filter table column index of column - match mode */
	public static final int COLUMN_MATCH_MODE_IDX = 3;
	/** The filter table column index of column - parameter name */
	public static final int COLUMN_NAME_IDX = 4;
	/** The filter table column index of column - value pattern */
	public static final int COLUMN_VALUE_IDX = 5;
	
	/** Array of the filter table column labels in correct order */
	public static final String[] COLUMNS;
	static {
		COLUMNS = new String[6];
		COLUMNS[COLUMN_ACTIVE_IDX] = COLUMN_ACTIVE;
		COLUMNS[COLUMN_FILTER_MODE_IDX] = COLUMN_FILTER_MODE;
		COLUMNS[COLUMN_FILTER_TYPE_IDX] = COLUMN_FILTER_TYPE;
		COLUMNS[COLUMN_MATCH_MODE_IDX] = COLUMN_MATCH_MODE;
		COLUMNS[COLUMN_NAME_IDX] = COLUMN_NAME;
		COLUMNS[COLUMN_VALUE_IDX] = COLUMN_VALUE;
	}
	
	/** Label for the filter table button - add row */
	private static final String BUTTON_ADD = "Add row";
	/** Label for the filter table button - apply */
	private static final String BUTTON_APPLY = "Apply";
	/** Label for the filter table button - reset */
	private static final String BUTTON_RESET = "Reset";
	
	private static long rowid = 0;
	private TableViewer viewer;
	private List<FilterTableRow> inputRows;
	
	@Override
	public void createPartControl(Composite parent) {
		
		Composite container = createContainer(parent);
		
		viewer = createTableViewer(container);
		configureTable(viewer);
		
		createButtons(container);
		
		initializeInput();
		
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	/**
	 * Creates the container with correct layout for the FilterView view.
	 * 
	 * @param parent
	 *          parent Composite
	 * @return the composite
	 */
	private Composite createContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth = false;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return container;
	}
	
	/**
	 * Creates and configures the TableViewer object and CellEditors.
	 * 
	 * @param parent
	 *          parent Composite
	 * @return the TableViewer
	 */
	private TableViewer createTableViewer(Composite parent) {
		TableViewer viewer = new TableViewer(parent, SWT.FULL_SELECTION);
		
		Table table = viewer.getTable();
		
		CellEditor[] editors = new CellEditor[COLUMNS.length];
		editors[COLUMN_ACTIVE_IDX] = new CheckboxCellEditor(table);
		editors[COLUMN_FILTER_MODE_IDX] = new CheckboxCellEditor(table);
		editors[COLUMN_FILTER_TYPE_IDX] = new CheckboxCellEditor(table);
		editors[COLUMN_MATCH_MODE_IDX] = new ComboBoxCellEditor(table, PropertyFilter.MATCH_MODES, SWT.READ_ONLY);
		editors[COLUMN_NAME_IDX] = new TextCellEditor(table);
		editors[COLUMN_VALUE_IDX] = new TextCellEditor(table);
		
		viewer.setCellEditors(editors);
		
		viewer.setColumnProperties(COLUMNS);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new FilterViewContentProvider());
		viewer.setLabelProvider(new FilterViewLabelProvider());
		viewer.setCellModifier(new FilterViewCellModifier(viewer));
		
		return viewer;
	}
	
	/**
	 * Creates and configures the table (and columns) for the input TableViewer.
	 * 
	 * @param viewer
	 *          input TableViewer
	 * @return Table
	 */
	private Table configureTable(TableViewer viewer) {
		Table table = viewer.getTable();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn col = null;
		
		col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMNS[COLUMN_ACTIVE_IDX]);
		col.setToolTipText(COLUMN_ACTIVE_TOOLTIP);
		col.setWidth(20);
		col.setResizable(false);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMNS[COLUMN_FILTER_MODE_IDX]);
		col.setToolTipText(COLUMN_FILTER_MODE_TOOLTIP);
		col.setWidth(16);
		col.setResizable(false);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMNS[COLUMN_FILTER_TYPE_IDX]);
		col.setToolTipText(COLUMN_FILTER_TYPE_TOOLTIP);
		col.setWidth(16);
		col.setResizable(false);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMNS[COLUMN_MATCH_MODE_IDX]);
		col.setToolTipText(COLUMN_MATCH_MODE_TOOLTIP);
		col.setWidth(60);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMNS[COLUMN_NAME_IDX]);
		col.setToolTipText(COLUMN_NAME_TOOLTIP);
		col.setWidth(100);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMNS[COLUMN_VALUE_IDX]);
		col.setToolTipText(COLUMN_VALUE_TOOLTIP);
		col.setWidth(85);
		
		return table;
	}
	
	/**
	 * Creates the buttons associated with filter table.
	 * 
	 * @param parent
	 *          parent Composite
	 */
	private void createButtons(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		layout.horizontalSpacing = 2;
		layout.verticalSpacing = 2;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button button = null;
		
		button = new Button(container, SWT.PUSH);
		button.setText(BUTTON_ADD);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		// Add a new row when the user clicks button
		button.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent event) {
				addNextDefaultRow();
				viewer.refresh();
			}
		});
		
		button = new Button(container, SWT.PUSH);
		button.setText(BUTTON_APPLY);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		// Apply filters when the user clicks button
		button.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent event) {
				applyFilters();
				// viewer.refresh();
			}
		});
		
		button = new Button(container, SWT.PUSH);
		button.setText(BUTTON_RESET);
		button.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));
		// Reset filters when the user clicks button
		button.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent event) {
				resetFilters();
				viewer.refresh();
			}
		});
		
	}
	
	/**
	 * Applies the filters defined in the filter table to the GraphEditor.
	 */
	private void applyFilters() {
		log.debug("applyFilters");
		GraphEditor editor = (GraphEditor) getSite().getPage().getActiveEditor();
		if (editor != null) {
			GraphViewerFilter faderFilter = new GraphViewerFilter(GraphEditor.FILTER_TYPE_NAME_FADE);
			GraphViewerFilter removerFilter = new GraphViewerFilter(GraphEditor.FILTER_TYPE_NAME_REMOVE);
			for (FilterTableRow tr : inputRows) {
				if (tr.isActive()) {
					boolean fader = FilterTableRow.FILTER_TYPE_FADE.equals(tr.getFilterType());
					boolean pass = FilterTableRow.FILTER_MODE_PASS.equals(tr.getFilterMode());
					
					GraphViewerFilter filter = fader ? faderFilter : removerFilter;
					if (pass) {
						filter.addPassFilter(tr.getPropertyFilter());
					}
					else {
						filter.addRejectFilter(tr.getPropertyFilter());
					}
				}
			}
			editor.setFilters(faderFilter, removerFilter);
		}
		else {
			log.debug("No active editors to apply filters to!");
		}
	}
	
	/**
	 * Resets the filter table to the initial state.
	 */
	private void resetFilters() {
		initializeInput();
	}
	
	/**
	 * Initializes this views input.
	 */
	private void initializeInput() {
		inputRows = new ArrayList<FilterTableRow>();
		rowid = 0;
		for (int i = 1; i <= 10; i++) {
			addNextDefaultRow();
		}
		viewer.setInput(inputRows);
	}
	
	/**
	 * Adds next default row to the filter table.
	 */
	private synchronized void addNextDefaultRow() {
		addRow(getNextDefaultFilterTableRow());
	}
	
	/**
	 * Creates next default filter table row object. It does not insert it into
	 * the table!
	 * 
	 * @return next filter table row object
	 */
	private synchronized FilterTableRow getNextDefaultFilterTableRow() {
		FilterTableRow tr = new FilterTableRow(++rowid);
		tr.setInActive();
		return tr;
	}
	
	/**
	 * Adds a row into the filter table.
	 * 
	 * @param tr
	 *          table row to be inserted
	 */
	private synchronized void addRow(FilterTableRow tr) {
		inputRows.add(tr);
	}
	
}
