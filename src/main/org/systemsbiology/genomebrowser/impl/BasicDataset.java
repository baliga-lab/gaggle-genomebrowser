package org.systemsbiology.genomebrowser.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.SequenceFetcher;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.Attributes;


public class BasicDataset implements Dataset {
	protected UUID uuid;
	protected String name;
	protected Attributes attributes = new Attributes();
	protected List<Sequence> sequences = new ArrayList<Sequence>();
	protected List<Track<Feature>> tracks = new ArrayList<Track<Feature>>();
	protected SequenceFetcher sequenceFetcher = null;

	public BasicDataset() {}
	
	public BasicDataset(UUID uuid, String name) {
		this.name = name;
		this.uuid = uuid;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	public List<Sequence> getSequences() {
		return sequences;
	}

	public void setSequences(List<Sequence> sequences) {
		this.sequences = sequences;
	}

	public void addSequence(Sequence sequence) {
		sequences.add(sequence);
	}

	public List<Track<Feature>> getTracks() {
		return tracks;
	}

	public Track<Feature> getTrack(String name) {
		for (Track<Feature> track: tracks) {
			if (name.equals(track.getName())) {
				return track;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void addTracks(List<Track<? extends Feature>> newTracks) {
		// can't figure out how to use addAll due to generics crap 
		for (Track<? extends Feature> t: newTracks) {
			tracks.add((Track<Feature>)t);
		}
	}

	@SuppressWarnings("unchecked")
	public void addTrack(Track<? extends Feature> track) {
		tracks.add((Track<Feature>)track);
	}

	public Sequence getSequence(String seqId) {
		for (Sequence seq : sequences) {
			if (seqId.equals(seq.getSeqId())) return seq;
		}
		return null;
	}

	public SequenceFetcher getSequenceFetcher() {
		return sequenceFetcher;
	}

	public void setSequenceFetcher(SequenceFetcher sequenceFetcher) {
		this.sequenceFetcher = sequenceFetcher;
	}

	@Override
	public String toString() {
		return "{Dataset: " + name + "}";
	}
}
