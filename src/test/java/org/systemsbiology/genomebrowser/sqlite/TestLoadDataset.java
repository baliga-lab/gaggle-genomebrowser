package org.systemsbiology.genomebrowser.sqlite;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Track;


public class TestLoadDataset {
	@Test
	public void testLoadDataset() {
		SqliteDataSource db = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		Dataset dataset = db.loadDataset();
		assertEquals("Halobacterium Tiling Array", dataset.getName());
		assertEquals("Halobacterium sp. NRC-1", dataset.getAttributes().getString("species"));
		assertEquals(3, dataset.getSequences().size());
		Sequence chr = dataset.getSequence("chromosome");
		assertNotNull(chr);
		assertEquals(2014239, chr.getLength());
		assertEquals("chromosome", chr.getSeqId());
		assertNotNull(dataset.getSequence("pNRC200"));
		assertNotNull(dataset.getSequence("pNRC100"));
	}

	@Test
	public void testTracks() {
		SqliteDataSource db = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		Dataset dataset = db.loadDataset();
		List<Track<Feature>> tracks = dataset.getTracks();
		System.out.println("number of tracks: " + tracks.size());
		assertEquals(7, tracks.size());
		List<String> names = getExpectedTrackNames();
		for (Track<Feature> track : tracks) {
			System.out.println(track.getName() + " -> " + track.getAttributes().getString("viewer"));
			assertTrue(names.contains(track.getName()));
		}
	}
	
	private List<String> getExpectedTrackNames() {
		List<String> names = new ArrayList<String>(7);
		names.add("Genome");
		names.add("Transcription signal");
		names.add("Segmentation");
		names.add("ChIP-chip TFBd nimb");
		names.add("ChIP-chip TFBd 500bp");
		names.add("Peaks ChIP-chip TFBd nimb");
		names.add("Peaks ChIP-chip TFBd 500bp");
		return names;
	}

	@Test
	public void testGeneTrack() {
		SqliteDataSource db = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		Dataset dataset = db.loadDataset();
		Track<Feature> track = dataset.getTrack("Genome");
		assertNotNull(track);
		Sequence chr = dataset.getSequence("chromosome");
		assertNotNull(chr);
		for (Feature feature: track.features(new FeatureFilter(chr, 400000, 402000))) {
			System.out.println(feature);
		}
	}
}
