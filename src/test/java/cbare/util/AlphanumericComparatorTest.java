package cbare.util;

import java.util.Arrays;

import junit.framework.TestCase;

public class AlphanumericComparatorTest extends TestCase {

	public AlphanumericComparatorTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testArraySort() {
		String[] strings = new String[] { "1", "2", "6", "10", "11", "101", "25", "33", "99",
				"moose", "halobacterium", "11doodle", "12doodle", "9doodle", "3doodle", "100doodle",
				"110ac", "110ba", "whatever", "14", "19", "7", "001", "002", "010",
				"01", "001", "0001", "asdf001aaa", "asdf001zzz", "asdf0001xyz", "asdf001xyz",
				 "127.0.0.1", "127.0.0.2",  "127.0.0.10",  "127.0.0.10", "127.0.10.10"};
		AlphanumericComparator comp = new AlphanumericComparator();
		Arrays.sort(strings, comp);
		System.out.println(Arrays.toString(strings));
		assertEquals("1", strings[0]);
	}
	
	public void test() {
		AlphanumericComparator comp = new AlphanumericComparator();
		assertTrue(comp.compare("11doodle","101doodle") < 0);
		assertTrue(comp.compare("101doodle","11doodle") > 0);
		assertTrue(comp.compare("11doodle","1doodle") > 0);
		assertTrue(comp.compare("1doodle","11doodle") < 0);
		assertTrue(comp.compare("1234", "1234") == 0);
		assertTrue(comp.compare("1234asdf", "1234asdf") == 0);
		assertTrue(comp.compare("123abc", "123za") < 0);
		assertTrue(comp.compare("123za", "123abc") > 0);
	}
	
	public void testOffTheEnd() {
		AlphanumericComparator comp = new AlphanumericComparator();
		assertTrue(comp.compare("33","101doodle") < 0);
		assertTrue(comp.compare("500abcd","44") > 0);
		assertTrue(comp.compare("1234","abcd") < 0);
	}

	public void testMore() {
		AlphanumericComparator comp = new AlphanumericComparator();

		assertTrue(comp.compare("1.5.9", "1.5.15") < 0);
		assertTrue(comp.compare("1.5.99", "1.5.15") > 0);
		assertTrue(comp.compare("moose20", "moose190") < 0);
		assertTrue(comp.compare("moose111", "moose1") > 0);
		assertTrue(comp.compare("moose111", "moose101") > 0);
		assertTrue(comp.compare("moose500b", "moose500a") > 0);
		assertTrue(comp.compare("moose500a", "moose500b") < 0);

		assertTrue(comp.compare("abc", "abcd") < 0);
		assertTrue(comp.compare("ab123c", "ab123cd") < 0);
	}
	
	public void testLeadingZeros() {
		AlphanumericComparator comp = new AlphanumericComparator();

		assertTrue(comp.compare("moose500", "moose0499") > 0);
		assertTrue(comp.compare("moose0499", "moose500") < 0);

		assertTrue(comp.compare("000", "001") < 0);
		assertTrue(comp.compare("000", "11") < 0);

		assertTrue(comp.compare("asdf0001", "asdf001") > 0);
		assertTrue(comp.compare("asdf001", "asdf0001") < 0);

		assertTrue(comp.compare("asdf0001xyz", "asdf001xyz") > 0);
		assertTrue(comp.compare("asdf001zyz", "asdf0001zyz") < 0);

		assertTrue(comp.compare("asdf001aaa", "asdf001zzz") < 0);
		assertTrue(comp.compare("asdf001zzz", "asdf001aaa") > 0);
	}

	public void testTooLong() {
		AlphanumericComparator comp = new AlphanumericComparator();

		assertTrue(comp.compare("abc9999999999999992", "abc9999999999999991") > 0);
		assertTrue(comp.compare("abc9999999999999991", "abc9999999999999992") < 0);
		assertTrue(comp.compare("abc9999999999999992", "abc9999999999999992") == 0);
		assertTrue(comp.compare("abc99", "abc8888888888888888") < 0);
		assertTrue(comp.compare("abc99", "abc0000000000000088") > 0);
	}

	public void testMultipleDigitSegments() {
		AlphanumericComparator comp = new AlphanumericComparator();

		assertTrue(comp.compare("acb1969abc04abc28abc", "abc1969abc04abc24abc") > 0);
		assertTrue(comp.compare("acb1969abc04abc28abc", "abc1969abc4abc24abc") > 0);
		assertTrue(comp.compare("abc1969abc04abc28abc", "abc1969abc5abc24abc") < 0);
		assertTrue(comp.compare("abc1969abc04abc28abc", "abc1973abc12abc08abc") < 0);
		assertTrue(comp.compare("acb1969abc04abc28abc", "abc1969abc4abc28abc") > 0);
		
		// not entirely ideal that this sorts this way, but it's an edge case
		assertTrue(comp.compare("acb1969abc04abc28abc", "abc1969abc4abc29abc") > 0);
	}

	public void testPathologic() {
		AlphanumericComparator comp = new AlphanumericComparator();

		assertTrue(comp.compare("", "") == 0);

		assertTrue(comp.compare("", "s") < 0);
		assertTrue(comp.compare("a", "") > 0);
	}
}
