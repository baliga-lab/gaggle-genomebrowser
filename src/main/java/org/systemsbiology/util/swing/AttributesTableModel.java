package org.systemsbiology.util.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.systemsbiology.util.Attributes;



public class AttributesTableModel extends AbstractTableModel {
	Attributes attributes;
	List<String> keys;

	public AttributesTableModel(Attributes attributes) {
		this.attributes = attributes;
		this.keys = new ArrayList<String>(attributes.keySet());
		Collections.sort(keys);
	}

	@Override
	public String getColumnName(int column) {
		if (column==0)
			return "key";
		if (column==1)
			return "value";
		return super.getColumnName(column);
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return keys.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex==0)
			return keys.get(rowIndex);
		if (columnIndex==1)
			return String.valueOf(attributes.get(keys.get(rowIndex)));
		return "--";
	}
}