package org.systemsbiology.genomebrowser.io.track;


import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.sqlite.FeatureSource;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.Iteratable;



// can I do any better than the ui.importtrackwizard.TrackImporter interface?

/**
 *  Construct a new track and add it to the current dataset. This interface is
 *  an attempt to be abstract enough that implementations will be free to vary
 *  both the form of storage and the kind of features.
 *
 *  Steps to import a track (using SQLite store) are:
 *  
 *  1. create features temp table
 *       - can be either quantitative segments, quantitative positions, or named features
 *       - leave open for new types of tracks?
 *  2. map sequences
 *       - map arbitrary strings to sequences_id? or sequence name?
 *       - done in code rather than in SQL? or a bunch of update statements?
 *       - we might want a sequences_id column in the temp table we could fill in later with a mapper
 *  3. copy features to permanent table (sorting and substituting sequence_ids)
 *       - using SQL
 *  4. tracks entry
 *       - uuid, name, type, table_name
 *  5. datasets_tracks entry
 *  6. attributes entries
 *  
 *  
 *  Stuff you need:
 *    track name
 *    track type
 *    ?source
 *    features
 *    ?sequence mapper
 *    ?coordinate mapper
 *    attributes
 *    
 */
public interface TrackBuilder {

	/**
	 * Begin buiding a new track.
	 * @param name name of new track
	 * @param type track type ('gene', 'quantitative.segment', 'quantitative.positional', etc.)
	 * @see org.systemsbiology.genomebrowser.visualization.tracks.TrackRendererRegistry
	 */
	public void startNewTrack(String name, String type);

	/**
	 * Source of features for the track. Exactly what this takes is intentionally
	 * left vague to be determined by implementing classes.
	 */
	public void setSource(String source);

	/**
	 * add features to temporary storage.
	 */
	public void addFeatures(Iteratable<Feature> features);

	/**
	 * add features to temporary storage potentially asynchronously.
	 */
	public void addFeatures(FeatureSource featureSource);

	/**
	 * Transform features by replacing sequence names to names compatible
	 * with the target dataset.
	 */
	public void applySequenceMapper(SequenceMapper<String> mapper);

	/**
	 * Transform features by mapping feature names to coordinates on the genome.
	 */
	public void applyCoordinateMapper(CoordinateMapper mapper);

	/**
	 * Transform features by correcting strand names to '+' or '-'
	 */
	//public void convertStrandNames();

	public void setAttributes(Attributes attributes);

	/**
	 * Copy features from a temporary location into the dataset.
	 * Potentially implemented asynchronously, periodically reporting progress to listeners.
	 */
	public void processFeatures();

	/**
	 * To be called after processFeatures completes.
	 */
	public Track<Feature> getFinishedTrack();

    /**
     * Stop any import in progress. Clean up any shrapnel.
     */
	public void cancel();

	public void addProgressListener(ProgressListener progressListener);
	public void removeProgressListener(ProgressListener progressListener);
}
