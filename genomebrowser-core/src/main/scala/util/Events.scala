package org.systemsbiology.util

import java.awt.event.{ActionEvent, ActionListener}
import java.util.Set
import java.util.concurrent.CopyOnWriteArraySet
import scala.collection.JavaConversions._

// TODO should be unified with application events?
/** holds references to action listeners */
class ActionListenerSupport {
  private val listeners = new CopyOnWriteArraySet[ActionListener]

  def addActionListener(listener: ActionListener) = listeners.add(listener)
  def removeActionListener(listener: ActionListener) = listeners.remove(listener)
  def fireActionEvent(event: ActionEvent) {
    listeners.map(_.actionPerformed(event))
  }
}

trait DialogListener {
  def ok(action: String, result: AnyRef)
  def cancel
  def error(message: String, e: Exception)
}
