package org.systemsbiology.genomebrowser.sqlite;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.TrackStub;


/**
 * Tests our ability to load and index blocks of track data.
 * @author cbare
 */
public class TestBlocks {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void testIndex() {
		SqliteDataSource dataSource = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		List<TrackStub> trackStubs = dataSource.loadTrackStubs(UUID.fromString("21676c27-782f-469d-972b-a0204ee295c9"));
		assertTrue(trackStubs.size() > 0);
		int i=0;
		for (;i<trackStubs.size(); i++)
			if (trackStubs.get(i).name.equals("Transcription signal"))
				break;
		BlockIndex index = dataSource.createBlockIndex(trackStubs.get(i));
		
		for (BlockKey key: index.keys("chromosome", Strand.forward, 0, 100)) {
			log.info(key);
			assertEquals("chromosome", key.getSeqId());
			assertEquals(Strand.forward, key.getStrand());
		}

		for (BlockKey key: index.keys("chromosome", Strand.reverse, 0, 100)) {
			log.info(key);
			assertEquals("chromosome", key.getSeqId());
			assertEquals(Strand.reverse, key.getStrand());
		}

		for (BlockKey key: index.keys("pNRC200", Strand.forward, 0, 100)) {
			log.info(key);
			assertEquals("pNRC200", key.getSeqId());
			assertEquals(Strand.forward, key.getStrand());
		}

		for (BlockKey key: index.keys("pNRC200", Strand.reverse, 0, 100)) {
			log.info(key);
			assertEquals("pNRC200", key.getSeqId());
			assertEquals(Strand.reverse, key.getStrand());
		}
	}
}
