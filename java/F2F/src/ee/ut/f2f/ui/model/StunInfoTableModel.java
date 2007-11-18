package ee.ut.f2f.ui.model;

import javax.swing.table.AbstractTableModel;

import ee.ut.f2f.util.nat.traversal.StunInfo;

public class StunInfoTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 5032243596390541739L;

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void add(StunInfo sinf){
		// TODO
	}
	
	public void remove(String id){
		// TODO
	}
	
	public StunInfo get(String id){
		// TODO
		return null;
		
	}
}
