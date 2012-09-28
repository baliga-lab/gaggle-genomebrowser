package org.systemsbiology.genomebrowser.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.systemsbiology.util.Attributes;


/**
 * A dataset is a set of sequences and a set of tracks holding features
 * on those sequences.
 * 
 * @author cbare
 */
public interface Dataset {
	public UUID getUuid();
	public String getName();
	public List<Sequence> getSequences();
	public Sequence getSequence(String seqId);
	public List<Track<Feature>> getTracks();
	public Track<Feature> getTrack(String name);
	public void addTrack(Track<? extends Feature> track);
	public Attributes getAttributes();
	
	/**
	 * @return an implementation of SequenceFetcher or null if there is none
	 */
	public SequenceFetcher getSequenceFetcher();


	Dataset EMPTY_DATASET = new Dataset() {
		private Attributes attr = Attributes.EMPTY;

		public UUID getUuid() {
			return null;
		}

		public Attributes getAttributes() {
			return attr;
		}

		public String getName() {
			return "";
		}


		public List<Sequence> getSequences() {
			return Collections.emptyList();
		}

		public Sequence getSequence(String seqId) {
			throw new RuntimeException("Dataset does not contain a sequence named \"" + seqId + "\".");
		}

		public List<Track<Feature>> getTracks() {
			return Collections.emptyList();
		}

		public Track<Feature> getTrack(String name) {
			return null;
		}

		public void addTrack(Track<? extends Feature> track) {
			throw new RuntimeException("Can't add a track to the empty dataset");
		}
		
		public SequenceFetcher getSequenceFetcher() {
			return null;
		}
	};


}
