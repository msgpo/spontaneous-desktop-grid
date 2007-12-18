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
		C_CAN_USE_TCP = 3,
		C_BLOCKED_UDP = 4,
		C_FULL_CONE = 5,
		C_OPEN_ACCESS = 6,
		C_PORT_RESTR_CONE = 7,
		C_RESTR_CONE = 8,
		C_SYMM_CONE = 9,
		C_SYMM_UDP = 10
	;
	
	private static final String[] headers = new String[] {
		"Peer ID", "Local IP", "Public IP", "Can Use TCP", "Blocked UDP",
		"Full Cone", "Open Access", "Port Restricted Cone",
		"Restricted Cone", "Symmetric Cone", "Symmetric UDP Firewall"
	};
	
	public static final int[] widths = new int[] { 100, 110, 110, 90, 90, 90, 90, 140, 110, 110, 140 };
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		StunInfoTableItem sinf = stunInfoList.get(rowIndex);
		
		switch (columnIndex) {
			case C_PEER:
				return sinf.getId();
			
			case C_LOCAL_IP:
				return sinf.getLocalIp();
			
			case C_PUBLIC_IP:
				return sinf.getPublicIp();
			
			case C_CAN_USE_TCP:
				return sinf.getTcpConnectivity();
				
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
	
	public StunInfoTableItem getByLocalIp(String localIp){
		for (StunInfoTableItem sinfItem : stunInfoList)
			if (sinfItem.getLocalIp().equals(localIp))
				return sinfItem;
		
		return null;
	}
	
	public StunInfoTableItem getByPublicIp(String publicIp){
		for (StunInfoTableItem sinfItem : stunInfoList)
			if (sinfItem.getPublicIp().equals(publicIp))
				return sinfItem;
		
		return null;
	}
	public boolean update(StunInfoTableItem sinft){
		for (StunInfoTableItem sinfItem : stunInfoList){
			if (sinfItem.getId().equals(sinft.getId())){
				stunInfoList.remove(sinfItem);
				stunInfoList.add(sinft);
				fireTableDataChanged();
				return true;
			}
		}
		return false;
	}
}
