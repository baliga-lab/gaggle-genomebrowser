package org.systemsbiology.genomebrowser.util

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AttributesSpec extends FlatSpec with ShouldMatchers {

  "Attributes" should "store boolean values" in {
    val a = new Attributes
    a.put("foo", true)
    a.put("bar", false)
    assert(a.getBoolean("foo"))
    assert(!a.getBoolean("bar"))
    assert(a.getBoolean("foo", false))
    assert(!a.getBoolean("bar", true))
    assert(a.getBoolean("this key doesn't exist", true))
    assert(!a.getBoolean("this key doesn't exist", false))
  }
}

@RunWith(classOf[JUnitRunner])
class ParseAttributesSpec extends FlatSpec with ShouldMatchers {

  "Attributes.parse" should "parse null" in {
    val attrs = Attributes.parse(null)
    attrs.getString("key1") should be (null)
  }
  it should "parse an empty string" in {
    val attrs = Attributes.parse("")
    attrs.getString("key1") should be (null)
  }
  it should "parse a simple input string" in {
    val attrs = Attributes.parse("key1=value1;key2=value2;key3=value3;")
    attrs.getString("key1") should be ("value1")
    attrs.getString("key2") should be ("value2")
    attrs.getString("key3") should be ("value3")
  }
  it should "parse an input string that contains newlines and slashes" in {
    val attrs = Attributes.parse("key1=value1\\nnext line!;key2=value2\\nYo\\\\yo;key3=value 3;")
    attrs.getString("key1") should be ("value1\nnext line!")
    attrs.getString("key2") should be ("value2\nYo\\yo")
    attrs.getString("key3") should be ("value 3")
  }
}
