package org.systemsbiology.genomebrowser.sqlite;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.systemsbiology.util.FileUtils;


public class TestGffLoader {
	
	@Test
	public void testSomething() throws Exception {
		GffReader reader = new GffReader("jdbc:sqlite:test.hbgb");
		reader.read(FileUtils.getReaderFor("test.example.gff"));

		SqliteDataSource dataSource = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		assertEquals(30, dataSource.countRows("temp"));

		List<String> unknownSequences = reader.checkSequenceNames();
		assertTrue(unknownSequences.isEmpty());

		reader.createNewTrack("TestTrack", "quantitative.segment", "features_test_track");
		assertEquals(30, dataSource.countRows("features_test_track"));
		
		// cleanup
		dataSource.deleteTrackByName("TestTrack");
	}
}
