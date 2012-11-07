package org.systemsbiology.util

import java.io.Serializable
import java.util.{List, ArrayList}
import org.systemsbiology.gaggle.core.datatypes.{Single, Tuple}
import scala.collection.JavaConversions._

// tuples might be used in (at least) a couple of ways. The obvious is to
// assign key-value pairs that describe the object that owns the Tuple. So, if
// a track owns a tuple, the tuple holds its attributes (color, position, etc.)
// A tuple could also be used as an RDF triple. The tuple could have three
// values - subject, attribute, value - or more generally - object, relation,
// object. As in RDF then, a list of tuples can describe an object graph.

/**
 * Extends the Gaggle Tuple datatype with some convenience functions.
 * 
 * The get and set functions are to be used with unique keys (the names of
 * the Singles). This differs from add which will add any number of Singles
 * with the same name to the Tuple.  
 */
class NiceTuple(name: String, singleList: List[Single]) extends Tuple(name, singleList) {

  def this() = this(null, new ArrayList[Single])
  def this(name: String) = this(name, new ArrayList[Single])
  def this(tuple: Tuple) = this(tuple.getName, tuple.getSingleList)

  def add(value: Serializable) = super.addSingle(new Single(value))
  def add(name: String, value: Serializable) = super.addSingle(new Single(name, value))
  /**
   * Sets the first existing single with the given name to
   * the given value. If no entry exists with the specified
   * name, create one.
   * @return true if the key already existed.
   */
  def set(name: String, value: Serializable): Boolean = {
    for (single <- getSingleList) {
      if (name == single.getName) {
        single.setValue(value)
        return true
      }
    }
    add(name, value)
    false
  }

  private def _get(name: String): Option[Serializable] = {
    for (single <- getSingleList) {
      if (name == single.getName) Some(single.getValue)
    }
    None
  }

  /**
   * @return the value associated with the given key. (The first such
   * value, if there is more than one)
   */
  def get(name: String): Serializable = _get(name).getOrElse(null)

  def get(name: String, defaultValue: Serializable): Serializable = {
    _get(name).getOrElse(defaultValue)
  }

  // ---- convenience methods for basic data types --------------------------
	
  def getNotNull(name: String): Serializable = _get(name).get

  def getString(name: String): String = {
    _get(name) match {
      case Some(value) => String.valueOf(value)
      case _ => null
    }
  }

  def getString(name: String, defaultValue: String): String = {
    String.valueOf(_get(name).getOrElse(defaultValue))
  }

  def getInt(name: String): Int = {
    val value = getNotNull(name)
    value match {
      case num:Number => num.intValue
      case str:String => Integer.parseInt(str)
      case _ =>
        throw new ClassCastException("Can't convert a " + value.getClass.getName +
                                     " to an integer")
    }
  }

  def getInt(name: String, defaultValue: Int): Int = {
    get(name) match {
      case num:Number => num.intValue
      case str:String => Integer.parseInt(str)
      case _ => defaultValue
    }
  }

  def getDouble(name: String): Double = {
    val value = getNotNull(name)
    value match {
      case num:Number => num.doubleValue
      case str:String => java.lang.Double.parseDouble(str)
      case _ =>
        throw new ClassCastException("Can't convert a " + value.getClass.getName +
                                     " to a double")
    }
  }

  def getDouble(name: String, defaultValue: Double): Double = {
    get(name) match {
      case num:Number => num.doubleValue
      case str:String => java.lang.Double.parseDouble(str)
      case _ => defaultValue
    }
  }

  def getBoolean(name: String): Boolean = {
    val value = getNotNull(name)
    value match {
      case bval:java.lang.Boolean => bval
      case str:String => java.lang.Boolean.parseBoolean(str)
      case _ =>
        throw new ClassCastException("Can't convert a " + value.getClass.getName +
                                     " to a boolean")
    }
  }

  def getBoolean(name: String, defaultValue: Boolean): Boolean = {
    get(name) match {
      case bval:java.lang.Boolean => bval
      case str:String => java.lang.Boolean.parseBoolean(str)
      case _ => defaultValue
    }
  }

  /**
   * convert back to Gaggle Tuple
   */
  def toTuple: Tuple = new Tuple(getName, getSingleList)
}
