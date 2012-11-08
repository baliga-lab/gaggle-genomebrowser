package org.systemsbiology.genomebrowser.model

import java.util.{List, Iterator}
import org.systemsbiology.util.Iteratable

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
