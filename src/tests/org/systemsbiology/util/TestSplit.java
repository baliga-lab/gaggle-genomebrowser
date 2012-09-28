package org.systemsbiology.util;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Split handles empty fields strangely. If we split on the tab character and
 * there are several tabs at the end of the string we're splitting, it seems
 * to trim them off before doing the split. You'll get a truncated array rather
 * than an array ending in empty fields. How annoying.
 * 
 * @author cbare
 */
public class TestSplit {
	private static final Logger log = Logger.getLogger("unit-test");


	@Test
	public void test1() {
		String[] fields = "foo\t\t\t\t\t\t\tbar".split("\t");
		log.info("fields.length = " + fields.length);
		assertEquals(fields.length, 8);
	}

	@Test
	public void test2() {
		// 7 tabs
		String[] fields = "\t\t\t\t\t\t\t".split("\t");
		log.info("fields.length = " + fields.length);
		assertEquals(fields.length, 8);
	}

	@Test
	public void test3() {
		String[] fields = "nospaceshereatall".split(" ");
		log.info("fields = " + Arrays.toString(fields));
	}
}
