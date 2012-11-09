package org.systemsbiology.genomebrowser.bookmarks

import scala.reflect.BeanProperty
import java.util.Arrays

import org.systemsbiology.genomebrowser.model.{Feature, Strand}
import org.systemsbiology.genomebrowser.util.Attributes
import org.systemsbiology.genomebrowser.util.Attributes._

trait BookmarkCatalogListener {
	def addBookmarkDataSource(dataSource: BookmarkDataSource): Unit
	def removeBookmarkDataSource(dataSource: BookmarkDataSource): Unit
	def renameBookmarkDataSource(dataSource: BookmarkDataSource): Unit
	def updateBookmarkCatalog: Unit
}

object BookmarkFactory {
  /**
   * Complex constructor changed to factory method
   * Create a bookmark from the given feature. If the feature is a named
   * feature, we associate that feature with the bookmark by recording its
   * name.
   */
  def createBookmark(feature: Feature): Bookmark = {
    val bookmark = feature match {
      case nf:Feature#NamedFeature =>
        val bm = new Bookmark(nf.getSeqId, nf.getStrand, nf.getStart,
                              nf.getEnd, nf.getLabel, null, null, null)
        bm.setAssociatedFeatureNames(Array(nf.getName))
        bm
      case bm:Bookmark =>
        val bm2 = new Bookmark(bm.getSeqId, bm.getStrand, bm.getStart,
                               bm.getEnd, bm.getLabel, bm.annotation, bm.sequence)
        bm2.setAssociatedFeatureNames(bm.getAssociatedFeatureNames)
        bm2.attributes = bm.attributes
        bm2
      case _ =>
        new Bookmark(feature.getSeqId, feature.getStrand, feature.getStart,
                     feature.getEnd, feature.getLabel, null, null, null)
    }
    bookmark
  }
}

/**
 * A Bookmark is a specialization of Feature, which can have a text annotation
 * attached. Bookmarks may also have associated features. The associated features
 * are identified by names, since we don't have an enforced unique identifier for
 * features in general. Instances of NamedFeature have a name
 */
class Bookmark(@BeanProperty var seqId: String,
               var strand: Strand,
               @BeanProperty var start: Int,
               @BeanProperty var end: Int,
               var label: String,
               var annotation: String,
               var attributes: String,
               var sequence: String) extends Feature {

  // attributes field of the form "key1=value1;key2=value2;"

  // to be used by specialized bookmarks (ex: transcript boundaries)
  private var associatedFeatureName: Array[String] = null

  def this() = this(null, null, 0, 0, null, null, null, null)
  def this(seqId: String, strand: Strand, start: Int, end: Int) =
    this(seqId, strand, start, end, null, null, null, null)

  def this(seqId: String, start: Int, end: Int) = this(seqId, Strand.none, start, end);

  def this(seqId: String, strand: Strand, start: Int, end: Int, label: String,
           annotation: String) = this(seqId, strand, start, end, label, annotation, null, null)

  def this(seqId: String, strand: Strand, start: Int, end: Int, label: String,
           annotation: String, attributes: String) =
    this(seqId, strand, start, end, label, annotation, attributes, null)

  // overflow-safe integer average
  def getCentralPosition = (start + end) >>> 1
  def getAnnotation = if (annotation == null) "" else annotation
  def setAnnotation(annotation: String) { this.annotation = annotation }	
  def getSequence = if (sequence == null) "" else sequence
  def setSequence(sequence: String) { this.sequence = sequence }
  def getStrand = if (strand == null) Strand.none else strand
  def setStrand(strand: Strand) { this.strand = strand }
  def getLabel = {
    if (label == null) ("[" + getStart + ", " + getEnd + "]") else label
  }
  def setLabel(label: String) { this.label = label }
  def getToolTip = annotation
  /**
   * Associated features are the names of features associated with this
   * bookmark.
   */
  def getAssociatedFeatureNames: Array[String] = associatedFeatureName
  def getAttributes: Attributes = Attributes.parse(attributes)
  def getAttributesString = attributes
  def setAttributes(attributes: String) { this.attributes = attributes }
  def hasAttributes = this.attributes != null
  def setAssociatedFeatureNames(associatedFeatureName: Array[String]) {
    this.associatedFeatureName = associatedFeatureName
  }

  override def hashCode = {
    val prime = 31
    var result = 1
    result = prime * result + (if (annotation == null) 0 else annotation.hashCode)
    result = prime * result + Arrays.hashCode(associatedFeatureName.asInstanceOf[Array[AnyRef]])
    result = prime * result + end
    result = prime * result + (if (label == null) 0 else label.hashCode)
    result = prime * result + (if (seqId == null) 0 else seqId.hashCode)
    result = prime * result + start
    result = prime * result + (if (strand == null) 0 else strand.hashCode)
    result = prime * result + (if (attributes == null) 0 else attributes.hashCode)
    result = prime * result + (if (sequence == null) 0 else sequence.hashCode)
    result
  }

  override def equals(obj: Any) = {
    if (this == obj) true
    else if (obj == null) false
    else obj match {
      case other:Bookmark =>
        if (annotation == null && other.annotation != null) false
        else if (annotation != null && !annotation.equals(other.annotation)) false
        else if (!Arrays.equals(associatedFeatureName.asInstanceOf[Array[AnyRef]],
                                other.associatedFeatureName.asInstanceOf[Array[AnyRef]])) false
        else if (end != other.end) false
        else if (label == null && other.label != null) false
        else if (label != null && !label.equals(other.label)) false
        else if (seqId == null && other.seqId != null) false
        else if (seqId != null && !seqId.equals(other.seqId)) false
        else if (start != other.start) false
        else if (strand == null && other.strand != null)	false
        else if (strand != null && !strand.equals(other.strand)) false
        else if (attributes == null && other.attributes != null)	false
        else if (attributes != null && !attributes.equals(other.attributes)) false
        else true
      case _ => false
    }
  }

  override def toString = {
    if (label != null) label
    else "%s%s:%d-%d".format(seqId, strand.toAbbreviatedString, start, end)
  }
}
