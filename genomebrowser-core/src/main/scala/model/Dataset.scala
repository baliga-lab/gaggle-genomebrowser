package org.systemsbiology.genomebrowser.model

import java.util.{Collections, List, UUID}

import org.systemsbiology.genomebrowser.util.Attributes

/**
 * A dataset is a set of sequences and a set of tracks holding features
 * on those sequences.
 */
trait Dataset {
  def getUuid: UUID
  def getName: String
  def getSequences: List[Sequence]
  def getSequence(seqId: String): Sequence

  def getTracks: List[Track[Feature]]
  def getTrack(name: String): Track[Feature]
  def addTrack(track: Track[_ <: Feature])
  def getAttributes: Attributes

  /**
   * @return an implementation of SequenceFetcher or null if there is none
   */
  def getSequenceFetcher: SequenceFetcher
}

object Datasets {
  val EMPTY_DATASET = new Dataset() {
    val attr = Attributes.EMPTY

    def getUuid = null
    def getAttributes = attr
    def getName = ""
    def getSequences = Collections.emptyList.asInstanceOf[List[Sequence]]
    def getSequence(seqId: String) = {
      throw new RuntimeException("Dataset does not contain a sequence named \"" +
                                 seqId + "\".")
    }
    def getTracks = Collections.emptyList.asInstanceOf[List[Track[Feature]]]
    def getTrack(name: String) = null
    def addTrack(track: Track[_ <: Feature]) {
      throw new RuntimeException("Can't add a track to the empty dataset");
    }
    def getSequenceFetcher = null
  }
}
