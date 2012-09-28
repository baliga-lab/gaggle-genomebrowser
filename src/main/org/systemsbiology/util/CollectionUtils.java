package org.systemsbiology.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class CollectionUtils {
	
	public static <T> List<T> sortedCopy(List<T> list, Comparator<? super T> comparator) {
		List<T> result = new ArrayList<T>(list);
		Collections.sort(result, comparator);
		return result;
	}
}
