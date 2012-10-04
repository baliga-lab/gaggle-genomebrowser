package org.systemsbiology.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;


public class TestMultiIterator {
    @Test
    public void test() {
        List<List<Integer>> lists = new ArrayList<List<Integer>>();
        for (int i=0; i<12; i+=3) {
            List<Integer> list = new ArrayList<Integer>();
            list.add(i);
            list.add(i+1);
            list.add(i+2);
            lists.add(list);
        }
        MultiIteratable<Number> mi = new MultiIteratable<Number>(lists);
        assertTrue(mi.hasNext());
        assertEquals(0, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(1, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(2, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(3, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(4, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(5, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(6, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(7, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(8, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(9, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(10, mi.next());
        assertTrue(mi.hasNext());
        assertEquals(11, mi.next());
        assertFalse(mi.hasNext());
    }
}
