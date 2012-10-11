package org.systemsbiology.genomebrowser.model;

import java.util.List;
import java.util.UUID;

/**
 * Interface for building a specific implementation of a genome browser dataset. The
 * process for building a dataset goes like this:
 * <ol>
 * <li>call beginNewDataset("dataset name", uuid)</li>
 * <li>set attributes on the dataset</li>
 * <li>create sequences (chromosomes, plasmids, replicons...)</li>
 * <li>add attributes to sequences, if necessary</li>
 * <li>create tracks
 * <ol>
 *   <li>track a track</li>
 *   <li>add features to track</li>
 * </ol></li>
 * <li>call getDataset to retrieve the dataset</li>
 * </ol>
 */
public interface DatasetBuilder {

	/**
	 * Start building the dataset.
	 */
	public UUID beginNewDataset(String name);

	/**
	 * Set an attribute on the item with the given UUID
	 */
	public void setAttribute(UUID uuid, String key, Object value);

	/**
	 * add a sequence.
	 * @return the sequence's UUID, in case we want to add attributes.
	 */
	public UUID addSequence(String seqId, int length, Topology topology);

	/**
	 * Add sequences. Allow more efficient implementations adding lots of sequences at once. 
	 */
	public void addSequences(List<Sequence> sequences);

	/**
	 * @param trackType supported types (so far) are "gene", 
	 * "quantitative.positional", and "quantitative.segment"
	 * @return UUID of newly created track
	 */
	public UUID addTrack(String trackType, String name, FeatureSource featureSource);

	/**
	 * Retrieve the dataset after we finish building it.
	 */
	public Dataset getDataset();
}
