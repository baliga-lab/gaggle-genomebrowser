package org.systemsbiology.genomebrowser.event

import java.util.Set
import java.util.concurrent.CopyOnWriteArraySet
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._

// TODO: write a script to find all event action strings
/**
 * An event with a source and an arbitrary action. Optionally, events can
 * specify that they require the UI to repaint itself.
 */
class Event(@BeanProperty val source: AnyRef,
            @BeanProperty val action: String,
            @BeanProperty val data: AnyRef,
            repaint: Boolean,
            @BeanProperty val timestamp: Long) {

  def this(source: AnyRef, action: String) = this(source, action, null, false,
                                                  System.currentTimeMillis)
	def this(source: AnyRef, action: String, repaint: Boolean) = this(source, action,
                                                                    null, repaint,
                                                                    System.currentTimeMillis)
	def this(source: AnyRef, action: String, data: AnyRef) = this(source, action, data,
                                                                false,
                                                                System.currentTimeMillis)
	def this(source: AnyRef, action: String, data: AnyRef,
           repaint: Boolean) = this(source, action, data, repaint, System.currentTimeMillis)

	def requiresRepaint = repaint
	override def toString = {
		"(Event src=%s, action=%s, object=%s)".format(String.valueOf(source), action,
                                                  String.valueOf(data))
	}
}

trait EventListener {
	def receiveEvent(event: Event)
}

// might use this to tag components that produce events
// the underscore is an attempt to flag dependency methods?
trait EventSource {
	def setEventSupport(eventSupport: EventSupport)
}

class EventSupport {
	private val listeners = new CopyOnWriteArraySet[EventListener]

	def addEventListener(listener: EventListener) = listeners.add(listener)
	def removeEventListener(listener: EventListener) = listeners.remove(listener)
	def fireEvent(event: Event) { listeners.map(_.receiveEvent(event)) }
	def fireEvent(source: AnyRef, action: String) { fireEvent(new Event(source, action)) }
	def fireEvent(source: AnyRef, action: String, repaint: Boolean) {
    fireEvent(new Event(source, action, repaint))
  }
	def fireEvent(source: AnyRef, action: String, data: AnyRef) {
		fireEvent(new Event(source, action, data));
	}
	def fireEvent(source: AnyRef, action: String, data: AnyRef, repaint: Boolean) {
		fireEvent(new Event(source, action, data, repaint))
	}
}
