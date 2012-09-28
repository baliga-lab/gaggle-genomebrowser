package org.systemsbiology.util;

import java.util.Iterator;

// just a thought, not used
public interface BetterIterator<T> extends Iterator<T>, Iterable<T> {

	public void cleanup();
}
