package org.systemsbiology.util;

import java.util.HashMap;


/**
 * Keep a set of counts indexed by keys.
 * @author cbare
 */
public class HashCounter extends HashMap<Object, Integer> {
	private static final long serialVersionUID = -855802811482282817L;

	public int increment(Object key) {
		Integer count = get(key);
		if (count == null) {
			put(key, 1);
			return 1;
		}
		else {
			count++;
			put(key, count);
			return count;
		}
	}

	public int getCount(Object key) {
		Integer count = get(key);
		return count == null ? 0 : count;
	}

	public int getTotal() {
		int total = 0;
		for (Object key: this.keySet()) {
			total += get(key);
		}
		return total;
	}
}
