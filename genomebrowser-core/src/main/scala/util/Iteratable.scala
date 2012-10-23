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
