package org.systemsbiology.util;

import static org.junit.Assert.*;
import java.awt.Color;
import org.junit.Test;

public class TestColorUtils {
    @Test
    public void testDecodeColor() {
        Color c = ColorUtils.decodeColor("java.awt.Color[r=51,g=102,b=153]");
        assertEquals(51, c.getRed());
        assertEquals(102, c.getGreen());
        assertEquals(153, c.getBlue());
		
        // make a color with transparency
        c = new Color(0.5f, 0.6f, 0.7f, 0.8f);

        // colorToString prints the 32-bit hex value w/ alpha
        assertEquals("cc8099b3", ColorUtils.colorToString(c));
    }
}
