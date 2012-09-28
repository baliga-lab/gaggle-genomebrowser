package org.systemsbiology.util;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import org.systemsbiology.util.StringUtils;


public class TestStringUtils {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void test() {
		String a = StringUtils.join("/", "/this/is/a/", "/long/", "file", "/path", "for/", "a", "test");
		assertEquals("/this/is/a/long/file/path/for/a/test", a);
		
		String b = StringUtils.join("/", "////this/is/////", "////another//", "////test/");
		assertEquals("////this/is/another/test/", b);
		
		String c = StringUtils.join("-=-", "abc", "xyz");
		assertEquals("abc-=-xyz", c);

		String d = StringUtils.join(", ", "abcdefg");
		assertEquals("abcdefg", d);

		String e = StringUtils.join(", ");
		assertEquals("", e);

		String f = StringUtils.join(", ", "abc", "def");
		assertEquals("abc, def", f);
	}

	@Test
	public void testIn() {
		String g = "this, that, and the other thing";
		
		String[] groups = StringUtils.trim(g.split(","));
		log.info("groups = " + Arrays.toString(groups));

		assertTrue(StringUtils.in("this", groups));
		assertTrue(StringUtils.in("that", groups));
		assertTrue(StringUtils.in("and the other thing", groups));
		assertFalse(StringUtils.in("qwer", groups));
	}

	@Test
	public void testIn2() {
		String g = "sasquatch";

		String[] groups = StringUtils.trim(g.split(","));
		log.info("groups = " + Arrays.toString(groups));

		assertTrue(StringUtils.in("sasquatch", groups));
		assertFalse(StringUtils.in("qwer", groups));
	}
}
