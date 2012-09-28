package org.systemsbiology.genomebrowser.util;

import java.util.Comparator;

import org.systemsbiology.genomebrowser.model.Feature;

public class FeatureComparator implements Comparator<Feature> {
	public int compare(Feature f1, Feature f2) {
		if (f1==null)
			if (f2==null)
				return 0;
			else
				return 1;
		if (f2==null)
			return -1;
		if (f1==f2)
			return 0;
		int result = f1.getSeqId().compareTo(f2.getSeqId());
		if (result!=0)
			return result;
		result = f1.getStrand().compareTo(f2.getStrand());
		if (result!=0)
			return result;
		result = f1.getStart() - f2.getStart();
		if (result!=0)
			return result;
		return f1.getEnd() - f2.getEnd();
	}
}