package org.systemsbiology.util

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MathUtilsSpec extends FlatSpec with ShouldMatchers {
  "MathUtils.confine()" should "clip integers to a range" in {
    MathUtils.confine(-1, 3, 7) should be (3)
    MathUtils.confine(8, 3, 7) should be (7)
    MathUtils.confine(5, 3, 7) should be (5)
  }
  "MathUtils.clip()" should "clip double values to a range" in {
    MathUtils.clip(-1.0, 3.2, 7.3) should be (3.2)
    MathUtils.clip(8, 3.2, 7.3) should be (7.3)
    MathUtils.clip(6.1, 3.2, 7.3) should be (6.1)
  }
  "MathUtils.average()" should "average two int values" in {
    MathUtils.average(4, 2) should be (3)
    MathUtils.average(2, 4) should be (3)
    MathUtils.average(6, 1) should be (3)
    MathUtils.average(1, 6) should be (3)
    MathUtils.average(6, 2) should be (4)
    MathUtils.average(2, 6) should be (4)
  }
  it should "average a list of ints" in {
    MathUtils.average(Array(4, 2, 3)) should be (3)
    MathUtils.average(Array(2, 2, 2)) should be (2)
  }
  "MathUtils.max" should "get the max in a list of ints" in {
    MathUtils.max(Array(4, 2, 3)) should be (4)
  }
}
