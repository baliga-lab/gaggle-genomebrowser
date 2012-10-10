package org.systemsbiology.genomebrowser.util

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PairSpec extends FlatSpec with ShouldMatchers {
  "Pair" should "be initialized" in {
    val pair = new Pair[Int, String](1, "hello")
    pair.first should be (1)
    pair.second should be ("hello")
    pair.getFirst should be (1)
    pair.getSecond should be ("hello")
  }
  it should "compute the same hash code for equivalent values" in {
    val pair1 = new Pair[Int, String](1, "hello")
    val pair2 = new Pair[Int, String](1, "hello")
    pair1.hashCode should be (pair2.hashCode)
  }
  it should "implement toString()" in {
    val pair = new Pair[Int, String](1, "hello")
    pair.toString should be ("(1, hello)")
  }
  it should "implement equals()" in {
    val pair1 = new Pair[String, String]("1", "hello")
    val pair2 = new Pair[String, String]("1", "hello")
    val pair3 = new Pair[String, String](null, null)
    val pair4 = new Pair[String, String]("2", "hello")
    val pair5 = new Pair[String, String]("1", "hallo")

    pair1 == pair1 should be (true)
    pair1 == pair2 should be (true)
    pair3 == pair3 should be (true)

    pair1 == null should be (false)
    pair1 == pair3 should be (false)
    pair3 == pair1 should be (false)
    pair1 == pair4 should be (false)
    pair1 == pair5 should be (false)
  }
}
