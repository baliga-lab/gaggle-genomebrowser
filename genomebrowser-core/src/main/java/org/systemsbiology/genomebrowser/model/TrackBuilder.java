package org.systemsbiology.genomebrowser.model;

import java.util.UUID;

// this will be implemented with Sqlite's temp table, of which
// there can be only one at a time. The FeatureSource abstraction
// prevents (at least for single threaded use) the possibility
// that multiple TrackBuilders could be in use at once. And,
// gives the TrackBuilder implementation the chance to do any
// necessary finalization after all features have been loaded.
// So, our SQLite implementation can sort and copy all features
// into a permanent table.

public interface TrackBuilder {
    void beginNewTrack(UUID uuid, String name);
    void addFeatures(FeatureSource featureSource);
    void setAttribute(String key, Object Value);
    Track<? extends Feature> getTrack();
}
