package org.systemsbiology.util;

import java.util.Iterator;


/**
 * Isn't it irritating that an Iterator isn't Iterable? Idiotic,
 * ill conceived, and intolerable!
 * 
 * @author cbare
 */
public interface Iteratable<E> extends java.util.Iterator<E>, Iterable<E> {

    /**
     * Wraps an iterator and makes it iterable.
     */
    public class Wrapper<T> implements Iteratable<T> {
        private Iterator<T> iterator;

        public Wrapper(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() { return iterator.hasNext(); }
        public T next() { return iterator.next(); }
        public void remove() { iterator.remove(); }
        public Iterator<T> iterator() { return iterator; }
    }

    public class Empty<T> implements Iteratable<T> {
        private static final Empty<Object> empty = new Empty<Object>();

        @SuppressWarnings("unchecked")
        public static <A> Iteratable<A> instance() { return (Iteratable<A>)empty; }

        public boolean hasNext() { return false; }
        public T next() { return null; }
        public void remove() {}
        public Iterator<T> iterator() {	return this; }
    }
}
