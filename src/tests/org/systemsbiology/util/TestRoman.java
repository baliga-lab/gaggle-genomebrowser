package org.systemsbiology.util;

import org.junit.Test;
import static org.junit.Assert.*;


public class TestRoman {

	@Test
	public void testExhaustively() {
		for (int i=1; i<5000; i++) {
			Roman r = new Roman(i);
			assertTrue(Roman.isRoman(r.toString()));
			Roman r2 = new Roman(r.toString());
			assertEquals(r.toInt(), r2.toInt());
		}
	}

	@Test
	public void testSpotCheck() {
		assertEquals(1, Roman.romanToInt("I"));
		assertEquals(2, Roman.romanToInt("II"));
		assertEquals(3, Roman.romanToInt("III"));
		assertEquals(4, Roman.romanToInt("IV"));
		assertEquals(5, Roman.romanToInt("V"));
		assertEquals(6, Roman.romanToInt("VI"));
		assertEquals(7, Roman.romanToInt("VII"));
		assertEquals(8, Roman.romanToInt("VIII"));
		assertEquals(9, Roman.romanToInt("IX"));
		assertEquals(10, Roman.romanToInt("X"));
		assertEquals(369, Roman.romanToInt("CCCLXIX"));
		assertEquals(449, Roman.romanToInt("CDXLIX"));
		assertEquals(1998, Roman.romanToInt("MCMXCVIII"));
		assertEquals(1999, Roman.romanToInt("MCMXCIX"));
		assertEquals(2001, Roman.romanToInt("MMI"));
		assertEquals(4888, Roman.romanToInt("MMMMDCCCLXXXVIII"));
	}

	@Test
	public void testIsRoman() {
		assertFalse(Roman.isRoman(""));
		assertFalse(Roman.isRoman("Monkeybutt"));
		assertFalse(Roman.isRoman("MMMMM"));
		assertFalse(Roman.isRoman("VXII"));
		assertFalse(Roman.isRoman("VL"));
		assertFalse(Roman.isRoman("LCXVI"));
		assertFalse(Roman.isRoman("XVIC"));
		assertFalse(Roman.isRoman("MCMXICIX"));
		assertFalse(Roman.isRoman("MDDCLI"));
		assertFalse(Roman.isRoman("VIIII"));
		assertFalse(Roman.isRoman("CCCXXXXVIII"));
		assertTrue(Roman.isRoman("I"));
		assertTrue(Roman.isRoman("X"));
		assertTrue(Roman.isRoman("C"));
		assertTrue(Roman.isRoman("CLI"));
		assertTrue(Roman.isRoman("CXL"));
		assertTrue(Roman.isRoman("CXLVIII"));
		assertTrue(Roman.isRoman("MMMMDCCCLXXXVIII"));
	}

	@Test
	public void testCompare() {
		Roman r1 = new Roman(567);
		Roman r2 = new Roman(789);
		Roman r3 = new Roman("DCCLXXXIX");
		assertTrue(r1.compareTo(r2) < 0);
		assertTrue(r2.compareTo(r3) == 0);
		assertTrue(r2.equals(r3));
		assertFalse(r1.equals(r3));
	}
}
