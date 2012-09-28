package org.systemsbiology.genomebrowser.io;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.systemsbiology.genomebrowser.bookmarks.*;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Attributes;


public class TestBookmarkIO {
	private static final Logger log = Logger.getLogger(TestBookmarkIO.class);

	@Test
	public void test() throws Exception {
		BookmarkDataSource ds = new ListBookmarkDataSource("test.bookmarks");
		ds.add(new Bookmark("Chr", Strand.forward, 100, 200, "bogus", "a bogus bookmark"));
		ds.add(new Bookmark("Chr", Strand.reverse, 300, 400, "foo", "a bogus bookmark named foo in the reverse strand."));
		ds.add(new Bookmark("Chr", Strand.none, 500, 512, "qwer", "a bookmark with no strand orientation."));
		
		File temp = File.createTempFile("test.bookmarks", ".tsv");

		BookmarkWriter writer = new BookmarkWriter();
		writer.writeBookmarks(temp, ds);
		
		BookmarkReader reader = new BookmarkReader();
		BookmarkDataSource ds2 = reader.loadData(temp);
		
		assertEquals("test.bookmarks", ds2.getName());
		
		for (Bookmark bookmark : ds2) {
			log.info("bookmark=" + bookmark.toString());
			if ("bogus".equals(bookmark.getLabel())) {
				assertEquals("Chr", bookmark.getSeqId());
				assertEquals(Strand.forward, bookmark.getStrand());
				assertEquals(100, bookmark.getStart());
				assertEquals(200, bookmark.getEnd());
				assertEquals("a bogus bookmark", bookmark.getAnnotation());
			}
			else if ("foo".equals(bookmark.getLabel())) {
				assertEquals("Chr", bookmark.getSeqId());
				assertEquals(Strand.reverse, bookmark.getStrand());
				assertEquals(300, bookmark.getStart());
				assertEquals(400, bookmark.getEnd());
				assertEquals("a bogus bookmark named foo in the reverse strand.", bookmark.getAnnotation());
			}
			else if ("qwer".equals(bookmark.getLabel())) {
				assertEquals("Chr", bookmark.getSeqId());
				assertEquals(Strand.none, bookmark.getStrand());
				assertEquals(500, bookmark.getStart());
				assertEquals(512, bookmark.getEnd());
				assertEquals("a bookmark with no strand orientation.", bookmark.getAnnotation());
			}
			else {
				fail("bookmark: " + bookmark.getLabel());
			}
		}
	}


	@Test
	public void testAttributes() throws Exception {
		BookmarkDataSource ds = new ListBookmarkDataSource("test.bookmarks");
		ds.add(new Bookmark("Chr", Strand.forward, 100, 200, "bogus", "a bogus bookmark", "key1=value1;"));
		ds.add(new Bookmark("Chr", Strand.reverse, 300, 400, "foo", "a bogus bookmark named foo in the reverse strand."));
		ds.add(new Bookmark("Chr", Strand.none, 500, 512, "qwer", "a bookmark with no strand orientation.", "a=1;b=123;c=40001;key1=value 1;fiddle=faddle;"));
		
		File temp = File.createTempFile("test.bookmarks", ".tsv");

		BookmarkWriter writer = new BookmarkWriter();
		writer.writeBookmarks(temp, ds);
		
		BookmarkReader reader = new BookmarkReader();
		BookmarkDataSource ds2 = reader.loadData(temp);
		
		assertEquals("test.bookmarks", ds2.getName());
		
		for (Bookmark bookmark : ds2) {
			log.info("bookmark=" + bookmark.toString());
			if ("bogus".equals(bookmark.getLabel())) {
				assertEquals("Chr", bookmark.getSeqId());
				assertEquals(Strand.forward, bookmark.getStrand());
				assertEquals(100, bookmark.getStart());
				assertEquals(200, bookmark.getEnd());
				assertEquals("a bogus bookmark", bookmark.getAnnotation());
				assertEquals("key1=value1;", bookmark.getAttributesString());

				Attributes a = bookmark.getAttributes();
				assertEquals("value1", a.getString("key1"));
			}
			else if ("foo".equals(bookmark.getLabel())) {
				assertEquals("Chr", bookmark.getSeqId());
				assertEquals(Strand.reverse, bookmark.getStrand());
				assertEquals(300, bookmark.getStart());
				assertEquals(400, bookmark.getEnd());
				assertEquals("a bogus bookmark named foo in the reverse strand.", bookmark.getAnnotation());
				assertEquals(null, bookmark.getAttributesString());
			}
			else if ("qwer".equals(bookmark.getLabel())) {
				assertEquals("Chr", bookmark.getSeqId());
				assertEquals(Strand.none, bookmark.getStrand());
				assertEquals(500, bookmark.getStart());
				assertEquals(512, bookmark.getEnd());
				assertEquals("a bookmark with no strand orientation.", bookmark.getAnnotation());

				Attributes a = bookmark.getAttributes();
				assertEquals(1, a.getInt("a"));
				assertEquals(123, a.getInt("b"));
				assertEquals(40001, a.getInt("c"));
				assertEquals("value 1", a.getString("key1"));
				assertEquals("faddle", a.getString("fiddle"));
			}
			else {
				fail("bookmark: " + bookmark.getLabel());
			}
		}
	}
}
