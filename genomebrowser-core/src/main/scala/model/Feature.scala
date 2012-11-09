package org.systemsbiology.genomebrowser.model

import scala.reflect.BeanProperty
import java.util.{List, Iterator}
import org.systemsbiology.util.{Iteratable, ProgressListener}
import org.systemsbiology.util.MathUtils

// TODO should it be FeatureFields responsibility to validate Strand and GeneType values?
// Those values must come from a restricted set of strings.

/**
 * A feature is a position on the genome augmented by some arbitrary additional
 * fields. This class is an attempt to accommodate all known cases as a superset
 * of all fields currently in use by a quantitative segment, quantitative positional,
 * or gene feature. We also allow sequence to be specified as as arbitrary string
 * that will be mapped to a standard sequence ID later.
 * 
 * Is this adequate for extensibility to unknown feature types?
 * 
 * They may have other fields (error bars or p-values for example). That might be
 * covered by subclassing FeatureFields. Other cases might encode some or all fields
 * in a String (chr+:1000-1200, for example). These cases are handled already (in 
 * DataMatrixFeatureSource, for example) by doing the conversion first in the
 * FeatureSource implementation, then calling featureProcessor.process(fields).
 * 
 * Maybe something more flexible? Start with a truly arbitrary
 * set of fields, accompanied by a schema and a means of transforming that schema
 * to a standard form known to the program. Carrying the schema with each object
 * would be inefficient. Maybe a collection of similar FeatureFeilds object with a
 * schema attached to the collection?
 */
trait FeatureFields {
  def getSequenceName: String
  def getStrand: String
  def getStart: Int
  def getEnd: Int
  def getPosition: Int
  def getValue: Double
  def getName: String
  def getCommonName: String
  def getGeneType: String
}

trait FeatureProcessor {
  @throws(classOf[Exception]) def process(fields: FeatureFields)
  def getCount: Int
  def cleanup: Unit
}

/**
 * The purpose of this interface is to connect a source of features to
 * a "sink" for features -- the FeatureProcessor. It is used, for example,
 * to load features from various file formats and store them in a datastore,
 * whose implementation is supposed to be flexible. The source can stream
 * feature information to limit memory usage.
 * 
 * The pattern used here is a double-dispatch something like the Visitor
 * pattern in order to process features in a manner dependent on both source
 * and consumer.
 * 
 * I considered a producer-consumer implementation using a thread-safe queue,
 * but the concurrency complicates things without much benefit. The real issue
 * here is to allow variation of both the producer and consumer and not to
 * manage the workload in any special way.
 * 
 * A FeatureSource is like an iterator except the FeatureSource pushes features
 * to the FeatureProcessor rather than having the consumer pull features from
 * the iterator. This makes it easier for the FeatureSource to ensure that
 * proper cleanup happens, such as closing files.
 *
 * It's still possible that there's one too many levels of indirection going on
 * here or maybe an Iterator would have worked just as well. 
 */
trait FeatureSource {
  @throws(classOf[Exception])
  def processFeatures(featureProcessor: FeatureProcessor)

  // TODO remove progress methods from FeatureSource
  def addProgressListener(progressListener: ProgressListener)
  def removeProgressListener(progressListener: ProgressListener)
}

/**
 * Iterates the subset of a list of features that falls inside the range
 * given by the start and end constructor parameters.
 */
class FeatureIteratable[F <: Feature](features: List[F], start: Int, end: Int)
extends Iteratable[F] {
	val len = if (features == null) 0 else features.size
	var _next = 0

	def hasNext: Boolean = {
		while (_next < len && features.get(_next).getEnd < start) {
			_next += 1
		}
		(_next < len) && features.get(_next).getStart < end
	}

	def next: F = {
    val i = _next
    _next += 1
		features.get(i)
	}

	def remove =
		throw new UnsupportedOperationException("remove not supported in feature iterators");

	def iterator : Iterator[F] = this
}

class BasicFeature(@BeanProperty var seqId: String,
                   @BeanProperty var strand: Strand,
                   @BeanProperty var start: Int,
                   @BeanProperty var end: Int,
                   var label: String) extends Feature {
  def this(seqId: String, strand: Strand, start: Int, end: Int) =
    this(seqId, strand, start, end, null)

  def getLabel = if (label == null)
    "%s%s:%d-%d".format(seqId, strand.toAbbreviatedString, start, end)
                 else label
  def setLabel(label: String) { this.label = label }
  def getCentralPosition: Int = MathUtils.average(start, end)
}

class BasicPositionalFeature(@BeanProperty var seqId: String,
                             @BeanProperty var strand: Strand,
                             position: Int)
extends Feature {
  def this() = this(null, null, 0)
  def this(feature: Feature) = this(feature.getSeqId, feature.getStrand,
                                    feature.getCentralPosition)
  def getStart = position
  def getEnd = position
  def getCentralPosition = position
  def getLabel =
    "%s%s:%d".format(seqId, strand.toAbbreviatedString, position)
}

/**
 * A simple implementation of a quantitative feature. In practice,
 * quantitative features will usually be implemented using Flyweights
 * backed by primitive arrays. This implementation exists for small
 * quantities of data and for testing.
 */
/*
class BasicQuantitativeFeature(@BeanProperty val seqId: String,
                               @BeanProperty val strand: Strand,
                               @BeanProperty val start: Int,
                               @BeanProperty val end: Int,
                               @BeanProperty val value: Double)
extends Quantitative {

  // average without overflow
  def getCentralPosition: Int = (start + end) >>> 1
  def getLabel = "%.2f".format(value)
  override def toString = {
    "Feature(" + seqId + ", " + strand.toAbbreviatedString() + ", " + start + ", " +
    end + ", " + value + ")"
  }
}
*/
