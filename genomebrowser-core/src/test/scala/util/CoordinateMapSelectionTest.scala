package org.systemsbiology.genomebrowser.util

import java.util.{Arrays, Collections}

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CoordinateMapSelectionSpec extends FlatSpec with ShouldMatchers {

  "CoordinateMapSelection" should "ensure a descending natural order" in {
    val selectionA = new CoordinateMapSelection("a", 0.1)
    val selectionB = new CoordinateMapSelection("b", 0.01)
    val list = Arrays.asList(selectionB, selectionA)
    Collections.sort(list)
    list.get(0) should be (selectionA)
  }
}
