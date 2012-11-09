package org.systemsbiology.genomebrowser.model

import scala.reflect.BeanProperty

/**
 * Location on the genome. For positional features, use start as position.
 */
class Coordinates(@BeanProperty var seqId: String,
                  @BeanProperty var strand: Strand,
                  @BeanProperty var start: Int,
                  @BeanProperty var end: Int)
extends Comparable[Coordinates] {

	def this(seqId: String, strand: Strand, position: Int) = this(seqId, strand, position,
                                                                position)

	def getPosition = start

	override def toString = {
		val sb = new StringBuilder("(")
		sb.append(seqId).append(", ")
		sb.append(strand.toAbbreviatedString()).append(", ")
		sb.append(start).append(", ")
		sb.append(end).append(")")
		sb.toString
	}

	override def compareTo(other: Coordinates): Int = {
		if (other == null) -1
    else {
		  var result = this.seqId.compareTo(other.getSeqId)
		  if (result == 0) {
			  result = this.strand.compareTo(other.strand)
			  if (result == 0) {
				  result = this.start - other.start
				  if (result == 0) result = this.end - other.end				 
			  }
		  }
		  result
	  }	
  }
}

/**
 * Take a String and return a set of coordinates on the genome.
 */
trait CoordinateMap {
	def getCoordinates(name: String): Coordinates
	
	// do the coordinates map to positions (a single location on the genome) as opposed
	// to segments (a range of locations)?
	def isPositional: Boolean
}

/**
 * Map a name to coordinates on the genome.
 */
trait CoordinateMapper {
  def map(name: String): Coordinates
}
