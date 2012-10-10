package org.systemsbiology.genomebrowser.util

import scala.collection.JavaConversions._
import java.awt.Color
import java.util.{Collections, HashMap, Map, Set}

/**
 * Attributes is a set of convenience methods added to a HashMap
 * for retrieving values of various types using a String key. It's
 * meant to store an arbitrary set of properties.
 */
@SerialVersionUID(7921514258446479640L)
class Attributes(map: Map[String, AnyRef]) {
  import Attributes._

  def this() = this(new HashMap[String, AnyRef])
  private def _thisMap = map
  def put(key: String, value: Boolean) = map.put(key, new java.lang.Boolean(value))
  def put(key: String, value: String) = map.put(key, value)
  def put(key: String, value: AnyRef) = map.put(key, value)
  def putAll(aMap: Map[String, AnyRef]) = map.putAll(aMap)
  // can't put attrs.map directly - seems to be a compiler bug
  def putAll(attrs: Attributes) = map.putAll(attrs._thisMap)
  def get(key: String): AnyRef = map.get(key)
  def remove(key: String) = map.remove(key)
  def entrySet = map.entrySet
  def keySet = map.keySet
  def containsKey(key: String) = map.containsKey(key)

  def getString(key: String, defaultValue: String): String = {
    if (!map.containsKey(key)) defaultValue else map.get(key).toString
  }
  def getString(key: String): String = getString(key, null)

  def getInt(key: String, defaultValue: Int): Int = {
    if (!map.containsKey(key)) defaultValue
    else map.get(key) match {
      case num: Number => num.intValue
      case str: String => Integer.parseInt(str)
      case _ => throw new ClassCastException("Can't convert a " + getClassName(map.get(key)) +
                                             " to an integer")
    }
  }
  def getInt(key: String): Int = getInt(key, 0)

  def getFloat(key: String, defaultValue: Float): Float = {
    if (!map.containsKey(key)) defaultValue
    else {
      map.get(key) match {
        case num: Number => num.floatValue
        case str: String => java.lang.Float.parseFloat(str)
        case _ => throw new ClassCastException("Can't convert a " + getClassName(map.get(key)) +
                                               " to a float")
      }
    }
  }
  def getFloat(key: String): Float = getFloat(key, 0.0f)

  def getDouble(key: String, defaultValue: Double): Double = {
    if (!map.containsKey(key)) defaultValue
    else map.get(key) match {
      case num: Number => num.doubleValue
      case str: String => if (str == "") defaultValue else java.lang.Double.parseDouble(str)
      case _ => throw new ClassCastException("Can't convert a " + getClassName(map.get(key)) +
                                             " to a double")
    }
  }
  def getDouble(key: String): Double = getDouble(key, 0.0)

  def getBoolean(key: String, defaultValue: Boolean): Boolean = {
    if (!map.containsKey(key)) defaultValue
    else map.get(key) match {
      case boolval: java.lang.Boolean => boolval.booleanValue
      case str: String => java.lang.Boolean.parseBoolean(str)
      case _ => throw new ClassCastException("Can't convert a " + getClassName(map.get(key)) +
                                             " to a boolean")
    }
  }
  def getBoolean(key: String): Boolean = getBoolean(key, false)

  def getColor(key: String, defaultValue: Color): Color = {
    if (!map.containsKey(key)) defaultValue
    else map.get(key) match {
      case color: Color => color
      case num: Number => new Color(num.intValue, true)
      case str: String => ColorUtils.decodeColor(str)
      case _ => throw new ClassCastException("Can't convert a " + getClassName(map.get(key)) +
                                             " to a Color")
    }
	}
  def getColor(key: String): Color = getColor(key, Color.BLACK)

  def getClassName(o: AnyRef) = if (o == null) "null" else o.getClass.getName

  /**
   * @return a string representation of the map in the form
   * "key1=value1;key2=value2;" suitable for inclusion in TSV files.
   */
  def toAttributesString: String = {
    val sb = new StringBuilder
    for (entry <- map.entrySet) {
      sb.append(escape(entry.getKey)).append('=').append(escape(String.valueOf(entry.getValue))).
      append(';')
    }
    sb.toString
  }
}

// Companion object for Attributes
object Attributes {
  /**
   * Parses a string of the form "key1=value1;key2=value2;".
   * TODO doesn't handle escaped delimiters ("\=", "\;").
   * @return an Attributes object populated with the key/value pairs parsed from the string.
   */
  def parse(attributes: String): Attributes = {
    val results = new Attributes
    if (attributes != null) {
      val pairs = attributes.split(";")
      for (pair <- pairs) {
        if (pair.length > 0) {
          val fields = pair.split("=")
          val key = unescape(fields(0))
          val value = unescape(fields(1))
          results.put(key, value)
        }
      }
    }
    results
  }
  
  private def unescape(string: String): String = {
    string.replace("\\n","\n").replace("\\\\", "\\")
  }

  private def escape(string: String): String = {
    string.replace("\\", "\\\\").replace("\n","\\n")
  }

  // a semi-safe EMPTY attributes object. Be nice and don't
  // add attributes to it.
  val EMPTY = new Attributes {
    //private static final long serialVersionUID = -7199122687556831258L;
    override def put(key: String, value: Boolean) = null
    override def put(key: String, value: String) = null
    override def entrySet = Collections.emptySet()
  }
}
