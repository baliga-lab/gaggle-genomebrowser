package org.systemsbiology.util

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SpecRoman extends FlatSpec with ShouldMatchers {

  "Roman" should "be tested exhaustively" in {
    for (i <- 1 until 5000) {
      val r = new Roman(i)
      Roman.isRoman(r.toString) should be (true)
      val r2 = new Roman(r.toString)
      r.toInt == r2.toInt should be (true)
    }
  }
  it should "be tested for a couple of input strings" in {
    Roman.romanToInt("I") should be (1)
    Roman.romanToInt("II") should be (2)
    Roman.romanToInt("III") should be (3)
    Roman.romanToInt("IV") should be (4)
    Roman.romanToInt("V") should be (5)
    Roman.romanToInt("VI") should be (6)
    Roman.romanToInt("VII") should be (7)
    Roman.romanToInt("VIII") should be (8)
    Roman.romanToInt("IX") should be (9)
    Roman.romanToInt("X") should be (10)
    Roman.romanToInt("CCCLXIX") should be (369)
    Roman.romanToInt("CDXLIX") should be (449)
    Roman.romanToInt("MCMXCVIII") should be (1998)
    Roman.romanToInt("MCMXCIX") should be (1999)
    Roman.romanToInt("MMI") should be (2001)
    Roman.romanToInt("MMMMDCCCLXXXVIII") should be (4888)
  }
  it should "verify roman strings" in {
    Roman.isRoman("") should be (false)
    Roman.isRoman("Monkeybutt") should be (false)
    Roman.isRoman("MMMMM") should be (false)
    Roman.isRoman("VXII") should be (false)
    Roman.isRoman("VL") should be (false)
    Roman.isRoman("LCXVI") should be (false)
    Roman.isRoman("XVIC") should be (false)
    Roman.isRoman("MCMXICIX") should be (false)
    Roman.isRoman("MDDCLI") should be (false)
    Roman.isRoman("VIIII") should be (false)
    Roman.isRoman("CCCXXXXVIII") should be (false)

    Roman.isRoman("I") should be (true)
    Roman.isRoman("X") should be (true)
    Roman.isRoman("C") should be (true)
    Roman.isRoman("CLI") should be (true)
    Roman.isRoman("CXL") should be (true)
    Roman.isRoman("CXLVIII") should be (true)
    Roman.isRoman("MMMMDCCCLXXXVIII") should be (true)
  }

  it should "compare Roman objects correctly" in {
    val r1 = new Roman(567)
    val r2 = new Roman(789)
    val r3 = new Roman("DCCLXXXIX")
    r1.compareTo(r2) < 0 should be (true)
    r2.compareTo(r3) == 0 should be (true)
    r2.equals(r3) should be (true)
    r1.equals(r3) should be (false)
  }
}
