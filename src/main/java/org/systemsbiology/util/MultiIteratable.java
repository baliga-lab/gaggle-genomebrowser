package org.systemsbiology.util;

import java.util.Iterator;

/**
 * 
 * @author cbare
 */
public class MultiIteratable<E> implements Iteratable<E> {
	Iterable<? extends Iterable<? extends E>> iterables;
	Iterator<? extends Iterable<? extends E>> ii;
	Iterator<? extends E> iterator;
	
	public MultiIteratable(Iterable<? extends Iterable<? extends E>> iterables) {
		this.iterables = iterables;
		this.ii = iterables.iterator();
	}

	public boolean hasNext() {
		while (iterator==null || !iterator.hasNext()) {
			if (!ii.hasNext()) return false;
			iterator = ii.next().iterator();
		}
		return (iterator!=null && iterator.hasNext());
	}

	public E next() {
		return iterator.next();
	}

	public void remove() {
		throw new UnsupportedOperationException("remove() not supported.");
	}

	public Iterator<E> iterator() {
		return this;
	}
}
