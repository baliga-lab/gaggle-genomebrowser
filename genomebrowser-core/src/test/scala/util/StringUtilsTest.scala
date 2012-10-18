package org.systemsbiology.util

import java.util.Arrays

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StringUtilsSpec extends FlatSpec with ShouldMatchers {

  "StringUtils.join()" should "join strings" in {
    StringUtils.join("/", "/this/is/a/", "/long/", "file", "/path",
                     "for/", "a", "test") should be ("/this/is/a/long/file/path/for/a/test")
    StringUtils.join("/", "////this/is/////", "////another//",
                     "////test/") should be ("////this/is/another/test/")
    StringUtils.join("-=-", "abc", "xyz") should be ("abc-=-xyz")
    StringUtils.join(", ", "abcdefg") should be ("abcdefg")
    StringUtils.join(", ") should be ("")
    StringUtils.join(", ", "abc", "def") should be ("abc, def")
  }

  "StringUtils.in()" should "find strings in string arrays" in {
    val groups = StringUtils.trim("this, that, and the other thing".split(","))
    StringUtils.in("this", groups) should be (true)
    StringUtils.in("that", groups) should be (true)
    StringUtils.in("and the other thing", groups) should be (true)
    StringUtils.in("qwer", groups) should be (false)
  }
  it should "find a single string in an array" in {
    val groups = StringUtils.trim("sasquatch".split(","))
    StringUtils.in("sasquatch", groups) should be (true)
    StringUtils.in("qwer", groups) should be (false)
  }
}
