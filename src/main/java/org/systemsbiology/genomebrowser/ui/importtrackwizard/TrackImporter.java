package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.util.UUID;

import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.sqlite.FeatureSource;
import org.systemsbiology.genomebrowser.util.Attributes;

// TODO move TrackImporter (to app?)

/**
 * A track importer is responsible for importing and storing track data.
 * @author cbare
 */
public interface TrackImporter {
	public UUID importQuantitativeSegmentTrack(String trackName, UUID datasetUuid, FeatureSource featureSource);
	public UUID importQuantitativeSegmentMatrixTrack(String trackName, UUID datasetUuid, FeatureSource featureSource, int columns);
	public UUID importQuantitativePositionalTrack(String trackName, UUID datasetUuid, FeatureSource featureSource);
	public UUID importGeneTrack(String trackName, UUID datasetUuid, FeatureSource featureSource);
	public void storeAttributes(UUID trackUuid, Attributes attributes);
	public void addProgressListener(ProgressListener progressListener);
	public void removeProgressListener(ProgressListener progressListener);
	public boolean deleteTrack(UUID trackUuid);
	public Track<Feature> loadTrack(UUID trackUuid);
}

/*

Steps to import a track are:

1. create features temp table
2. map sequences / map coordinates
3. copy features to permanent table (sorting and substituting sequence_ids)
4. tracks entry
5. datasets_tracks entry
6. attributes entries


Where can a new track come from?
- a file (GFF, etc.)
- an R data.frame
- a gaggle matrix
- a gaggle tuple?
- a UCSC data track
- NCBI genome


backing data store may vary.
types of features we're storing will definitely vary


*/
