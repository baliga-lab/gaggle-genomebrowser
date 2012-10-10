package cbare.stringsearch;

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WildcardPatternSpec extends FlatSpec with ShouldMatchers {

  "WildcardPattern" should "do simple matches" in {
		val p = new WildcardPattern("abc")
		p matches "xyz" should be (false)
		p matches "" should be (false)
    p matches "a" should be (false)
		p matches "ab" should be (false)
		p matches "abcd" should be (false)
    p matches "axx" should be (false)
		p matches "aabc" should be (false)
		p matches "abcc" should be (false)
		p matches "abc" should be (true)
  }

  it should "match a pattern ending with a wildcard" in {
		val p = new WildcardPattern("abc*")
		p matches "xyz" should be (false)
		p matches "" should be (false)
		p matches "a" should be (false)
		p matches "ab" should be (false)
		p matches "abcd" should be (true)
		p matches "axx" should be (false)
		p matches "aabc" should be (false)
		p matches "abcc" should be (true)
		p matches "abc" should be (true)    
  }

  it should "match a pattern with 1 wildcard in the middle" in {
		val p = new WildcardPattern("ab*cd")
		p matches "xyz" should be (false)
		p matches "" should be (false)
		p matches "abcd" should be (true)
		p matches "abcc" should be (false)
		p matches "abc" should be (false)
		p matches "abcddd" should be (false)
		p matches "abcacacacd" should be (true)
		p matches "abxzxzzxxcd" should be (true)
  }
  it should "match a pattern with 2 wildcards" in {
		val p = new WildcardPattern("ab*cd*ef")
		p matches "xyz" should be (false)
		p matches "" should be (false)
		p matches "abcdef" should be (true)
		p matches "abcc" should be (false)
		p matches "abc" should be (false)
		p matches "abcdddf" should be (false)
		p matches "abcddde" should be (false)
		p matches "abcacacacdef" should be (true)
		p matches "abcacacacdxaxefxxef" should be (true)
		p matches "abcdcdcdcdcdefefefefef" should be (true)
		p matches "abcdcdcdcdcdefefefefex" should be (false)
  }
  it should "match a pattern with 3 wildcards" in {
		val p = new WildcardPattern("ab*cd*ef*")
		p matches "xyz" should be (false)
		p matches "" should be (false)
		p matches "abcdef" should be (true)
		p matches "abcdefzzzzz" should be (true)
		p matches "abcc" should be (false)
		p matches "abc" should be (false)
		p matches "abcdddf" should be (false)
		p matches "abcddde" should be (false)
		p matches "abcacacacdef" should be (true)
		p matches "abcacacacdxaxefxxef" should be (true)
		p matches "abcdcdcdcdcdefefefefef" should be (true)
		p matches "abcdcdcdcdcdefefefefex" should be (true)
  }
  it should "match a pattern with 1 escape" in {
		val p = new WildcardPattern("\\*abc")
		p matches "abc\\\\" should be (false)
		p matches "" should be (false)
		p matches "\\*abc" should be (false)
		p matches "abc" should be (false)
		p matches "*abc" should be (true)
  }
  it should "match a pattern with 2 escapes" in {
		// pattern = \*\\\ which should match "*\"
		val p = new WildcardPattern("\\*\\\\\\")
		p matches "a\\\\" should be (false)
		p matches "" should be (false)
		p matches "*\\" should be (true)
  }
  it should "match a pattern with 3 escapes" in {
		val p = new WildcardPattern("\\*\\\\*abc")
		p matches "a\\\\" should be (false)
		p matches "" should be (false)
		p matches "*\\qwertyabc" should be (true)
  }
  it should "reject some wacky things" in {
		val p = new WildcardPattern("ab*cd*ef*");
		p matches null should be (false)
		p matches "1234" should be (false)
		p matches "           " should be (false)
  }
  it should "demonstrate backtracking" in {
		val p = new WildcardPattern("abcdefg*1234567");
		p matches null should be (false)
		p matches "" should be (false)
		p matches "abcdefg1234567" should be (true)
		p matches "abcdefg11234567" should be (true)
		p matches "abcdefg121234567" should be (true)
		p matches "abcdefg1231234567" should be (true)
		p matches "abcdefg12341234567" should be (true)
		p matches "abcdefg123451234567" should be (true)
		p matches "abcdefg1234561234567" should be (true)
		p matches "abcdefg123456712345671234567" should be (true)
		p matches "abcdefgq1234567" should be (true)
		p matches "abcdefgqq1234567" should be (true)
		p matches "abcdefgqqqqqq1234567" should be (true)
  }
}
