package org.systemsbiology.genomebrowser.io;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;


public class TestTabDelimitedFileLoader {
	private static final Logger log = Logger.getLogger(TestTabDelimitedFileLoader.class);

	@Test
	public void testUndefinedColumns() throws Exception {
		TabDelimitedFileLoader tr = new TabDelimitedFileLoader(0);
		tr.loadData("classpath:/example/1/tiling_array.tsv");
	}

	@Test
	public void test() throws Exception {
		TabDelimitedFileLoader tr = new TabDelimitedFileLoader(1000);
		tr.addIntColumn(0);
		tr.addIntColumn(1);
		tr.addDoubleColumn(2);
		tr.loadData("classpath:/example/1/tiling_array.tsv");

		log.info("Verifying data...");

		int[] starts = tr.getIntColumn(0);
		assertEquals(1000, starts.length);
		assertEquals(1, starts[0]);
		assertEquals(101, starts[1]);
		assertEquals(99901, starts[999]);

		int[] ends = tr.getIntColumn(1);
		assertEquals(1000, ends.length);
		assertEquals(100, ends[0]);
		assertEquals(200, ends[1]);
		assertEquals(100000, ends[999]);

		double[] values = tr.getDoubleColumn(2);
		assertEquals(1000, values.length);
		assertEquals(0, values[0], 0.000001);
		assertEquals(0.062853290044482, values[1], 0.000001);
		assertEquals(-0.0628532900444885, values[998], 0.000001);

		log.info("Verifying column headers...");
		assertEquals("START", tr.getColumnHeader(0));
		assertEquals("END", tr.getColumnHeader(1));
		assertEquals("VALUE", tr.getColumnHeader(2));
	}

	@Test
	public void testErrors() throws Exception {
		TabDelimitedFileLoader tr = new TabDelimitedFileLoader(1000);
		tr.addIntColumn(0);
		tr.addDoubleColumn(2);
		tr.loadData("classpath:/example/1/tiling_array.tsv");

		// try wrong type of column
		try {
			tr.getDoubleColumn(0);
			fail("Should have thrown exception");
		}
		catch (Exception e) {
			log.info("Got Expected Exception: " + e.getClass().getName());
		}

		// bad column index
		try {
			tr.getDoubleColumn(3);
			fail("Should have thrown exception");
		}
		catch (Exception e) {
			log.info("Got Expected Exception: " + e.getClass().getName());
		}
	}

	@Test
	public void testComputedColumn() throws Exception {
		TabDelimitedFileLoader tr = TabDelimitedFileLoader.createSegmentToPositionDataPointLoader(1000);
		tr.loadData("classpath:/example/1/tiling_array.tsv");

		// tests computed columns which have a name rather than an index
		int[] positions = tr.getIntColumn("position");
		assertEquals(1000, positions.length);
		assertEquals(50, positions[0]);
		assertEquals(150, positions[1]);
		assertEquals(99950, positions[999]);

		double[] values = tr.getDoubleColumn(2);
		assertEquals(1000, values.length);
		assertEquals(0, values[0], 0.000001);
		assertEquals(0.062853290044482, values[1], 0.000001);
		assertEquals(-0.0628532900444885, values[998], 0.000001);

		log.info("Verifying column headers...");
		assertEquals("START", tr.getColumnHeader(0));
		assertEquals("END", tr.getColumnHeader(1));
		assertEquals("VALUE", tr.getColumnHeader(2));

	}
}
