package org.systemsbiology.genomebrowser.model;

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SegmentParseSpec extends FlatSpec with ShouldMatchers {

  "Segment.parse()" should "parse sequence ids and ranges with hyphen separator" in {
    val s1 = Segment.parse("pNRC100", "100-200");
    s1.seqId should be ("pNRC100")
    s1.start should be (100)
    s1.end should be (200)

    val s2 = Segment.parse("pNRC100", "10000   -     20000");
    s2.start should be (10000)
    s2.end should be (20000)
  }

  it should "parse a range with a blank separator" in {
    val s = Segment.parse("pNRC100", "10000 20000");
    s.start should be (10000)
    s.end should be (20000)
  }

  it should "parse a range with a comma separator" in {
    val s1 = Segment.parse("pNRC100", "10000, 20000");
    s1.start should be (10000)
    s1.end should be (20000)

    val s2 = Segment.parse("pNRC100", "10000,20000");
    s2.start should be (10000)
    s2.end should be (20000)
  }

  it should "parse a range with a comma separator and reorder start and end" in {
    val s = Segment.parse("pNRC100", "20000, 10000");
    s.start should be (10000)
    s.end should be (20000)
  }
}
