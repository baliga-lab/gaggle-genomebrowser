package org.systemsbiology.util

import scala.reflect.BeanProperty

class NotImplementedException(message: String, cause: Throwable)
extends RuntimeException(message, cause) {

  def this() = this("", null)
  def this(message: String) = this(message, null)
  def this(cause: Throwable) = this("", cause)
}

trait Selectable {
  def selected: Boolean
  def setSelected(selected: Boolean)
}

/**
 * class to hold the link text and address (url) of a hyperlink.
 */
class Hyperlink(@BeanProperty val text: String, @BeanProperty val url: String)

/**
 * Represents an immutable pair of objects.
 */
class Pair[A, B](@BeanProperty val first: A, @BeanProperty val second: B) {
	
  override def hashCode = {
    val prime = 31
    var result = 1
    result = prime * result + (if (first == null) 0 else first.hashCode)
    result = prime * result + (if (second == null) 0 else second.hashCode)
    result
  }

  override def equals(obj: Any): Boolean = {
    val o = obj.asInstanceOf[AnyRef] // signature for Java compatibility, cast for Scala
    if (this eq o) true
    else if (o eq null) false
    else {
      obj match {
        case other: Pair[_, _] =>
          if (first == null && other.first != null) false
          else if (!first.equals(other.first)) false
          else if (second == null && other.second != null) false
          else if (!second.equals(other.second)) false
          else true
        case _ => false
      }
    }
  }

  override def toString = "(%s, %s)".format(String.valueOf(first), String.valueOf(second))
}
