package org.systemsbiology.genomebrowser.model;

import org.junit.Test;

import static org.junit.Assert.*;


public class TestSegmentParse {

	@Test
	public void test() {
		Segment s;
		
		s = Segment.parse("pNRC100", "100-200");
		System.out.println(s);
		assertEquals(100,s.start);
		assertEquals(200,s.end);
		assertEquals("pNRC100",s.seqId);

		s = Segment.parse("pNRC100", "10000 20000");
		System.out.println(s);
		assertEquals(10000,s.start);
		assertEquals(20000,s.end);

		s = Segment.parse("pNRC100", "20000, 10000");
		System.out.println(s);
		assertEquals(10000,s.start);
		assertEquals(20000,s.end);

		s = Segment.parse("pNRC100", "10000, 20000");
		System.out.println(s);
		assertEquals(10000,s.start);
		assertEquals(20000,s.end);

		s = Segment.parse("pNRC100", "10000,20000");
		System.out.println(s);
		assertEquals(10000,s.start);
		assertEquals(20000,s.end);

		s = Segment.parse("pNRC100", "10000   -     20000");
		System.out.println(s);
		assertEquals(10000,s.start);
		assertEquals(20000,s.end);

	}
}
