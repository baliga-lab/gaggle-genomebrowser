package org.systemsbiology.util

/**
 * Isn't it irritating that an Iterator isn't Iterable? Idiotic,
 * ill conceived, and intolerable!
 */
trait Iteratable[E] extends java.util.Iterator[E] with java.lang.Iterable[E]

/** Wraps an iterator and makes it iterable. */
class IteratableWrapper[T](iter: java.util.Iterator[T]) extends Iteratable[T] {
  def hasNext: Boolean = iter.hasNext
  def next = iter.next
  def remove = iter.remove
  def iterator = iter
}

class MultiIteratable[E](iterables: java.lang.Iterable[_ <: java.lang.Iterable[_ <: E]])
extends Iteratable[E] {

  val ii = iterables.iterator
  var _iterator: java.util.Iterator[_ <: E] = null

  def hasNext: Boolean = {
    while (_iterator==null || !_iterator.hasNext) {
      if (!ii.hasNext) return false;
      _iterator = ii.next.iterator
    }
    return _iterator != null && _iterator.hasNext
  }
  def next = _iterator.next
  def remove {
    throw new UnsupportedOperationException("remove() not supported.");
  }
  def iterator: java.util.Iterator[E] = this
}
