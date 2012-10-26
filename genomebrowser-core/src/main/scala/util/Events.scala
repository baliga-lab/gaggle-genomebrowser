package org.systemsbiology.util

import java.awt.event.{ActionEvent, ActionListener}
import java.util.Set
import java.util.concurrent.CopyOnWriteArraySet
import scala.collection.JavaConversions._
import org.apache.log4j.Logger

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
  def init(totalExpectedProgress: Int, message: String): Unit
  def init(totalExpectedProgress: Int): Unit
  def setProgress(progress: Int): Unit
  def incrementProgress(amount: Int): Unit
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

class LoggingProgressListener(log: Logger) extends ProgressListener {

  def this(logger: String) = this(Logger.getLogger(logger))
  def incrementProgress(amount: Int) {
    log.info("increment progress = " + amount)
  }
  def init(expected: Int) {
    log.info("init(): expected progress = " + expected)
  }
  def init(expected: Int, message: String) {
    log.info("init(): expected progress = " + expected + " msg: " + message)
  }
  def setExpectedProgress(expected: Int) {
    log.info("expected progress = " + expected)
  }
  def setProgress(progress: Int) {
    log.info("progress = " + progress)
  }
  def setMessage(message: String) {
    log.info("progress message = " + message)
  }
  def done {
    log.info("progress done!")
  }
}

class RunnableProgressReporter(progress: Progress)
extends ProgressReporter with Runnable {
  private val progressListenerSupport = new ProgressListenerSupport
  private var _done : Boolean = false // volatile in Java original

  def addProgressListener(listener: ProgressListener) {
    progressListenerSupport.addProgressListener(listener)
    // this might not be strictly correct. The listener might get
    // two done notifications, but I don't think it could get none?
    if (_done) listener.done
  }

  def removeProgressListener(listener: ProgressListener) {
    progressListenerSupport.removeProgressListener(listener)
  }

  def done { _done = true }

  def run {
    while (!_done) {
      val pair = progress.getProgressAndExpected
      progressListenerSupport.fireSetExpectedProgressEvent(pair.getSecond)
      progressListenerSupport.fireProgressEvent(pair.getFirst)
      try {
        Thread.sleep(400)
      } catch {
        case _ => _done = true
      }
      
      if (Thread.currentThread().isInterrupted)	_done = true
    }
    val pair = progress.getProgressAndExpected
    progressListenerSupport.fireSetExpectedProgressEvent(pair.getSecond)
    progressListenerSupport.fireProgressEvent(pair.getFirst)
    progressListenerSupport.fireDoneEvent
  }

  def start { new Thread(this).start }
}
