package ee.ut.f2f.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ee.ut.f2f.util.nat.traversal.StunInfo;

public class StunInfoTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 5032243596390541739L;

	private List<StunInfoTableItem> stunInfoList = new ArrayList<StunInfoTableItem>();
	
	public final int
		C_PEER = 0,
		C_LOCAL_IP = 1,
		C_PUBLIC_IP = 2,
		C_BLOCKED_UDP = 3,
		C_FULL_CONE = 4,
		C_OPEN_ACCESS = 5,
		C_PORT_RESTR_CONE = 6,
		C_RESTR_CONE = 7,
		C_SYMM_CONE = 8,
		C_SYMM_UDP = 9
	;
	
	private static final String[] headers = new String[] {
		"Peer", "Local IP", "Public IP", "Blocked UDP", "Full Cone",
		"Open Access", "Port Restricted Cone", "Restricted Cone",
		"Symmetric Cone", "Symmetric UDP Firewall"
	};
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		StunInfoTableItem sinf = stunInfoList.get(rowIndex);
		
		switch (columnIndex) {
			case C_PEER:
				return sinf.getId();
			
			case C_LOCAL_IP:
				return sinf.getLocalIp();
			
			case C_PUBLIC_IP:
				return sinf.getPublicIP();
			
			case C_BLOCKED_UDP:
				return boolStr(sinf.isBlockedUDP());
			
			case C_FULL_CONE:
				return boolStr(sinf.isFullCone());
				
			case C_OPEN_ACCESS:
				return boolStr(sinf.isOpenAccess());
				
			case C_PORT_RESTR_CONE:
				return boolStr(sinf.isPortRestrictedCone());
				
			case C_RESTR_CONE:
				return boolStr(sinf.isRestrictedCone());
				
			case C_SYMM_CONE:
				return boolStr(sinf.isSymmetricCone());
				
			case C_SYMM_UDP:
				return boolStr(sinf.isSymmetricUDPFirewall());
			
			default:
				return null;
		}
	}
	
	private static String boolStr(boolean b) {
		return b ? "Yes" : "No";
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
		StunInfoTableItem sinft = new StunInfoTableItem(sinf);
		stunInfoList.add(sinft);
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
