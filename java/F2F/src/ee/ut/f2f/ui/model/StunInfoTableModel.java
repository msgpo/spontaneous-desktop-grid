package ee.ut.f2f.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ee.ut.f2f.util.nat.traversal.StunInfo;

public class StunInfoTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 5032243596390541739L;

	private List<StunInfo> stunInfoList = new ArrayList<StunInfo>();
	
	public final int
		C_PEER = 0,
		C_ADAPTER = 1,
		C_LOCALIP = 2,
		C_PUBLICIP = 3,
		C_FIREWALL = 4
	;
	
	private static final String[] headers = new String[] {
		"Peer", "Adapter", "Local IP", "Public IP", "Firewall Type"
	};
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		StunInfo sinf = stunInfoList.get(rowIndex);
		
		switch (columnIndex) {
			case C_PEER:
				return "-peer-";
			
			case C_ADAPTER:
				return "-adapter-";
			
			case C_LOCALIP:
				return sinf.getLocalIp();
			
			case C_PUBLICIP:
				return sinf.getPublicIP();
			
			case C_FIREWALL:
				return "-firewall-";
			
			default:
				return null;
		}
	}
	
	public int getColumnCount() {
		return headers.length;
	}

	public String getColumnName(int column) {
		return headers[column];
	}
	
	public int getRowCount() {
		return stunInfoList.size();
	}
	
	public void add(StunInfo sinf){
		stunInfoList.add(sinf);
		fireTableDataChanged();
	}
	
	public void remove(String id){
		StunInfo sinf = get(id);
		
		if (sinf != null) {
			stunInfoList.remove(sinf);
			fireTableDataChanged();
		}
	}
	
	public StunInfo get(String id){
		for (StunInfo sinf : stunInfoList)
			if (sinf.getId().equals(id))
				return sinf;
		
		return null;
	}
}
