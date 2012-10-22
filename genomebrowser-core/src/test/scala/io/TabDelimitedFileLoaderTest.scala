package org.systemsbiology.genomebrowser.io

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestTabDelimitedFileLoader extends FlatSpec with ShouldMatchers {

  "TabDelimitedFileLoader" should "read tiling array when initialized with 0 size" in {
    // just taken over from the old tests, column arrays are 0-sized,
    // so this just does not make much sense
	  val tr = new TabDelimitedFileLoader(0)
		tr.loadData("classpath:/example/1/tiling_array.tsv")
  }
  it should "read in a file with 1000 lines" in {
		val tr = new TabDelimitedFileLoader(1000)
		tr.addIntColumn(0)
		tr.addIntColumn(1)
		tr.addDoubleColumn(2)
		tr.loadData("classpath:/example/1/tiling_array.tsv")

		val starts = tr.getIntColumn(0)
		starts.length should be (1000)
    starts(0) should be (1)
    starts(1) should be (101)
    starts(999) should be (99901)

		val ends = tr.getIntColumn(1)
    ends.length should be (1000)
    ends(0) should be (100)
    ends(1) should be (200)
    ends(999) should be (100000)

		val values = tr.getDoubleColumn(2)
    values.length should be (1000)
    values(0) should be (0.0)
    values(1) should be (0.062853290044482 plusOrMinus 0.000001)
    values(998) should be (-0.0628532900444885 plusOrMinus 0.000001)
		tr.getColumnHeader(0) should be ("START")
		tr.getColumnHeader(1) should be ("END")
		tr.getColumnHeader(2) should be ("VALUE")
  }

  it should "throws errors" in {
		val tr = new TabDelimitedFileLoader(1000)
		tr.addIntColumn(0)
		tr.addDoubleColumn(2)
		tr.loadData("classpath:/example/1/tiling_array.tsv")

    // check for type error
		evaluating { tr.getDoubleColumn(0) } should produce [Exception]
    // check for range error
		evaluating { tr.getDoubleColumn(3) } should produce [Exception]
  }

  it should "create a computed position column" in {
		val tr = TabDelimitedFileLoader.createSegmentToPositionDataPointLoader(1000);
		tr.loadData("classpath:/example/1/tiling_array.tsv")

		// tests computed columns which have a name rather than an index
		val positions = tr.getIntColumn("position")
		positions.length should be (1000)
    positions(0) should be (50)
    positions(1) should be (150)
    positions(999) should be (99950)

		val values = tr.getDoubleColumn(2)
    values.length should be (1000)
    values(0) should be (0.0)
    values(1) should be (0.062853290044482 plusOrMinus 0.000001)
    values(998) should be (-0.0628532900444885 plusOrMinus 0.000001)

		tr.getColumnHeader(0) should be ("START")
		tr.getColumnHeader(1) should be ("END")
		tr.getColumnHeader(2) should be ("VALUE")
  }
}
