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

trait ProgressListener {
  def init(totalExpectedProgress: Int, message: String)
  def init(totalExpectedProgress: Int)
  def setProgress(progress: Int)
  def incrementProgress(amount: Int)
  def done: Unit
  // WW: These were originally in org.systemsbiology.genomebrowser.app.ProgressListener
  // which is dead now
  def setExpectedProgress(expected: Int)
  def setMessage(message: String)
}

trait ProgressReporter {
  def addProgressListener(listener: ProgressListener)
  def removeProgressListener(listener: ProgressListener)
}
