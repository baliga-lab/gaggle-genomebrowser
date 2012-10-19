package org.systemsbiology.genomebrowser.io

import java.io.File

import org.systemsbiology.genomebrowser.bookmarks._
import org.systemsbiology.genomebrowser.model.Strand
import org.systemsbiology.genomebrowser.util.Attributes

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BookmarkIOSpec extends FlatSpec with ShouldMatchers {

  "BookmarkIO" should "write and read bookmarks" in {
		val ds = new ListBookmarkDataSource("test.bookmarks")
		ds.add(new Bookmark("Chr", Strand.forward, 100, 200, "bogus", "a bogus bookmark"))
		ds.add(new Bookmark("Chr", Strand.reverse, 300, 400, "foo",
                        "a bogus bookmark named foo in the reverse strand."))
		ds.add(new Bookmark("Chr", Strand.none, 500, 512, "qwer",
                        "a bookmark with no strand orientation."))
		val temp = File.createTempFile("test.bookmarks", ".tsv")
		(new BookmarkWriter).writeBookmarks(temp, ds)
		val ds2 = (new BookmarkReader).loadData(temp);
		
		ds2.getName should be ("test.bookmarks")
    val iter2 = ds2.iterator
    while (iter2.hasNext) {
      val bookmark = iter2.next
			if ("bogus" == bookmark.getLabel) {
				bookmark.getSeqId should be ("Chr")
				bookmark.getStrand should be (Strand.forward)
				bookmark.getStart should be (100)
        bookmark.getEnd should be (200)
				bookmark.getAnnotation should be ("a bogus bookmark")
			} else if (bookmark.getLabel == "foo") {
				bookmark.getSeqId should be ("Chr")
				bookmark.getStrand should be (Strand.reverse)
				bookmark.getStart should be (300)
        bookmark.getEnd should be (400)
				bookmark.getAnnotation should be ("a bogus bookmark named foo in the reverse strand.")
			} else if (bookmark.getLabel == "qwer") {
				bookmark.getSeqId should be ("Chr")
				bookmark.getStrand should be (Strand.none)
				bookmark.getStart should be (500)
        bookmark.getEnd should be (512)
				bookmark.getAnnotation should be ("a bookmark with no strand orientation.")
			} else {
        fail("bookmark: " + bookmark.getLabel)
      }
    }
  }

  it should "save and read attributes correctly" in {
		val ds = new ListBookmarkDataSource("test.bookmarks")
		ds.add(new Bookmark("Chr", Strand.forward, 100, 200, "bogus",
                        "a bogus bookmark", "key1=value1;", "mysequence"))
		ds.add(new Bookmark("Chr", Strand.reverse, 300, 400, "foo",
                        "a bogus bookmark named foo in the reverse strand."))
		ds.add(new Bookmark("Chr", Strand.none, 500, 512, "qwer",
                        "a bookmark with no strand orientation.", "annot2",
                        "a=1;b=123;c=40001;key1=value 1;fiddle=faddle;"))
		val temp = File.createTempFile("test.bookmarks", ".tsv")
    new BookmarkWriter().writeBookmarks(temp, ds)
		val ds2 = new BookmarkReader().loadData(temp)
		ds2.getName should be ("test.bookmarks")

    val iter2 = ds2.iterator
		while (iter2.hasNext) {
      val bookmark = iter2.next
			if (bookmark.getLabel == "bogus") {
				bookmark.getSeqId should be ("Chr")
				bookmark.getStrand should be (Strand.forward)
				bookmark.getStart should be (100)
        bookmark.getEnd should be (200)
				bookmark.getAnnotation should be ("a bogus bookmark")
        bookmark.getAttributesString should be ("key1=value1;")
        bookmark.getAttributes.getString("key1") should be ("value1")
        bookmark.getSequence should be ("mysequence")
			} else if (bookmark.getLabel == "foo") {
				bookmark.getSeqId should be ("Chr")
				bookmark.getStrand should be (Strand.reverse)
				bookmark.getStart should be (300)
        bookmark.getEnd should be (400)
				bookmark.getAnnotation should be ("a bogus bookmark named foo in the reverse strand.")
			} else if (bookmark.getLabel == "qwer") {
				bookmark.getSeqId should be ("Chr")
				bookmark.getStrand should be (Strand.none)
				bookmark.getStart should be (500)
        bookmark.getEnd should be (512)
				bookmark.getAnnotation should be ("a bookmark with no strand orientation.")
			} else {
        fail("bookmark: " + bookmark.getLabel)
      }
    }
  }
}
