package org.systemsbiology.genomebrowser.model

import scala.reflect.BeanProperty
import org.systemsbiology.util.{Iteratable, IteratableWrapper}
import java.util.{List, Iterator}

/**
 * A Feature is anything that has a start and end coordinate on a sequence.
 * The feature may pertain to a particular strand or not.
 */
trait Feature {
  def getSeqId: String
  def getStrand: Strand
  def getStart: Int
  def getEnd: Int
  def getCentralPosition: Int
  def getLabel: String

  trait Quantitative extends Feature {
    def getValue: Double
  }
  trait QuantitativePvalue extends Quantitative {
    def getPvalue: Double
  }
  trait NamedFeature extends Feature {
    def getName: String
  }
  trait ScoredFeature extends Feature {
    def getScore: Double
  }
  trait ScaledQuantitative extends Quantitative {
    def getMin: Double
    def getMax: Double
  }
  trait Matrix extends Quantitative {
    def getValues: Array[Double]
  }
  trait Nested extends Feature {
    def getFeatures: List[Feature]
  }
}

/**
 * A block is a bunch of contiguous features on the same sequence and
 * strand that can be loaded and cached as a unit. Blocks are implemented
 * in some cases (PositionalBlock and SegmentBlock) by returning flyweight
 * features. So, features returned from a Block's iterators should be used
 * immediately and not stored or passed to functions that might store them.
 * @author cbare
 * @see org.systemsbiology.genomebrowser.sqlite.PositionalBlock
 * @see org.systemsbiology.genomebrowser.sqlite.SegmentBlock
 */
trait Block[F <: Feature] extends java.lang.Iterable[F] {
  def getSequence: Sequence
  def getStrand: Strand
  def features: Iteratable[F]
  def features(start: Int, end: Int): Iteratable[F]
}

/**
 * An entry in a list that can be quickly searched for blocks
 * overlapping a given feature filter.
 */
class BlockEntry[F <: Feature](val key: FeatureFilter, val block: Block[F])

/**
 * A simple implementation of Block backed by a List of features for use when
 * holding in memory all features in a track is desirable for performance and
 * not prohibitively large. (Gene tracks.)
 */
class FeatureBlock[F <: Feature](@BeanProperty val sequence: Sequence,
                                 @BeanProperty val strand: Strand,
                                 _features: List[F]) extends Block[F] {

	def features: Iteratable[F] = new IteratableWrapper[F](_features.iterator)
	def features(start: Int, end: Int): Iteratable[F] =
    new FeatureIteratable[F](_features, start, end)
	def iterator = _features.iterator
}
