package org.systemsbiology.util;

import java.util.{List, ArrayList}

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MultiIteratorSpec extends FlatSpec with ShouldMatchers {
  "MultiIterator" should "iterate over a list of lists" in {
    val lists = new ArrayList[List[java.lang.Integer]]
    for (i <- 0 until 12 by 3) {
      val list = new ArrayList[java.lang.Integer]
      list.add(i)
      list.add(i + 1)
      list.add(i + 2)
      lists.add(list)
    }
    val mi = new MultiIteratable[Number](lists)
    mi.hasNext should be (true)
    mi.next should be (0)
    mi.hasNext should be (true)
    mi.next should be (1)
    mi.hasNext should be (true)
    mi.next should be (2)

    mi.hasNext should be (true)
    mi.next should be (3)
    mi.hasNext should be (true)
    mi.next should be (4)
    mi.hasNext should be (true)
    mi.next should be (5)

    mi.hasNext should be (true)
    mi.next should be (6)
    mi.hasNext should be (true)
    mi.next should be (7)
    mi.hasNext should be (true)
    mi.next should be (8)

    mi.hasNext should be (true)
    mi.next should be (9)
    mi.hasNext should be (true)
    mi.next should be (10)
    mi.hasNext should be (true)
    mi.next should be (11)

    mi.hasNext should be (false)
  }
}
