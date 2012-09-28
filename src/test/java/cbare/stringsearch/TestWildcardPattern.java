package cbare.stringsearch;

import cbare.stringsearch.Pattern;
import cbare.stringsearch.WildcardPattern;
import junit.framework.TestCase;

public class TestWildcardPattern extends TestCase {

	public TestWildcardPattern(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testSimple() {
		Pattern p = new WildcardPattern("abc");
		assertFalse(p.match("xyz"));
		assertFalse(p.match(""));
		assertFalse(p.match("a"));
		assertFalse(p.match("ab"));
		assertFalse(p.match("abcd"));
		assertFalse(p.match("axx"));
		assertFalse(p.match("aabc"));
		assertFalse(p.match("abcc"));
		assertTrue(p.match("abc"));
	}

	public void testEndWithWildcard() {
		Pattern p = new WildcardPattern("abc*");
		assertFalse(p.match("xyz"));
		assertFalse(p.match(""));
		assertFalse(p.match("a"));
		assertFalse(p.match("ab"));
		assertTrue(p.match("abcd"));
		assertFalse(p.match("axx"));
		assertFalse(p.match("aabc"));
		assertTrue(p.match("abcc"));
		assertTrue(p.match("abc"));
	}

	public void testWildCard() {
		Pattern p = new WildcardPattern("ab*cd");
		assertFalse(p.match("xyz"));
		assertFalse(p.match(""));
		assertTrue(p.match("abcd"));
		assertFalse(p.match("abcc"));
		assertFalse(p.match("abc"));
		assertFalse(p.match("abcddd"));
		assertTrue(p.match("abcacacacd"));
		assertTrue(p.match("abxzxzzxxcd"));
	}

	public void test2WildCard() {
		Pattern p = new WildcardPattern("ab*cd*ef");
		assertFalse(p.match("xyz"));
		assertFalse(p.match(""));
		assertTrue(p.match("abcdef"));
		assertFalse(p.match("abcc"));
		assertFalse(p.match("abc"));
		assertFalse(p.match("abcdddf"));
		assertFalse(p.match("abcddde"));
		assertTrue(p.match("abcacacacdef"));
		assertTrue(p.match("abcacacacdxaxefxxef"));
		assertTrue(p.match("abcdcdcdcdcdefefefefef"));
		assertFalse(p.match("abcdcdcdcdcdefefefefex"));
	}

	public void test3WildCard() {
		Pattern p = new WildcardPattern("ab*cd*ef*");
		assertFalse(p.match("xyz"));
		assertFalse(p.match(""));
		assertTrue(p.match("abcdef"));
		assertTrue(p.match("abcdefzzzzz"));
		assertFalse(p.match("abcc"));
		assertFalse(p.match("abc"));
		assertFalse(p.match("abcdddf"));
		assertFalse(p.match("abcddde"));
		assertTrue(p.match("abcacacacdef"));
		assertTrue(p.match("abcacacacdxaxefxxef"));
		assertTrue(p.match("abcdcdcdcdcdefefefefef"));
		assertTrue(p.match("abcdcdcdcdcdefefefefex"));
	}

	public void testEscape() {
		Pattern p = new WildcardPattern("\\*abc");
		assertFalse(p.match("abc\\\\"));
		assertFalse(p.match(""));
		assertFalse(p.match("\\*abc"));
		assertFalse(p.match("abc"));
		assertTrue(p.match("*abc"));
	}

	public void testEscape2() {
		// pattern = \*\\\ which should match "*\"
		Pattern p = new WildcardPattern("\\*\\\\\\");
		assertFalse(p.match("a\\\\"));
		assertFalse(p.match(""));
		assertTrue(p.match("*\\"));
	}

	public void testEscape3() {
		Pattern p = new WildcardPattern("\\*\\\\*abc");
		assertFalse(p.match("a\\\\"));
		assertFalse(p.match(""));
		assertTrue(p.match("*\\qwertyabc"));
	}

	public void testWacky() {
		Pattern p = new WildcardPattern("ab*cd*ef*");
		assertFalse(p.match(null));
		assertFalse(p.match("1234"));
		assertFalse(p.match("           "));
	}

	public void testBacktracking() {
		Pattern p = new WildcardPattern("abcdefg*1234567");
		assertFalse(p.match(null));
		assertFalse(p.match(""));
		assertTrue(p.match("abcdefg1234567"));
		assertTrue(p.match("abcdefg11234567"));
		assertTrue(p.match("abcdefg121234567"));
		assertTrue(p.match("abcdefg1231234567"));
		assertTrue(p.match("abcdefg12341234567"));
		assertTrue(p.match("abcdefg123451234567"));
		assertTrue(p.match("abcdefg1234561234567"));
		assertTrue(p.match("abcdefg123456712345671234567"));
		assertTrue(p.match("abcdefgq1234567"));
		assertTrue(p.match("abcdefgqq1234567"));
		assertTrue(p.match("abcdefgqqqqqq1234567"));
	}
}
