package org.systemsbiology.genomebrowser.io;

import java.util.List;

public interface FeatureImporter {

	String getName();
	List<Column> getExpectedColumns();

	public interface Column {
		String getName();
		String getDataType();
	}
}
