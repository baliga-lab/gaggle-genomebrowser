package org.systemsbiology.genomebrowser.model;

// TODO model package should have no external dependencies (except jfc libraries and utils)
import java.util.UUID;

import org.systemsbiology.genomebrowser.sqlite.FeatureSource;


// this will be implemented with Sqlite's temp table, of which
// there can be only one at a time. The FeatureSource abstraction
// prevents (at least for single threaded use) the possibility
// that multiple TrackBuilders could be in use at once. And,
// gives the TrackBuilder implementation the chance to do any
// necessary finalization after all features have been loaded.
// So, our SQLite implementation can sort and copy all features
// into a permanent table.

public interface TrackBuilder {
	public void beginNewTrack(UUID uuid, String name);
	public void addFeatures(FeatureSource featureSource);
	public void setAttribute(String key, Object Value);
	public Track<? extends Feature> getTrack();
}

