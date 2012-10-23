package org.systemsbiology.genomebrowser.model

import org.systemsbiology.util.Iteratable
import java.util.List

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
