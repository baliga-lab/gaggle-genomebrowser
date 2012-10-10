package org.systemsbiology.genomebrowser.util

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import java.awt.Color;

@RunWith(classOf[JUnitRunner])
class ColorUtilsSpec extends FlatSpec with ShouldMatchers {
  "ColorUtils.decodeColor()" should "parse a color" in {
    val c1 = ColorUtils.decodeColor("java.awt.Color[r=51,g=102,b=153]")
    c1.getRed should be (51)
    c1.getGreen should be (102)
    c1.getBlue should be (153)

    // make a color with transparency
    val c2 = new Color(0.5f, 0.6f, 0.7f, 0.8f);

    // colorToString prints the 32-bit hex value w/ alpha
    ColorUtils.colorToString(c2) should be ("cc8099b3")
  }
}
