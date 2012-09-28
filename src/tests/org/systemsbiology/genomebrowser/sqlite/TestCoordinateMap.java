package org.systemsbiology.genomebrowser.sqlite;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Coordinates;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.DatasetDescription;
import org.systemsbiology.util.MathUtils;


public class TestCoordinateMap {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void test() {
		SqliteDataSource db = new SqliteDataSource("jdbc:sqlite:test.hbgb");

		try {
			List<DatasetDescription> datasets = db.getDatasets();
			UUID datasetUuid = datasets.get(0).uuid;

			List<NamedFeature> mappings = new ArrayList<NamedFeature>();
			mappings.add(createMapping("mm_fwd_000001", "chromosome", Strand.forward, 1, 60));
			mappings.add(createMapping("mm_fwd_000002", "chromosome", Strand.forward, 15, 74));
			mappings.add(createMapping("mm_fwd_000003", "chromosome", Strand.forward, 29, 88));
			mappings.add(createMapping("mm_fwd_000004", "chromosome", Strand.forward, 43, 102));
			mappings.add(createMapping("mm_fwd_000005", "chromosome", Strand.forward, 57, 116));
			mappings.add(createMapping("mm_fwd_000006", "chromosome", Strand.forward, 71, 130));
			mappings.add(createMapping("mm_fwd_000007", "chromosome", Strand.forward, 85, 144));
			mappings.add(createMapping("mm_fwd_000008", "chromosome", Strand.forward, 99, 158));
			mappings.add(createMapping("mm_fwd_000009", "chromosome", Strand.forward, 113, 172));
			mappings.add(createMapping("mm_fwd_000010", "chromosome", Strand.forward, 127, 186));
			db.createCoordinateMapping(datasetUuid, "test", mappings);
	
			CoordinateMap map = db.loadCoordinateMap("map_test");
			Coordinates coord = map.getCoordinates("mm_fwd_000004");
			log.info("mm_fwd_000004 => " + coord);
			Coordinates coord5 = map.getCoordinates("mm_fwd_000005");
			log.info("mm_fwd_000005 => " + coord5);
			Coordinates coord10 = map.getCoordinates("mm_fwd_000010");
			log.info("mm_fwd_000010 => " + coord10);
			assertEquals("chromosome", coord.getSeqId());
			assertEquals(Strand.forward, coord.getStrand());
			assertEquals(43, coord.getStart());
			assertEquals(102, coord.getEnd());
		}
		finally {
			db.deleteCoordinateMap("map_test");
		}
	}

	/**
	 * Test inexact chromosome name matches
	 */
	@Test
	public void testChr() {
		SqliteDataSource db = new SqliteDataSource("jdbc:sqlite:test.hbgb");

		try {
			List<DatasetDescription> datasets = db.getDatasets();
			UUID datasetUuid = datasets.get(0).uuid;

			List<NamedFeature> mappings = new ArrayList<NamedFeature>();
			mappings.add(createMapping("mm_fwd_000001", "chr", Strand.forward, 1, 60));
			mappings.add(createMapping("mm_fwd_000002", "chr1", Strand.forward, 15, 74));
			mappings.add(createMapping("mm_fwd_000003", "Chromosome", Strand.forward, 29, 88));
			mappings.add(createMapping("mm_fwd_000004", "chr-1", Strand.forward, 43, 102));
			db.createCoordinateMapping(datasetUuid, "test", mappings);
	
			CoordinateMap map = db.loadCoordinateMap("map_test");
			Coordinates coord = map.getCoordinates("mm_fwd_000004");
			log.info("mm_fwd_000004 => " + coord);
			assertEquals("chromosome", coord.getSeqId());
			assertEquals(Strand.forward, coord.getStrand());
			assertEquals(43, coord.getStart());
			assertEquals(102, coord.getEnd());
		}
		finally {
			db.deleteCoordinateMap("map_test");
		}
	}

	public NamedFeature createMapping(final String name, final String sequence, final Strand strand, final int start, final int end) {
		return new NamedFeature() {
			public int getEnd() {
				return end;
			}

			public String getName() {
				return name;
			}

			public String getSeqId() {
				return sequence;
			}

			public int getStart() {
				return start;
			}

			public Strand getStrand() {
				return strand;
			}

			public int getCentralPosition() {
				return MathUtils.average(start, end);
			}

			public String getLabel() {
				return String.format("%s%s:%d-%d", sequence, strand.toAbbreviatedString(), start, end);
			}
		};
	}
}
