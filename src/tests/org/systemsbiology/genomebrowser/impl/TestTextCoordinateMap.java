package org.systemsbiology.genomebrowser.impl;

import static org.junit.Assert.*;

import org.junit.Test;
import org.systemsbiology.genomebrowser.model.Coordinates;
import org.systemsbiology.genomebrowser.model.Strand;


public class TestTextCoordinateMap {

	@Test
	public void test() {
		TextCoordinateMap cm = new TextCoordinateMap();
		Coordinates coords = cm.getCoordinates("foobar:12345000-12345678");
		assertEquals("foobar", coords.getSeqId());
		assertEquals(Strand.forward, coords.getStrand());
		assertEquals(12345000, coords.getStart());
		assertEquals(12345678, coords.getEnd());
		
		coords = cm.getCoordinates("chromosome-12:20123000-19999000");
		assertEquals("chromosome-12", coords.getSeqId());
		assertEquals(Strand.reverse, coords.getStrand());
		assertEquals(19999000, coords.getStart());
		assertEquals(20123000, coords.getEnd());

		coords = cm.getCoordinates("chromosome-12+:20123000-19999000");
		assertEquals("chromosome-12", coords.getSeqId());
		assertEquals(Strand.forward, coords.getStrand());
		assertEquals(19999000, coords.getStart());
		assertEquals(20123000, coords.getEnd());

		coords = cm.getCoordinates("chromosome-12+:19999000-20123000");
		assertEquals("chromosome-12", coords.getSeqId());
		assertEquals(Strand.forward, coords.getStrand());
		assertEquals(19999000, coords.getStart());
		assertEquals(20123000, coords.getEnd());

		coords = cm.getCoordinates("chromosome-12-:20123000-19999000");
		assertEquals("chromosome-12", coords.getSeqId());
		assertEquals(Strand.reverse, coords.getStrand());
		assertEquals(19999000, coords.getStart());
		assertEquals(20123000, coords.getEnd());

		coords = cm.getCoordinates("chromosome-12-:19999000-20123000");
		assertEquals("chromosome-12", coords.getSeqId());
		assertEquals(Strand.reverse, coords.getStrand());
		assertEquals(19999000, coords.getStart());
		assertEquals(20123000, coords.getEnd());
	}
}
