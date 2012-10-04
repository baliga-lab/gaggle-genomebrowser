package org.systemsbiology.util;

import static org.junit.Assert.*;
import org.junit.Test;

public class AttributesTest {

    @Test
    public void testBooleanAttribute() throws Exception {
        Attributes a = new Attributes();
        a.put("foo", true);
        a.put("bar", false);
        assertTrue(a.getBoolean("foo"));
        assertFalse(a.getBoolean("bar"));
        assertTrue(a.getBoolean("foo", false));
        assertFalse(a.getBoolean("bar", true));
        assertTrue(a.getBoolean("this key doesn't exist", true));
        assertFalse(a.getBoolean("this key doesn't exist", false));
    }
}
