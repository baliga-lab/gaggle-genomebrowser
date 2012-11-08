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

/**
 * Support code for a class that provides progress events.
 */
class ProgressListenerSupport
extends Iterable[ProgressListener] with ProgressReporter with ProgressListener {

  private val listeners = new CopyOnWriteArraySet[ProgressListener]

  def addProgressListener(l: ProgressListener) = listeners.add(l)
  def removeProgressListener(l: ProgressListener) = listeners.remove(l)
  def iterator = listeners.iterator
  def fireIncrementProgressEvent(amount: Int) = listeners.map(_.incrementProgress(amount))
  def fireProgressEvent(progress: Int) = listeners.map(_.setProgress(progress))
  def fireProgressEvent(progress: Int, expected: Int) {
    listeners.map(listener => {
      listener.setExpectedProgress(expected);
      listener.setProgress(progress);
    })
  }
  def fireSetExpectedProgressEvent(expected: Int) =
    listeners.map(_.setExpectedProgress(expected))
  
  def fireInitEvent(expected: Int) = listeners.map(_.init(expected))
  def fireInitEvent(expected: Int, message: String) = listeners.map(_.init(expected, message))
  def fireMessageEvent(message: String) = listeners.map(_.setMessage(message))
  def fireDoneEvent = listeners.map(_.done)

  // ---- progress listener methods -----------------------------------------
	
  // We implement these for forwarding progress from one source to listeners
  // of this source. This usually means that a process that reports progress
  // is being used as a part of a larger process. So, we it makes sense to
  // forward increments and messages, but probably not other progress events

  def done { }
  def init(expected: Int) { }
  def init(expected: Int, message: String) { }
  def setExpectedProgress(expected: Int) { }
  def setProgress(progress: Int) { }
  def incrementProgress(amount: Int) = fireIncrementProgressEvent(amount)
  def setMessage(message: String) = fireMessageEvent(message)
}

class ProgressListenerWrapper(progressListenerSupport: ProgressListenerSupport)
extends ProgressListener {
  private var start    = 0
  private var width    = 0
  private var expected = 100

  def this() = this(new ProgressListenerSupport())

  /**
   * Scale the progress reporting to fit within a subinterval of
   * the progress of a larger process. Let's say we have a 5 step
   * process. Step 2 reports its own progress on a scale of 0-100.
   * We want to adapt the progress of step 2 to cover the range of
   * 20-20
   * @param start
   * @param end
   */
  def scaleProgressToFit(start: Int, end: Int) {
    this.start = start
    this.width = end - start
  }
  def done = progressListenerSupport.fireDoneEvent
  def incrementProgress(amount: Int) =
    progressListenerSupport.fireIncrementProgressEvent(amount)

  def init(expected: Int) {
    this.expected = expected
    progressListenerSupport.fireInitEvent(expected)
  }
  def init(expected: Int, message: String) {
    this.expected = expected
    progressListenerSupport.fireInitEvent(expected, message)
  }
  def setExpectedProgress(expected: Int) {
    this.expected = expected;
    if (width <= 0)	progressListenerSupport.fireSetExpectedProgressEvent(expected)
  }
  def setMessage(message: String) = progressListenerSupport.fireMessageEvent(message)
  def setProgress(progress: Int) {
    val scaledProgress = if (width > 0)
      (start + progress.toDouble / expected.toDouble * width).toInt
                         else progress
    progressListenerSupport.fireProgressEvent(scaledProgress)
  }
  def addProgressListener(l: ProgressListener) = progressListenerSupport.addProgressListener(l)
  def removeProgressListener(l: ProgressListener) =
    progressListenerSupport.removeProgressListener(l)
}
