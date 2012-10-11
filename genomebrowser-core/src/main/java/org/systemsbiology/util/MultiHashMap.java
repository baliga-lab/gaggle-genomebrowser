package org.systemsbiology.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * maps from a key to a list of values
 */
public class MultiHashMap<T, V> extends HashMap<T, List<V>> {
    private static final long serialVersionUID = -8104431688383800791L;

    public MultiHashMap() {
        super();
    }

    public MultiHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public MultiHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public MultiHashMap(Map<? extends T, ? extends List<V>> m) {
        super(m);
    }

    /**
     * Each key maps to a list of values, so for new
     * keys create a list and insert the first value.
     * For existing keys, just add the new value to the
     * list (if it's not there already).
     */
    public void add(T key, V value) {
        if (!this.containsKey(key)) {
            this.put(key, new ArrayList<V>(1));
        }
        List<V> list = this.get(key);
        if (!list.contains(value)) list.add(value);
    }

    public void addAll(T key, Iterable<? extends V> values) {
        for (V value : values) add(key, value);
    }

    public List<V> getList(T key) {
        if (this.containsKey(key)) return this.get(key);
        else return Collections.emptyList();
    }

    public List<V> getAllValues() {
        List<V> list = new ArrayList<V>();
        for (T key: keySet()) list.addAll(get(key));
        return list;
    }
}
