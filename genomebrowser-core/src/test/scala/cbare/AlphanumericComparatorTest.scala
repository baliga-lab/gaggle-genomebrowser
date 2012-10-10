package cbare.util

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import java.util.Arrays

@RunWith(classOf[JUnitRunner])
class AlphanumericComparatorSpec extends FlatSpec with ShouldMatchers {
  "AlphanumericComparator" should "sort a string array" in {
		val strings = Array("1", "2", "6", "10", "11", "101", "25", "33", "99",
				                "moose", "halobacterium", "11doodle", "12doodle",
                        "9doodle", "3doodle", "100doodle",
				                "110ac", "110ba", "whatever", "14", "19", "7", "001", "002", "010",
				                "01", "001", "0001", "asdf001aaa", "asdf001zzz", "asdf0001xyz",
                        "asdf001xyz", "127.0.0.1", "127.0.0.2",  "127.0.0.10",
                        "127.0.0.10", "127.0.10.10")
		val comp = new AlphanumericComparator
		Arrays.sort(strings, comp)
    strings(0) should be ("1")
  }
  it should "do same-length comparisons" in {
		val comp = new AlphanumericComparator
		comp.compare("11doodle","101doodle") should be < 0
		comp.compare("101doodle","11doodle") should be > 0
		comp.compare("11doodle","1doodle") should be > 0
		comp.compare("1doodle","11doodle") should be < 0
		comp.compare("1234", "1234") should be (0)
		comp.compare("1234asdf", "1234asdf") should be (0)
		comp.compare("123abc", "123za") should be < 0
		comp.compare("123za", "123abc") should be > 0
  }
  it should "do non-equal length comparisons" in {
		val comp = new AlphanumericComparator
		comp.compare("33","101doodle") should be < 0
		comp.compare("500abcd","44") should be > 0
		comp.compare("1234","abcd") should be < 0
  }
  it should "handle pathological cases" in {
		val comp = new AlphanumericComparator
		comp.compare("", "") should be (0)
		comp.compare("", "s") should be < 0
		comp.compare("a", "") should be > 0
  }
  it should "handle some more cases" in {
		val comp = new AlphanumericComparator

		comp.compare("1.5.9", "1.5.15") should be < 0
		comp.compare("1.5.99", "1.5.15") should be > 0
		comp.compare("moose20", "moose190") should be < 0
		comp.compare("moose111", "moose1") should be > 0
		comp.compare("moose111", "moose101") should be > 0
		comp.compare("moose500b", "moose500a") should be > 0
		comp.compare("moose500a", "moose500b") should be < 0
		comp.compare("abc", "abcd") should be < 0
		comp.compare("ab123c", "ab123cd") should be < 0
  }
  it should "handle leading zeros" in {
		val comp = new AlphanumericComparator
		comp.compare("moose500", "moose0499") should be > 0
		comp.compare("moose0499", "moose500") should be < 0

		comp.compare("000", "001") should be < 0
		comp.compare("000", "11") should be < 0

		comp.compare("asdf0001", "asdf001") should be > 0
		comp.compare("asdf001", "asdf0001") should be < 0

		comp.compare("asdf0001xyz", "asdf001xyz") should be > 0
		comp.compare("asdf001zyz", "asdf0001zyz") should be < 0

		comp.compare("asdf001aaa", "asdf001zzz") should be < 0
		comp.compare("asdf001zzz", "asdf001aaa") should be > 0
  }
  it should "handle long strings" in {
		val comp = new AlphanumericComparator
		comp.compare("abc9999999999999992", "abc9999999999999991") should be > 0
		comp.compare("abc9999999999999991", "abc9999999999999992") should be < 0
		comp.compare("abc9999999999999992", "abc9999999999999992") should be (0)
		comp.compare("abc99", "abc8888888888888888") should be < 0
		comp.compare("abc99", "abc0000000000000088") should be > 0
  }
  it should "multiple digit segments" in {
		val comp = new AlphanumericComparator
		comp.compare("acb1969abc04abc28abc", "abc1969abc04abc24abc") should be > 0
		comp.compare("acb1969abc04abc28abc", "abc1969abc4abc24abc") should be > 0
		comp.compare("abc1969abc04abc28abc", "abc1969abc5abc24abc") should be < 0
		comp.compare("abc1969abc04abc28abc", "abc1973abc12abc08abc") should be < 0
		comp.compare("acb1969abc04abc28abc", "abc1969abc4abc28abc") should be > 0
		
		// not entirely ideal that this sorts this way, but it's an edge case
		comp.compare("acb1969abc04abc28abc", "abc1969abc4abc29abc") should be > 0
  }
}
