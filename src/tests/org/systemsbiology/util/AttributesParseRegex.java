package org.systemsbiology.util;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Test;


public class AttributesParseRegex {
	private static final Logger log = Logger.getLogger("unit-test");

	public void basic() {
		String a = "key1=value1;key2=value2;";
		String[] pairs = a.split(";");
		for (String pair : pairs) {
			log.info(pair);
		}
	}

	public void leading() {
		String a = ";key1=value1;key2=value2;";
		String[] pairs = a.split(";");
		for (String pair : pairs) {
			if (pair.length() > 0)
				log.info(pair);
		}
	}

	public void escaped() {
		String a = ";key\\=\\;1=value\\=\\;1;key\\;2=value\\;2";
		String[] pairs = a.split("([^\\\\]|^);");
		for (String pair : pairs) {
			if (pair.length() > 0)
				log.info(pair);
		}
	}

	@Test
	public void testParse() {
		Attributes a = Attributes.parse("key1=value1;key2=value2;key3=value3;");
		assertEquals("value1", a.getString("key1"));
		assertEquals("value2", a.getString("key2"));
		assertEquals("value3", a.getString("key3"));
	}

	@Test
	public void testParseEscapedReturnsAndSlashes() {
		Attributes a = Attributes.parse("key1=value1\\nnext line!;key2=value2\\nYo\\\\yo;key3=value 3;");
		assertEquals("value1\nnext line!", a.getString("key1"));
		assertEquals("value2\nYo\\yo", a.getString("key2"));
		assertEquals("value 3", a.getString("key3"));
	}

	@Test
	public void testParseEmptyString() {
		Attributes a = Attributes.parse("");
		assertNull(a.getString("key1"));
	}

	@Test
	public void testParseNull() {
		Attributes a = Attributes.parse(null);
		assertNull(a.getString("key1"));
	}

}
